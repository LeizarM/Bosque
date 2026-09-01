package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.dto.AsistenciaDiaDto;
import bo.bosque.com.impexpap.dto.BitacoraDto;
import bo.bosque.com.impexpap.dto.DiaNoHabilDto;
import bo.bosque.com.impexpap.dto.HorarioVigenteEmpleadoDto;
import bo.bosque.com.impexpap.dto.PermisoKardexDto;
import bo.bosque.com.impexpap.dto.ReporteBiometricoRequest;
import bo.bosque.com.impexpap.dto.ResumenAsistenciaEmpleadoDto;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Controlador REST del módulo Biométrico (tablas {@code tbio_}).
 *
 * <p><b>Arquitectura:</b> igual que el resto del proyecto — sin JPA, todo por Stored
 * Procedures. Los endpoints CRUD de acá abajo son delgados: solo mapean HTTP a los
 * {@code Bio*Dao} ya existentes. Los 7 SP {@code p_abm_Bio*} con ABM real (no
 * {@code p_abm_BioCHECKINOUT}, que nunca fue un ABM) se migraron al contrato
 * {@code @error/@errormsg/@idGenerado OUTPUT} — ver
 * {@code sql/01_migrar_sps_biometrico_a_sphelper.sql} en
 * {@code ClaudeBiometricoMigracion} — así que los DAOs usan
 * {@code SpHelper.ejecutarAbmMap} (no {@code JdbcTemplate} directo) y devuelven
 * {@link RespuestaSp}, igual que el resto de los módulos nuevos.
 *
 * <p><b>{@link #reporteMensual} es la pieza nueva de verdad.</b> Reemplaza a
 * {@code p_Rpt_Biometrico} (un {@code WHILE} anidado con ~6 subqueries correlacionadas
 * por día) por un cálculo en Java sobre datos traídos en bloque: los horarios del
 * empleado, sus marcaciones del mes, y dos consultas ya existentes en
 * {@link IPermiso} — {@code diasNoHabiles} (feriados y sábados que el rol de turnos
 * dice que no le tocan) y {@code kardex} (permisos/vacaciones). Así se corrigen, sin
 * tocar ningún SP:
 * <ul>
 *   <li>el bug de {@code p_abm_BioHrXEmplExpandido ACCION='A'} que colapsaba los N
 *       horarios de un empleado en el mes a uno solo (acá se elige el horario vigente
 *       día por día, no una vez por empleado);</li>
 *   <li>la falta de integración con el rol de sábados (antes un sábado "no le toca"
 *       salía como falta; {@code IPermiso.diasNoHabiles} ya resuelve eso, no hacía
 *       falta escribir el join a mano);</li>
 *   <li>vacaciones/permisos y feriados marcados como falta.</li>
 * </ul>
 *
 * <p>Convención de la casa: todos los endpoints son POST con {@code @RequestBody} y
 * responden {@code ApiResponse<T>}.
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/biometrico")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class BiometricoController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    /**
     * Hilos a la vez para {@link #calcularResumen}. HikariCP sólo da 5
     * conexiones para TODA la app (`application-*.properties`) — 3 deja
     * margen para el resto de las requests concurrentes mientras corre este
     * reporte de ~400 empleados.
     */
    private static final int PARALELISMO_RESUMEN = 3;

    private final ExecutorService resumenExecutor = Executors.newFixedThreadPool(PARALELISMO_RESUMEN);

    private final IBioCheckInOut checkInOutDao;
    private final IBioCheckInOutAdicional checkInOutAdicionalDao;
    private final IBioEmplBosqEmpl emplBosqEmplDao;
    private final IBioHrEmpleado hrEmpleadoDao;
    private final IBioHrs hrsDao;
    private final IBioHrSemanal hrSemanalDao;
    private final IBioHrSemanalDetalle hrSemanalDetalleDao;
    private final IBioHrXEmplExpandido hrXEmplExpandidoDao;
    private final IPermiso permisoDao;
    private final IBioBitacora bitacoraDao;
    private final ILoginDao loginDao;
    private final IEmpleado empleadoDao;
    private final JasperReportExport jasperReportExport;

    public BiometricoController(IBioCheckInOut checkInOutDao,
                                 IBioCheckInOutAdicional checkInOutAdicionalDao,
                                 IBioEmplBosqEmpl emplBosqEmplDao,
                                 IBioHrEmpleado hrEmpleadoDao,
                                 IBioHrs hrsDao,
                                 IBioHrSemanal hrSemanalDao,
                                 IBioHrSemanalDetalle hrSemanalDetalleDao,
                                 IBioHrXEmplExpandido hrXEmplExpandidoDao,
                                 IPermiso permisoDao,
                                 IBioBitacora bitacoraDao,
                                 ILoginDao loginDao,
                                 IEmpleado empleadoDao,
                                 JasperReportExport jasperReportExport) {
        this.checkInOutDao = checkInOutDao;
        this.checkInOutAdicionalDao = checkInOutAdicionalDao;
        this.emplBosqEmplDao = emplBosqEmplDao;
        this.hrEmpleadoDao = hrEmpleadoDao;
        this.hrsDao = hrsDao;
        this.hrSemanalDao = hrSemanalDao;
        this.hrSemanalDetalleDao = hrSemanalDetalleDao;
        this.hrXEmplExpandidoDao = hrXEmplExpandidoDao;
        this.permisoDao = permisoDao;
        this.bitacoraDao = bitacoraDao;
        this.loginDao = loginDao;
        this.empleadoDao = empleadoDao;
        this.jasperReportExport = jasperReportExport;
    }

    @PreDestroy
    public void cerrarResumenExecutor() {
        resumenExecutor.shutdown();
    }

    // ==================================================================
    // 1 · Marcaciones crudas (tbio_bioCHECKINOUT)
    // ==================================================================

    @PostMapping("/marcaciones/listar")
    public ResponseEntity<ApiResponse<?>> listarMarcaciones(@RequestBody(required = false) Map<String, Object> filtro) {
        return respuestaListado(checkInOutDao.listar(filtro != null ? filtro : new HashMap<>()));
    }

    /**
     * Dispara la importación de marcaciones del mes de {@code checkTime} desde el
     * dispositivo (ver javadoc de {@link IBioCheckInOut#dispararImportacionMensual}).
     * No es un alta de una fila — no tiene equivalente "registrar" fila por fila.
     */
    @PostMapping("/marcaciones/importar-mensual")
    public ResponseEntity<ApiResponse<?>> importarMarcacionesMensual(@RequestBody Map<String, String> body) {
        Date checkTime = parseFecha(body.get("checkTime"));
        String detalle = checkInOutDao.dispararImportacionMensual(checkTime);
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, detalle, 200));
    }

    // ==================================================================
    // 2 · Marcaciones adicionales / olvidadas (tbio_bioCHECKINOUTAdicinal)
    // ==================================================================

    @PostMapping("/marcaciones-adicionales/listar")
    public ResponseEntity<ApiResponse<?>> listarMarcacionesAdicionales(@RequestBody(required = false) Map<String, Object> filtro) {
        return respuestaListado(checkInOutAdicionalDao.listar(filtro != null ? filtro : new HashMap<>()));
    }

    @PostMapping("/marcaciones-adicionales/registrar")
    public ResponseEntity<ApiResponse<?>> registrarMarcacionAdicional(
            @RequestBody BioCheckInOutAdicional item, @RequestParam String acc,
            @RequestParam(required = false) String motivo, Authentication auth) {
        long audUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(checkInOutAdicionalDao.registrar(item, acc, audUsuario, motivo));
    }

    // ==================================================================
    // 3 · Cruce empleado biométrico ⇄ Bosque (tbio_bioEmplBosqEmpl)
    // ==================================================================

    /**
     * {@code filtro} acepta la clave extra {@code soloActivos} (no es un parámetro
     * de {@code p_list_BioEmplBosqEmpl} — se saca del mapa antes de mandarlo al SP,
     * que fallaría con un parámetro que no declara, y se aplica acá en Java): si
     * viene {@code true}, se excluyen los enlazados a un empleado de Bosque que ya
     * no está activo. No es un estado guardado — se recalcula contra
     * {@link IEmpleado#obtenerListaEmpleados(int)} en cada llamada, así que si el
     * empleado vuelve a estar activo en Bosque, reaparece solo, sin tocar nada acá.
     * Sin el flag (uso de la pestaña Empleados/Verificación) se sigue viendo el
     * padrón completo, activos e inactivos, para poder auditar/desenlazar cualquiera.
     */
    @PostMapping("/empleados/listar")
    public ResponseEntity<ApiResponse<?>> listarEmpleados(@RequestBody(required = false) Map<String, Object> filtro) {
        Map<String, Object> filtroCopia = filtro != null ? new HashMap<>(filtro) : new HashMap<>();
        boolean soloActivos = Boolean.TRUE.equals(filtroCopia.remove("soloActivos"));

        List<BioEmplBosqEmpl> lista = emplBosqEmplDao.listar(filtroCopia);
        if (soloActivos) {
            Set<Integer> activos = idsEmpleadosActivos();
            lista = lista.stream()
                    .filter(e -> e.getIdEmpleado() <= 0 || activos.contains((int) e.getIdEmpleado()))
                    .collect(Collectors.toList());
        }
        return respuestaListado(lista);
    }

    @PostMapping("/empleados/registrar")
    public ResponseEntity<ApiResponse<?>> registrarEmpleado(
            @RequestBody BioEmplBosqEmpl item, @RequestParam String acc, Authentication auth) {
        long audUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(emplBosqEmplDao.registrar(item, acc, audUsuario));
    }

    // ==================================================================
    // 4 · Plantillas de turno (tbio_bioHrs)
    // ==================================================================

    @PostMapping("/horarios/listar")
    public ResponseEntity<ApiResponse<?>> listarHorarios(@RequestBody(required = false) Map<String, Object> filtro) {
        return respuestaListado(hrsDao.listar(filtro != null ? filtro : new HashMap<>()));
    }

    @PostMapping("/horarios/registrar")
    public ResponseEntity<ApiResponse<?>> registrarHorario(
            @RequestBody BioHrs item, @RequestParam String acc,
            @RequestParam(required = false) String motivo, Authentication auth) {
        long audUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(hrsDao.registrar(item, acc, audUsuario, motivo));
    }

    // ==================================================================
    // 5 · Horarios semanales — cabecera (tbio_bioHrSemanal)
    // ==================================================================

    @PostMapping("/horarios-semanales/listar")
    public ResponseEntity<ApiResponse<?>> listarHorariosSemanales(@RequestBody(required = false) Map<String, Object> filtro) {
        return respuestaListado(hrSemanalDao.listar(filtro != null ? filtro : new HashMap<>()));
    }

    @PostMapping("/horarios-semanales/registrar")
    public ResponseEntity<ApiResponse<?>> registrarHorarioSemanal(
            @RequestBody BioHrSemanal item, @RequestParam String acc,
            @RequestParam(required = false) String motivo, Authentication auth) {
        long audUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(hrSemanalDao.registrar(item, acc, audUsuario, motivo));
    }

    // ==================================================================
    // 6 · Horarios semanales — detalle por día (tbio_bioHrSemanalDetalle)
    // ==================================================================

    @PostMapping("/horarios-semanales-detalle/listar")
    public ResponseEntity<ApiResponse<?>> listarHorariosSemanalesDetalle(@RequestBody(required = false) Map<String, Object> filtro) {
        return respuestaListado(hrSemanalDetalleDao.listar(filtro != null ? filtro : new HashMap<>()));
    }

    @PostMapping("/horarios-semanales-detalle/registrar")
    public ResponseEntity<ApiResponse<?>> registrarHorarioSemanalDetalle(
            @RequestBody BioHrSemanalDetalle item, @RequestParam String acc,
            @RequestParam(required = false) String motivo, Authentication auth) {
        long audUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(hrSemanalDetalleDao.registrar(item, acc, audUsuario, motivo));
    }

    // ==================================================================
    // 7 · Horario asignado a un empleado (tbio_bioHrEmpleado)
    // ==================================================================

    @PostMapping("/horario-empleado/listar")
    public ResponseEntity<ApiResponse<?>> listarHorarioEmpleado(@RequestBody(required = false) Map<String, Object> filtro) {
        return respuestaListado(hrEmpleadoDao.listar(filtro != null ? filtro : new HashMap<>()));
    }

    @PostMapping("/horario-empleado/registrar")
    public ResponseEntity<ApiResponse<?>> registrarHorarioEmpleado(
            @RequestBody BioHrEmpleado item, @RequestParam String acc,
            @RequestParam(required = false) String motivo, Authentication auth) {
        long audUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(hrEmpleadoDao.registrar(item, acc, audUsuario, motivo));
    }

    // ==================================================================
    // 8 · Calendario expandido, tal como lo dejó el generador legacy
    //     (tbio_bioHrXEmplExpandido) — el reporte de abajo NO depende de
    //     esta tabla a propósito (ver javadoc de la clase).
    // ==================================================================

    @PostMapping("/calendario-expandido/listar")
    public ResponseEntity<ApiResponse<?>> listarCalendarioExpandido(@RequestBody(required = false) Map<String, Object> filtro) {
        return respuestaListado(hrXEmplExpandidoDao.listar(filtro != null ? filtro : new HashMap<>()));
    }

    @PostMapping("/calendario-expandido/registrar")
    public ResponseEntity<ApiResponse<?>> registrarCalendarioExpandido(
            @RequestBody BioHrXEmplExpandido item, @RequestParam String acc, Authentication auth) {
        long audUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(hrXEmplExpandidoDao.registrar(item, acc, audUsuario));
    }

    /**
     * Regenera el calendario expandido de UN empleado para UN mes —
     * pensado para llamarse justo después de asignar/editar/inactivar un
     * horario en "Por empleado" (`tab_horarios.dart`), no como una acción
     * independiente del usuario.
     *
     * <p>Confirmado el 2026-09-01 (`sys.sql_modules` contra
     * `%tbio_bioHrXEmplExpandido%`): {@code p_Rpt_Biometrico} (el reporte
     * legacy) SÍ lee esta tabla — el reporte NUEVO
     * ({@link #calcularReporte}) no depende de ella, la bypasea a
     * propósito, pero mientras el legacy siga vivo esta tabla necesita
     * mantenerse correcta. Antes de esto, nada la regeneraba nunca.
     *
     * <p>{@code ACCION='A'} sólo INSERTA — un día que ya existe (con
     * cualquier {@code idHrEmpleado}) se salta en vez de corregirse — así
     * que hay que borrar primero. Se borra una vez por cada fila de
     * {@code tbio_bioHrEmpleado} que el empleado tenga (no se sabe de
     * antemano cuál de ellas generó qué día), y recién después se regenera
     * con la SP ya corregida
     * ({@code sql/05_fix_p_abm_BioHrXEmplExpandido_ACCION_A.sql}).
     */
    @PostMapping("/calendario-expandido/regenerar")
    public ResponseEntity<ApiResponse<?>> regenerarCalendarioExpandido(
            @RequestBody ReporteBiometricoRequest req, Authentication auth) {
        if (req.getCodEmpleado() <= 0 || req.getAnio() <= 0 || req.getMes() < 1 || req.getMes() > 12) {
            throw new SpBusinessException("Indique empleado, año y mes válidos.");
        }
        long audUsuario = DatosToken.codUsuarioDe(auth);
        Date unDiaDelMes = toDate(LocalDate.of(req.getAnio(), req.getMes(), 1));

        List<BioHrEmpleado> asignaciones = hrEmpleadoDao.listar(mapa("idEmplead", req.getCodEmpleado()));
        for (BioHrEmpleado a : asignaciones) {
            hrXEmplExpandidoDao.borrarMes(a.getIdHrEmpleado(), unDiaDelMes, audUsuario);
        }
        hrXEmplExpandidoDao.generarMes(req.getCodEmpleado(), unDiaDelMes, audUsuario);

        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, null, 200));
    }

    // ==================================================================
    // 9 · Bitácora de cambios manuales (Marcaciones olvidadas y Horarios)
    // ==================================================================

    /**
     * Historial de {@code tbio_bioBitacora}, con {@code audUsuario} resuelto a
     * nombre. Filtros esperados en el body: {@code tabla}, {@code idRegistro}
     * (historial de UNA fila puntual), {@code desde}/{@code hasta} — todos
     * opcionales.
     */
    @PostMapping("/bitacora/listar")
    public ResponseEntity<ApiResponse<?>> listarBitacora(@RequestBody(required = false) Map<String, Object> filtro) {
        List<BioBitacora> filas = bitacoraDao.listar(filtro != null ? filtro : new HashMap<>());
        return respuestaListado(conNombreUsuario(filas));
    }

    private List<BitacoraDto> conNombreUsuario(List<BioBitacora> filas) {
        Map<Integer, String> nombres = loginDao.getAllUsers().stream()
                .collect(Collectors.toMap(Login::getCodUsuario, Login::getNombreCompleto, (a, b) -> a));
        List<BitacoraDto> resultado = new ArrayList<>(filas.size());
        for (BioBitacora f : filas) {
            BitacoraDto d = new BitacoraDto();
            d.setIdBitacora(f.getIdBitacora());
            d.setTabla(f.getTabla());
            d.setIdRegistro(f.getIdRegistro());
            d.setAccion(f.getAccion());
            d.setMotivo(f.getMotivo());
            d.setAudUsuario(f.getAudUsuario());
            d.setNombreUsuario(nombres.getOrDefault((int) f.getAudUsuario(), "Usuario " + f.getAudUsuario()));
            d.setAudFecha(f.getAudFecha());
            resultado.add(d);
        }
        return resultado;
    }

    // ==================================================================
    // 10 · Reporte mensual de asistencia — corregido, set-based en Java
    // ==================================================================

    @PostMapping("/reporte-mensual")
    public ResponseEntity<ApiResponse<?>> reporteMensual(@RequestBody ReporteBiometricoRequest req) {
        return respuestaListado(calcularReporte(req));
    }

    /**
     * Mismo reporte que {@link #reporteMensual}, como PDF (`RptBiometricoDetallado.jrxml`).
     * A diferencia del resto de los reportes de este backend, el `.jrxml` no
     * tiene {@code <queryString>}: no hay una sola consulta SQL detrás de
     * esto, es el cálculo de {@link #calcularReporte} — así que se llena
     * desde la colección Java vía {@code exportPDFDesdeColeccion}, no desde
     * una {@code Connection}.
     */
    @PostMapping("/reporte-mensual-pdf")
    public ResponseEntity<byte[]> reporteMensualPdf(@RequestBody ReporteBiometricoRequest req) {
        List<AsistenciaDiaDto> dias = calcularReporte(req);

        List<BioEmplBosqEmpl> cruce = emplBosqEmplDao.listar(mapa("idEmpleado", req.getCodEmpleado()));
        String nombreEmpleado = cruce.isEmpty() ? "" : cruce.get(0).getDatoNombreBosq();

        Map<String, Object> params = new HashMap<>();
        params.put("nombreEmpleado", nombreEmpleado);
        params.put("mesAnio", MESES[req.getMes() - 1] + " " + req.getAnio());

        byte[] pdf = jasperReportExport.exportPDFDesdeColeccion("RptBiometricoDetallado", dias, params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(pdf.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /**
     * Un mes, todos los empleados enlazados: una fila con los totales de
     * cada uno (no el detalle día a día de los 400 a la vez — ver el
     * javadoc de la clase / CLAUDE.md sobre por qué esto es un resumen y no
     * el reporte detallado repetido N veces).
     *
     * <p><b>Sigue siendo N+1 en cantidad de consultas</b> — por cada
     * empleado esto llama {@link #calcularReporte}, que repite
     * `emplBosqEmplDao.listar`, `diasNoHabiles` y `kardex` una vez por
     * persona. Escribir un batch real (todos los empleados en una sola
     * pasada) es mucho más riesgoso — un SP/query nuevo sin probar tocando
     * el cálculo que ya está verificado — así que en cambio esto corre las
     * ~400 llamadas a {@link #calcularReporte} EN PARALELO, acotado a
     * {@link #PARALELISMO_RESUMEN} hilos a la vez: el pool de HikariCP tiene
     * sólo 5 conexiones para TODA la app (ver `application-*.properties`),
     * así que no se puede lanzar sin límite — eso dejaría sin conexión al
     * resto de la aplicación mientras corre este reporte.
     */
    @PostMapping("/reporte-mensual-resumen")
    public ResponseEntity<ApiResponse<?>> reporteMensualResumen(@RequestBody ReporteBiometricoRequest req) {
        return respuestaListado(calcularResumen(req));
    }

    @PostMapping("/reporte-mensual-resumen-pdf")
    public ResponseEntity<byte[]> reporteMensualResumenPdf(@RequestBody ReporteBiometricoRequest req) {
        List<ResumenAsistenciaEmpleadoDto> resumen = calcularResumen(req);

        Map<String, Object> params = new HashMap<>();
        params.put("mesAnio", MESES[req.getMes() - 1] + " " + req.getAnio());

        byte[] pdf = jasperReportExport.exportPDFDesdeColeccion("RptBiometricoResumenMensual", resumen, params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(pdf.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /**
     * Qué {@code BioHrSemanal} tiene HOY cada empleado enlazado — una fila por
     * empleado con el nombre del horario semanal vigente, desde cuándo, y las
     * horas de cada día de la semana. No es un reporte mensual (no hay mes:
     * es "ahora mismo", igual que la pestaña "Por empleado" del Flutter) por
     * eso no toma {@link ReporteBiometricoRequest} — no necesita nada en el
     * body.
     */
    @PostMapping("/horario-vigente-por-empleado")
    public ResponseEntity<ApiResponse<?>> horarioVigentePorEmpleado() {
        return respuestaListado(calcularHorariosVigentes());
    }

    @PostMapping("/horario-vigente-por-empleado-pdf")
    public ResponseEntity<byte[]> horarioVigentePorEmpleadoPdf() {
        List<HorarioVigenteEmpleadoDto> filas = calcularHorariosVigentes();

        Map<String, Object> params = new HashMap<>();
        params.put("fechaGeneracion", new Date());

        byte[] pdf = jasperReportExport.exportPDFDesdeColeccion("RptBiometricoHorarioVigente", filas, params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(pdf.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /**
     * Misma estrategia de paralelismo acotado que {@link #calcularResumen} —
     * ver ese javadoc para el motivo (HikariCP con sólo 5 conexiones).
     */
    private List<HorarioVigenteEmpleadoDto> calcularHorariosVigentes() {
        List<BioEmplBosqEmpl> todos = distintosPorEmpleado(emplBosqEmplDao.listar(new HashMap<>()));
        Set<Integer> activos = idsEmpleadosActivos();
        LocalDate hoy = LocalDate.now();

        List<CompletableFuture<HorarioVigenteEmpleadoDto>> corridas = todos.stream()
                .filter(e -> e.getIdEmpleado() > 0) // no enlazado a un empleado Bosque
                .filter(e -> activos.contains((int) e.getIdEmpleado())) // ya no está en la empresa
                .map(e -> CompletableFuture.supplyAsync(() -> calcularFilaHorarioVigente(e, hoy), resumenExecutor))
                .collect(Collectors.toList());

        return corridas.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(HorarioVigenteEmpleadoDto::getNombreEmpleado))
                .collect(Collectors.toList());
    }

    /** Una fila — {@code null} si el empleado no tiene ninguna asignación (nunca se le puso un horario). */
    private HorarioVigenteEmpleadoDto calcularFilaHorarioVigente(BioEmplBosqEmpl e, LocalDate hoy) {
        List<BioHrEmpleado> asignaciones = hrEmpleadoDao.listar(mapa("idEmplead", e.getIdEmpleado()));
        BioHrEmpleado vigente = horarioVigente(asignaciones, hoy);
        if (vigente == null) {
            return null;
        }

        HorarioVigenteEmpleadoDto dto = new HorarioVigenteEmpleadoDto();
        dto.setNombreEmpleado(e.getDatoNombreBosq());
        dto.setVigenteDesde(vigente.getInicio());

        List<BioHrSemanal> semanales = hrSemanalDao.listar(mapa("idHrSemanal", vigente.getIdHrSemanal()));
        dto.setNombreHorarioSemanal(semanales.isEmpty() ? "" : semanales.get(0).getNombre());

        dto.setLunes("—");
        dto.setMartes("—");
        dto.setMiercoles("—");
        dto.setJueves("—");
        dto.setViernes("—");
        dto.setSabado("—");
        dto.setDomingo("—");
        List<BioHrSemanalDetalle> detalle = hrSemanalDetalleDao.listar(mapa("idHrSemanal", vigente.getIdHrSemanal()));
        for (BioHrSemanalDetalle d : detalle) {
            List<BioHrs> r = hrsDao.listar(mapa("idHrs", d.getIdHrs()));
            BioHrs turno = r.isEmpty() ? null : r.get(0);
            String texto = turno == null ? "—" : formatoHora(turno.getIngreso()) + "–" + formatoHora(turno.getSalida());
            switch (d.getDia()) {
                case 1: dto.setLunes(texto); break;
                case 2: dto.setMartes(texto); break;
                case 3: dto.setMiercoles(texto); break;
                case 4: dto.setJueves(texto); break;
                case 5: dto.setViernes(texto); break;
                case 6: dto.setSabado(texto); break;
                case 7: dto.setDomingo(texto); break;
                default: break;
            }
        }
        return dto;
    }

    /**
     * El reporte DETALLADO día a día — el mismo {@code RptBiometricoDetallado.jrxml}
     * de {@link #reporteMensualPdf}, con su columna Obs — pero para todos los
     * empleados enlazados del mes, uno atrás del otro (no el resumen de una
     * fila por persona: eso es {@link #reporteMensualResumenPdf}).
     *
     * <p>Misma estrategia de paralelismo acotado que {@link #calcularResumen}
     * (mismo {@link #resumenExecutor}, mismo motivo: HikariCP sólo tiene 5
     * conexiones para toda la app) — pero acá además se compila el
     * {@code .jrxml} UNA sola vez y se llena una vez por empleado
     * ({@link JasperReportExport#exportPDFDesdeColeccionesMultiples}), no un
     * PDF por separado por persona.
     */
    @PostMapping("/reporte-mensual-detallado-todos-pdf")
    public ResponseEntity<byte[]> reporteMensualDetalladoTodosPdf(@RequestBody ReporteBiometricoRequest req) {
        if (req.getAnio() <= 0 || req.getMes() < 1 || req.getMes() > 12) {
            throw new SpBusinessException("Indique año y mes válidos.");
        }
        String mesAnio = MESES[req.getMes() - 1] + " " + req.getAnio();

        List<BioEmplBosqEmpl> todos = distintosPorEmpleado(emplBosqEmplDao.listar(new HashMap<>()));
        Set<Integer> activos = idsEmpleadosActivos();

        List<CompletableFuture<FilaDetallada>> corridas = todos.stream()
                .filter(e -> e.getIdEmpleado() > 0) // no enlazado a un empleado Bosque
                .filter(e -> activos.contains((int) e.getIdEmpleado())) // ya no está en la empresa
                .map(e -> CompletableFuture.supplyAsync(() -> calcularFilaDetallada(e, req), resumenExecutor))
                .collect(Collectors.toList());

        List<FilaDetallada> filas = corridas.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(f -> f.empleado.getDatoNombreBosq()))
                .collect(Collectors.toList());

        List<java.util.Collection<?>> lotes = new ArrayList<>(filas.size());
        List<Map<String, Object>> paramsPorLote = new ArrayList<>(filas.size());
        for (FilaDetallada f : filas) {
            lotes.add(f.dias);
            Map<String, Object> params = new HashMap<>();
            params.put("nombreEmpleado", f.empleado.getDatoNombreBosq());
            params.put("mesAnio", mesAnio);
            paramsPorLote.add(params);
        }

        byte[] pdf = jasperReportExport.exportPDFDesdeColeccionesMultiples(
                "RptBiometricoDetallado", lotes, paramsPorLote);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(pdf.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /** Una fila del detallado-todos para un empleado, o {@code null} si se lo omite (mismo criterio que {@link #calcularFilaResumen}). */
    private FilaDetallada calcularFilaDetallada(BioEmplBosqEmpl e, ReporteBiometricoRequest req) {
        ReporteBiometricoRequest porEmpleado = new ReporteBiometricoRequest();
        porEmpleado.setCodEmpleado(e.getIdEmpleado());
        porEmpleado.setAnio(req.getAnio());
        porEmpleado.setMes(req.getMes());
        try {
            return new FilaDetallada(e, calcularReporte(porEmpleado));
        } catch (SpBusinessException ex) {
            log.warn("Se omite del detallado-todos al empleado {}: {}", e.getIdEmpleado(), ex.getMessage());
            return null;
        }
    }

    /** Empleado + su mes ya calculado — el par que necesita cada lote de {@link JasperReportExport#exportPDFDesdeColeccionesMultiples}. */
    private static final class FilaDetallada {
        final BioEmplBosqEmpl empleado;
        final List<AsistenciaDiaDto> dias;

        FilaDetallada(BioEmplBosqEmpl empleado, List<AsistenciaDiaDto> dias) {
            this.empleado = empleado;
            this.dias = dias;
        }
    }

    private List<ResumenAsistenciaEmpleadoDto> calcularResumen(ReporteBiometricoRequest req) {
        if (req.getAnio() <= 0 || req.getMes() < 1 || req.getMes() > 12) {
            throw new SpBusinessException("Indique año y mes válidos.");
        }

        List<BioEmplBosqEmpl> todos = distintosPorEmpleado(emplBosqEmplDao.listar(new HashMap<>()));
        Set<Integer> activos = idsEmpleadosActivos();

        List<CompletableFuture<ResumenAsistenciaEmpleadoDto>> corridas = todos.stream()
                .filter(e -> e.getIdEmpleado() > 0) // no enlazado a un empleado Bosque
                .filter(e -> activos.contains((int) e.getIdEmpleado())) // ya no está en la empresa
                .map(e -> CompletableFuture.supplyAsync(() -> calcularFilaResumen(e, req), resumenExecutor))
                .collect(Collectors.toList());

        List<ResumenAsistenciaEmpleadoDto> resumen = corridas.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        resumen.sort(Comparator.comparing(ResumenAsistenciaEmpleadoDto::getNombreEmpleado));
        return resumen;
    }

    /** Una fila del resumen para un empleado, o {@code null} si se lo omite (mismo criterio que antes). */
    private ResumenAsistenciaEmpleadoDto calcularFilaResumen(BioEmplBosqEmpl e, ReporteBiometricoRequest req) {
        ReporteBiometricoRequest porEmpleado = new ReporteBiometricoRequest();
        porEmpleado.setCodEmpleado(e.getIdEmpleado());
        porEmpleado.setAnio(req.getAnio());
        porEmpleado.setMes(req.getMes());

        List<AsistenciaDiaDto> dias;
        try {
            dias = calcularReporte(porEmpleado);
        } catch (SpBusinessException ex) {
            log.warn("Se omite del resumen al empleado {}: {}", e.getIdEmpleado(), ex.getMessage());
            return null;
        }

        ResumenAsistenciaEmpleadoDto fila = new ResumenAsistenciaEmpleadoDto();
        fila.setCodEmpleado(e.getIdEmpleado());
        fila.setNombreEmpleado(e.getDatoNombreBosq());
        fila.setDiasAsignados((int) dias.stream().filter(d -> !AsistenciaDiaDto.SIN_HORARIO.equals(d.getEstado())).count());
        fila.setDiasNoMarcados((int) dias.stream().filter(d -> AsistenciaDiaDto.FALTA.equals(d.getEstado())).count());
        fila.setMinutosAtraso(dias.stream().mapToInt(AsistenciaDiaDto::getMinutosAtraso).sum());
        fila.setObservaciones(resumirObservaciones(dias));
        return fila;
    }

    /**
     * "2 feriado, 1 permiso" — por qué {@link ResumenAsistenciaEmpleadoDto#getDiasAsignados()} no
     * coincide con los días realmente trabajados. {@code null} si no hay ninguno (nada que aclarar).
     */
    private static String resumirObservaciones(List<AsistenciaDiaDto> dias) {
        Map<String, Long> porEstado = dias.stream()
                .filter(d -> AsistenciaDiaDto.FERIADO.equals(d.getEstado())
                        || AsistenciaDiaDto.SABADO_LIBRE.equals(d.getEstado())
                        || AsistenciaDiaDto.PERMISO.equals(d.getEstado())
                        || AsistenciaDiaDto.VACACION.equals(d.getEstado()))
                .collect(Collectors.groupingBy(AsistenciaDiaDto::getEstado, Collectors.counting()));
        if (porEstado.isEmpty()) return null;

        Map<String, String> etiquetas = new LinkedHashMap<>();
        etiquetas.put(AsistenciaDiaDto.FERIADO, "feriado");
        etiquetas.put(AsistenciaDiaDto.SABADO_LIBRE, "sábado libre");
        etiquetas.put(AsistenciaDiaDto.PERMISO, "permiso");
        etiquetas.put(AsistenciaDiaDto.VACACION, "vacación");
        return etiquetas.entrySet().stream()
                .filter(en -> porEstado.containsKey(en.getKey()))
                .map(en -> porEstado.get(en.getKey()) + " " + en.getValue())
                .collect(Collectors.joining(", "));
    }

    private static final String[] MESES = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private List<AsistenciaDiaDto> calcularReporte(ReporteBiometricoRequest req) {
        if (req.getCodEmpleado() <= 0 || req.getAnio() <= 0 || req.getMes() < 1 || req.getMes() > 12) {
            throw new SpBusinessException("Indique empleado, año y mes válidos.");
        }

        List<BioEmplBosqEmpl> cruce = emplBosqEmplDao.listar(mapa("idEmpleado", req.getCodEmpleado()));
        if (cruce.isEmpty()) {
            throw new SpBusinessException("El empleado no está enlazado a un usuario del biométrico.");
        }
        int userIdBiometrico = (int) cruce.get(0).getIdEmpleadBio();

        LocalDate desdeLd = LocalDate.of(req.getAnio(), req.getMes(), 1);
        LocalDate hastaLd = desdeLd.withDayOfMonth(desdeLd.lengthOfMonth());
        Date desde = toDate(desdeLd);
        Date hasta = toDate(hastaLd);

        // ── horario: todas las asignaciones del empleado, elegimos la vigente día a día ──
        List<BioHrEmpleado> asignaciones = hrEmpleadoDao.listar(mapa("idEmplead", req.getCodEmpleado()));

        Map<Long, List<BioHrSemanalDetalle>> detallePorSemanal = new HashMap<>();
        Map<Long, BioHrs> hrsPorId = new HashMap<>();

        // ── marcaciones del mes, agrupadas por día ──
        // fechaIni/fechaFin filtran en el propio SP (ver
        // sql/02_optimizar_p_list_BioCHECKINOUT.sql en ClaudeBiometricoMigracion):
        // antes esto traía TODO el historial del empleado sin filtro de fecha.
        // fechaFin es EXCLUSIVO (primer instante del día siguiente al último del
        // mes) — no depende de la hora exacta que el driver mande.
        Map<String, Object> filtroMarcas = new HashMap<>();
        filtroMarcas.put("USERID", userIdBiometrico);
        filtroMarcas.put("fechaIni", java.sql.Timestamp.valueOf(desdeLd.atStartOfDay()));
        filtroMarcas.put("fechaFin", java.sql.Timestamp.valueOf(hastaLd.plusDays(1).atStartOfDay()));
        List<BioCheckInOut> marcas = checkInOutDao.listar(filtroMarcas);
        // Se agrupa el objeto completo (no sólo CHECKTIME) — hace falta
        // CHECKTYPE más abajo para distinguir entrada de salida en vez de
        // adivinarlo por orden cronológico.
        Map<LocalDate, List<BioCheckInOut>> marcasPorDia = marcas.stream()
                .filter(m -> m.getCHECKTIME() != null)
                .collect(Collectors.groupingBy(m -> toLocalDate(m.getCHECKTIME())));

        // ── marcaciones olvidadas registradas a mano (tbio_bioCHECKINOUTAdicinal) ──
        // Hasta acá esta tabla se guardaba (pestaña Marcaciones Olvidadas /
        // el botón del calendario) pero NUNCA se leía de vuelta — el reporte
        // sólo miraba tbio_bioCHECKINOUT, así que registrar una marcación
        // olvidada no cambiaba nada: el día seguía en FALTA. Se mezclan acá
        // con las del reloj, y se recuerda cuál de las dos (entrada/salida)
        // vino de esta tabla para poder marcarla en el reporte
        // (AsistenciaDiaDto.entradaManual/salidaManual).
        //
        // Sin filtro de fecha en la SP a propósito: a diferencia de
        // tbio_bioCHECKINOUT (137 mil filas), esta tabla es chica —
        // correcciones manuales puntuales, no el historial completo del
        // reloj — así que no hace falta la misma optimización que el script
        // 02_optimizar_p_list_BioCHECKINOUT.sql le hizo a la otra.
        List<BioCheckInOutAdicional> marcasAdicionales =
                checkInOutAdicionalDao.listar(mapa("USERID", userIdBiometrico));
        Map<LocalDate, List<Date>> marcasManualesPorDia = marcasAdicionales.stream()
                .filter(m -> m.getCHECKTIME() != null)
                .collect(Collectors.groupingBy(m -> toLocalDate(m.getCHECKTIME()),
                        Collectors.mapping(BioCheckInOutAdicional::getCHECKTIME, Collectors.toList())));

        // ── feriados y sábados que no le tocan — ya resueltos por IPermiso ──
        Map<LocalDate, DiaNoHabilDto> noHabilesPorDia = permisoDao.diasNoHabiles(req.getCodEmpleado(), desde, hasta)
                .stream().collect(Collectors.toMap(d -> toLocalDate(d.getFecha()), d -> d, (a, b) -> a));

        // ── permisos/vacaciones del empleado, filtramos overlap con el mes en Java ──
        List<PermisoKardexDto> permisos = permisoDao.kardex(req.getCodEmpleado(), null, null, null, null, null);

        List<AsistenciaDiaDto> resultado = new ArrayList<>();
        for (LocalDate dia = desdeLd; !dia.isAfter(hastaLd); dia = dia.plusDays(1)) {
            AsistenciaDiaDto fila = new AsistenciaDiaDto();
            fila.setFecha(toDate(dia));

            BioHrEmpleado vigente = horarioVigente(asignaciones, dia);
            BioHrs turno = null;
            if (vigente != null) {
                List<BioHrSemanalDetalle> detalle = detallePorSemanal.computeIfAbsent(
                        vigente.getIdHrSemanal(), id -> hrSemanalDetalleDao.listar(mapa("idHrSemanal", id)));
                int diaSemana = dia.getDayOfWeek().getValue(); // 1=Lunes...7=Domingo, igual que tbio_bioHrSemanalDetalle
                Optional<BioHrSemanalDetalle> det = detalle.stream().filter(d -> d.getDia() == diaSemana).findFirst();
                if (det.isPresent()) {
                    long idHrs = det.get().getIdHrs();
                    turno = hrsPorId.computeIfAbsent(idHrs, id -> {
                        List<BioHrs> r = hrsDao.listar(mapa("idHrs", id));
                        return r.isEmpty() ? null : r.get(0);
                    });
                }
            }
            if (turno != null) {
                fila.setHoraEntradaEsperada(combinarFechaYHora(fila.getFecha(), turno.getIngreso()));
                fila.setHoraSalidaEsperada(combinarFechaYHora(fila.getFecha(), turno.getSalida()));
            }

            // Antes se juntaban TODAS las marcas del día (reloj + a mano) y se
            // usaba min()=entrada / max()=salida por orden cronológico — con
            // dos o más marcas funciona, pero con UNA sola marca real (marcó
            // entrada y nunca salida, o al revés) min y max son la MISMA
            // marca, y esa marca terminaba mostrándose en las dos columnas.
            // El legacy deja en blanco la pierna que falta (lo trata como
            // abandono, con el atraso de turno completo), que es lo correcto.
            //
            // Primer intento de fix (revertido el mismo día): separar por
            // CHECKTYPE ('I'/'O') en vez de adivinar por orden — en teoría
            // el dato correcto, pero confirmado EN VIVO que ese campo no es
            // confiable en el dispositivo real: días con dos marcas reales
            // que antes salían bien (una de entrada, una de salida) quedaron
            // con la salida en blanco después de ese cambio — CHECKTYPE
            // estaba clasificando mal. Se volvió a la idea de "distancia a
            // la hora esperada", pero aplicada a TODAS las marcas del día
            // (no sólo a las que "sinTipo" dejaba sin resolver): cada marca,
            // real o a mano, se asigna a la pierna (entrada/salida) cuya
            // hora esperada tenga más cerca. Con una sola marca cercana a la
            // entrada esperada, sólo llena entrada — la salida queda en
            // blanco, igual que el legacy. Sin hora esperada para comparar
            // (SIN_HORARIO), a falta de mejor criterio se completa primero
            // la entrada.
            List<BioCheckInOut> marcasReales = marcasPorDia.getOrDefault(dia, Collections.emptyList());
            List<Date> marcasManual = marcasManualesPorDia.getOrDefault(dia, Collections.emptyList());

            List<Date> todasLasMarcas = new ArrayList<>(marcasManual);
            for (BioCheckInOut m : marcasReales) todasLasMarcas.add(m.getCHECKTIME());

            List<Date> entradas = new ArrayList<>();
            List<Date> salidas = new ArrayList<>();
            for (Date m : todasLasMarcas) {
                boolean vaAEntrada;
                if (fila.getHoraEntradaEsperada() != null && fila.getHoraSalidaEsperada() != null) {
                    long distEntrada = Math.abs(m.getTime() - fila.getHoraEntradaEsperada().getTime());
                    long distSalida = Math.abs(m.getTime() - fila.getHoraSalidaEsperada().getTime());
                    vaAEntrada = distEntrada <= distSalida;
                } else {
                    vaAEntrada = entradas.isEmpty();
                }
                if (vaAEntrada) entradas.add(m); else salidas.add(m);
            }

            if (!entradas.isEmpty()) {
                Date entradaReal = Collections.min(entradas);
                fila.setHoraEntradaReal(entradaReal);
                fila.setEntradaManual(marcasManual.contains(entradaReal));
            }
            if (!salidas.isEmpty()) {
                Date salidaReal = Collections.max(salidas);
                fila.setHoraSalidaReal(salidaReal);
                fila.setSalidaManual(marcasManual.contains(salidaReal));
            }

            DiaNoHabilDto noHabil = noHabilesPorDia.get(dia);
            PermisoKardexDto permiso = permisoVigente(permisos, dia);

            if (noHabil != null && DiaNoHabilDto.FERIADO.equals(noHabil.getTipo())) {
                fila.setEstado(AsistenciaDiaDto.FERIADO);
                fila.setMotivo(noHabil.getMotivo());
            } else if (noHabil != null && DiaNoHabilDto.SABADO_LIBRE.equals(noHabil.getTipo())) {
                fila.setEstado(AsistenciaDiaDto.SABADO_LIBRE);
                fila.setMotivo(noHabil.getMotivo());
            } else if (permiso != null) {
                fila.setEstado("vac".equals(permiso.getTipoPermiso()) ? AsistenciaDiaDto.VACACION : AsistenciaDiaDto.PERMISO);
                fila.setMotivo(permiso.getDatoTipoPermiso());
                fila.setHoraInicioPermiso(permiso.getDesde());
                fila.setHoraFinPermiso(permiso.getHasta());
            } else if (turno == null) {
                fila.setEstado(AsistenciaDiaDto.SIN_HORARIO);
            } else if (fila.getHoraEntradaReal() == null || fila.getHoraSalidaReal() == null) {
                // Marcó UNA sola pierna (entrada sin salida, o al revés) y
                // ninguna marcación olvidada llenó la que falta — pedido
                // explícito del usuario: eso NO es un día trabajado normal
                // ("como si hubiera marcado su entrada y se hubiera ido a su
                // casa y ya"), es el mismo problema que un día sin marcar
                // nada. El Obs deja constancia de cuál de las dos faltó, a
                // diferencia de una falta completa (las dos null), que no
                // tiene nada más que contar.
                fila.setEstado(AsistenciaDiaDto.FALTA);
                if (fila.getHoraEntradaReal() != null) {
                    fila.setMotivo("Marcó entrada pero no registró salida (sin marcación olvidada)");
                } else if (fila.getHoraSalidaReal() != null) {
                    fila.setMotivo("Marcó salida pero no registró entrada (sin marcación olvidada)");
                }
            } else {
                fila.setEstado(AsistenciaDiaDto.TRABAJADO);
                // TRABAJADO nunca trae motivo por ningún otro camino (sólo
                // FERIADO/SABADO_LIBRE/PERMISO/VACACION lo setean arriba) —
                // seguro pisarlo acá para que el reporte diga POR QUÉ el día
                // quedó marcado como trabajado pese a faltarle una marca real
                // del reloj — acá SÍ están las dos piernas, una o las dos
                // completadas a mano.
                if (fila.isEntradaManual() || fila.isSalidaManual()) {
                    String pierna =
                            fila.isEntradaManual() && fila.isSalidaManual() ? "Entrada y salida"
                                    : fila.isEntradaManual() ? "Entrada"
                                    : "Salida";
                    fila.setMotivo(pierna + " registrada a mano (marcación olvidada)");
                }
            }

            fila.setMinutosAtraso(calcularMinutosAtraso(fila.getEstado(), turno,
                    fila.getHoraEntradaReal(), fila.getHoraSalidaReal(),
                    fila.getHoraEntradaEsperada(), fila.getHoraSalidaEsperada()));

            resultado.add(fila);
        }

        return resultado;
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /** {@code pageSize} para {@link #idsEmpleadosActivos()} — "todos", no una página real. */
    private static final int TAMANIO_LOTE_EMPLEADOS_ACTIVOS = 100_000;

    /**
     * Ids de empleados de Bosque activos AHORA MISMO — para no mostrar en reportes
     * ni selectores a quien ya no está en la empresa, sin dejar de mostrarlo en la
     * pestaña Empleados/Verificación (esa vista no llama a este método). No es un
     * estado guardado en el módulo Biométrico: se recalcula en cada llamada contra
     * el estado real de Bosque, así que si el empleado vuelve a estar activo,
     * reaparece solo.
     *
     * <p><b>No usa {@link IEmpleado#obtenerListaEmpleados(int)}</b> ({@code p_list_Empleado
     * @ACCION='B'}) — probado en vivo y confirmado roto: el row mapper de
     * {@code EmpleadoDAO} lee 5 columnas y esa rama de la SP sólo devuelve 4
     * ("Invalid column index 5"), un bug pre-existente en ese método que esta
     * llamada fue la primera en ejercitar. En cambio usa
     * {@link IEmpleado#obtenerLstEmpleados} ({@code ACCION='Y'}) — el mismo método
     * y la misma rama de la SP que ya usa {@code RrhhController.obtenerLstEmpleados}
     * (`/rrhh/obtenerLstEmpleados`, que a su vez ya usa
     * {@code PermisosRrhhImpl.buscarEmpleados} del frontend) — con {@code pageSize}
     * bien alto para traer el padrón completo en una sola pasada en vez de paginar.
     */
    private Set<Integer> idsEmpleadosActivos() {
        return empleadoDao.obtenerLstEmpleados(null, 1, 1, TAMANIO_LOTE_EMPLEADOS_ACTIVOS, null).stream()
                .map(Empleado::getCodEmpleado)
                .collect(Collectors.toSet());
    }

    /**
     * Una fila por {@code idEmpleado}, no una por fila de {@code tbio_bioEmplBosqEmpl}.
     *
     * <p>Confirmado con un caso real: un empleado enlazado a DOS usuarios del
     * biométrico (dos filas de {@code tbio_bioEmplBosqEmpl} con el mismo
     * {@code idEmpleado} y distinto {@code idEmpleadBio} — probablemente un
     * re-enrolamiento en el reloj que dejó el enlace viejo sin desenlazar)
     * salía duplicado en el Resumen mensual y el Detallado-todos, con los
     * mismos números exactos en las dos filas — porque
     * {@link #calcularReporte} siempre resuelve el enlace por
     * {@code idEmpleado} y toma el primero que encuentra
     * ({@code cruce.get(0)}), sin importar cuál de las dos filas de
     * {@code tbio_bioEmplBosqEmpl} disparó el cálculo. El resultado nunca
     * cambia según cuál enlace se use acá — sólo hace falta no procesar al
     * mismo empleado dos veces.
     *
     * <p>No es un fix de los datos: el enlace duplicado en
     * {@code tbio_bioEmplBosqEmpl} sigue ahí (se limpia desde la pestaña
     * Empleados, desenlazando el {@code idEmpleadBio} que ya no corresponde)
     * — esto sólo hace que el reporte no lo muestre dos veces mientras tanto.
     */
    private static List<BioEmplBosqEmpl> distintosPorEmpleado(List<BioEmplBosqEmpl> lista) {
        Map<Long, BioEmplBosqEmpl> porEmpleado = new LinkedHashMap<>();
        for (BioEmplBosqEmpl e : lista) {
            porEmpleado.putIfAbsent(e.getIdEmpleado(), e);
        }
        return new ArrayList<>(porEmpleado.values());
    }

    /** Tolerancia por pierna (entrada tarde / salida temprano) — ver el javadoc de {@code AsistenciaDiaDto.minutosAtraso}. */
    private static final int TOLERANCIA_ATRASO_MIN = 10;

    /**
     * Minutos de atraso del día — ver el javadoc de {@link AsistenciaDiaDto#getMinutosAtraso()}
     * para la fórmula completa y cómo se reconstruyó.
     */
    private static int calcularMinutosAtraso(String estado, BioHrs turno, Date entradaReal, Date salidaReal,
                                              Date entradaEsperada, Date salidaEsperada) {
        if (!AsistenciaDiaDto.TRABAJADO.equals(estado) && !AsistenciaDiaDto.FALTA.equals(estado)) {
            return 0;
        }
        // Recibe entradaReal/salidaReal YA clasificadas por CHECKTYPE (ver el
        // loop de calcularReporte) en vez de recalcularlas con min()/max()
        // sobre una lista sin tipo — antes, con una sola marca real, min y
        // max daban la MISMA marca y este método la contaba dos veces sin
        // saberlo. Menos de las dos ("no marcó" esa pierna, entrada o salida)
        // es el mismo caso que antes se detectaba por tamaño de lista < 2.
        if (entradaReal == null || salidaReal == null) {
            // Duración real del turno ESE día (Hr Ingreso → Hr Salida, las mismas
            // columnas que el reporte le muestra al usuario) — no turno.getCantMinutos().
            // Caso real que lo confirmó: un turno de 09:00 a 12:00 (180 min) con
            // cantMinutos=240 guardado aparte en tbio_bioHrs — el campo suelto no
            // coincidía con el propio ingreso/salida de esa fila, y el reporte
            // mostraba un atraso que no cuadraba con las horas de al lado.
            // cantMinutos queda sólo de resguardo por si algún turno no tuviera
            // ingreso/salida cargados.
            long duracionTurno = minutosPositivosEntre(entradaEsperada, salidaEsperada);
            return duracionTurno > 0 ? (int) duracionTurno : (int) Math.round(turno.getCantMinutos());
        }
        long minutosTarde = minutosPositivosEntre(entradaEsperada, entradaReal);
        long minutosTemprano = minutosPositivosEntre(salidaReal, salidaEsperada);
        int atraso = 0;
        if (minutosTarde > TOLERANCIA_ATRASO_MIN) atraso += minutosTarde;
        if (minutosTemprano > TOLERANCIA_ATRASO_MIN) atraso += minutosTemprano;
        return atraso;
    }

    /** {@code hasta - desde} en minutos, nunca negativo (0 si {@code hasta} es anterior a {@code desde}). */
    private static long minutosPositivosEntre(Date desde, Date hasta) {
        if (desde == null || hasta == null) return 0;
        return Math.max(0, (hasta.getTime() - desde.getTime()) / 60_000);
    }

    /** El horario vigente PARA ESE DÍA: el de {@code inicio} más reciente que no sea posterior a {@code dia}. */
    private static BioHrEmpleado horarioVigente(List<BioHrEmpleado> asignaciones, LocalDate dia) {
        BioHrEmpleado mejor = null;
        LocalDate mejorInicio = null;
        for (BioHrEmpleado a : asignaciones) {
            if (a.getInicio() == null) continue;
            LocalDate inicio = toLocalDate(a.getInicio());
            if (!inicio.isAfter(dia) && (mejorInicio == null || inicio.isAfter(mejorInicio))) {
                mejor = a;
                mejorInicio = inicio;
            }
        }
        return mejor;
    }

    private static PermisoKardexDto permisoVigente(List<PermisoKardexDto> permisos, LocalDate dia) {
        for (PermisoKardexDto p : permisos) {
            if (p.getDesde() == null || p.getHasta() == null) continue;
            LocalDate desde = toLocalDate(p.getDesde());
            LocalDate hasta = toLocalDate(p.getHasta());
            if (!dia.isBefore(desde) && !dia.isAfter(hasta)) {
                return p;
            }
        }
        return null;
    }

    private static final java.time.format.DateTimeFormatter FORMATO_HORA = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

    /** {@code null} → {@code ""} (no "—", eso lo decide el llamador según el contexto: acá es un componente de un texto más largo). */
    private static String formatoHora(Date hora) {
        if (hora == null) return "";
        return hora.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().format(FORMATO_HORA);
    }

    private static Date combinarFechaYHora(Date fecha, Date hora) {
        if (fecha == null || hora == null) return null;
        LocalDate ld = toLocalDate(fecha);
        java.time.LocalTime lt = hora.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        return Date.from(ld.atTime(lt).atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date parseFecha(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            throw new SpBusinessException("Falta el parámetro checkTime.");
        }
        try {
            return Date.from(java.time.Instant.parse(iso));
        } catch (Exception e) {
            LocalDate ld = LocalDate.parse(iso.substring(0, 10));
            return toDate(ld);
        }
    }

    private static Map<String, Object> mapa(String clave, Object valor) {
        Map<String, Object> m = new HashMap<>();
        m.put(clave, valor);
        return m;
    }

    /**
     * {@code SpHelper.ejecutarAbmMap} ya lanza {@link SpBusinessException} si
     * {@code error != 0} — para cuando esto se llama, {@code resp} es éxito.
     * {@code data} es {@code idGenerado} (0 si la ACCION no generó un id
     * nuevo, p.ej. 'U'/'D').
     */
    private ResponseEntity<ApiResponse<?>> respuestaEscritura(RespuestaSp resp) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, resp.getIdGenerado(), 201));
    }

    private ResponseEntity<ApiResponse<?>> respuestaListado(List<?> lista) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ApiResponse<>(SUCCESS_MESSAGE, null, 204));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, 200));
    }
}
