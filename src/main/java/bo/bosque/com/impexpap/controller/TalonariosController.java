package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ITalonario;
import bo.bosque.com.impexpap.dao.ITalonarioDetalle;
import bo.bosque.com.impexpap.dao.ITalonarioGrupo;
import bo.bosque.com.impexpap.dao.ITalonarioPorGrupo;
import bo.bosque.com.impexpap.dao.ITipoRecibo;
import bo.bosque.com.impexpap.dto.EntregaLoteDto;
import bo.bosque.com.impexpap.dto.ReporteTalonarioDto;
import bo.bosque.com.impexpap.dto.TalonarioFiltroDto;
import bo.bosque.com.impexpap.dto.TalonarioLoteDto;
import bo.bosque.com.impexpap.model.Talonario;
import bo.bosque.com.impexpap.model.TalonarioDetalle;
import bo.bosque.com.impexpap.model.TalonarioGrupo;
import bo.bosque.com.impexpap.model.TalonarioPorGrupo;
import bo.bosque.com.impexpap.model.TipoRecibo;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modulo de Talonarios. Migracion del WizardTalonario de Bosque v2 (JSF).
 *
 * Cinco tablas, cinco modelos, un par p_abm_/p_list_ por tabla:
 *   tmto_tipoRecibo         -> TipoRecibo
 *   tmto_talonarioGrupo     -> TalonarioGrupo
 *   tmto_talonarioPorGrupo  -> TalonarioPorGrupo
 *   tmto_talonario          -> Talonario
 *   tmto_talonarioDetalle   -> TalonarioDetalle
 *
 * El estado de un talonario NO esta guardado: se deriva contando el log de
 * eventos. p_list_tmto_Talonario lo devuelve ya resuelto en estadoActual y
 * en los tres flags puedeEntregar / puedeDevolver / puedeCerrar.
 *
 * Las dos cargas en lote (alta masiva y entrega masiva) son @Transactional:
 * si falla una fila no queda ninguna. El legacy las hacia en un bucle sin
 * transaccion y dejaba media carga escrita.
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/talonarios")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TalonariosController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";
    private static final String SIN_DATOS = "No se encontraron registros";

    /**
     * Topes del alta masiva. No son caprichosos: sin ellos
     * {@code (bloqueInicial - 1) * 50} desborda el int en silencio y salen
     * folios negativos. Con MAX_BLOQUE el folio más alto posible es 50.000.000,
     * bien lejos de los 2.147.483.647 que aguanta un int.
     *
     * MAX_CANTIDAD_LOTE tiene aire de sobra: el lote más grande que se registró
     * en producción fue de 62 talonarios.
     */
    private static final int MAX_CANTIDAD_LOTE = 1000;
    private static final int MAX_BLOQUE = 1_000_000;
    private static final int MAX_CORRELATIVO = 999_999;

    /** Ninguno de los reportes usa subreportes, pero el exportador pide el arreglo. */
    private static final String[] SIN_SUBREPORTES = new String[0];

    /**
     * Tope de @cadSel en p_SAP_Rpt_tmntoTalonario: VARCHAR(400). Con
     * codTalonario de cuatro digitos mas la coma son cinco caracteres por
     * talonario, o sea 80.
     */
    private static final int MAX_CADSEL = 400;

    private static final Set<String> ACCIONES_SAP =
            new HashSet<>(Arrays.asList("A", "B", "C", "D"));

    /**
     * SimpleDateFormat no es thread-safe y el controller es un singleton: dos
     * pedidos simultaneos compartiendo una instancia devuelven fechas mezcladas.
     * Con ThreadLocal cada hilo tiene la suya.
     */
    private static final ThreadLocal<SimpleDateFormat> FORMATO_FECHA =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("dd/MM/yyyy"));

    private final ITipoRecibo tipoReciboDao;
    private final ITalonarioGrupo talonarioGrupoDao;
    private final ITalonarioPorGrupo talonarioPorGrupoDao;
    private final ITalonario talonarioDao;
    private final ITalonarioDetalle talonarioDetalleDao;
    private final JasperReportExport jasperReportExport;

    public TalonariosController(ITipoRecibo tipoReciboDao,
                                ITalonarioGrupo talonarioGrupoDao,
                                ITalonarioPorGrupo talonarioPorGrupoDao,
                                ITalonario talonarioDao,
                                ITalonarioDetalle talonarioDetalleDao,
                                JasperReportExport jasperReportExport) {
        this.tipoReciboDao = tipoReciboDao;
        this.talonarioGrupoDao = talonarioGrupoDao;
        this.talonarioPorGrupoDao = talonarioPorGrupoDao;
        this.talonarioDao = talonarioDao;
        this.talonarioDetalleDao = talonarioDetalleDao;
        this.jasperReportExport = jasperReportExport;
    }

    // ==================== TIPOS DE RECIBO ====================

    /** Alta si codTipoRecibo == 0, edicion si viene con id. */
    @PostMapping("/registrar-tipo")
    public ResponseEntity<ApiResponse<?>> registrarTipo(@RequestBody TipoRecibo mb) {
        return respuestaEscritura(
                tipoReciboDao.registrarTipoRecibo(mb, mb.getCodTipoRecibo() == 0 ? "I" : "U"));
    }

    /** Rebota si el tipo tiene talonarios o esta asignado a un grupo. */
    @PostMapping("/eliminar-tipo")
    public ResponseEntity<ApiResponse<?>> eliminarTipo(@RequestBody TipoRecibo mb) {
        return respuestaEscritura(tipoReciboDao.registrarTipoRecibo(mb, "D"));
    }

    @PostMapping("/obtener-tipo")
    public ResponseEntity<ApiResponse<?>> obtenerTipo(@RequestBody TipoRecibo mb) {
        return procesarLista(tipoReciboDao.obtenerTipoRecibo(mb.getCodTipoRecibo()), SIN_DATOS);
    }

    @PostMapping("/listar-tipos")
    public ResponseEntity<ApiResponse<?>> listarTipos() {
        return procesarLista(tipoReciboDao.listarTipoRecibo(), SIN_DATOS);
    }

    // ==================== GRUPOS ====================

    @PostMapping("/registrar-grupo")
    public ResponseEntity<ApiResponse<?>> registrarGrupo(@RequestBody TalonarioGrupo mb) {
        return respuestaEscritura(
                talonarioGrupoDao.registrarTalonarioGrupo(mb, mb.getCodGrupo() == 0 ? "I" : "U"));
    }

    /** Rebota si el grupo tiene tipos asignados. */
    @PostMapping("/eliminar-grupo")
    public ResponseEntity<ApiResponse<?>> eliminarGrupo(@RequestBody TalonarioGrupo mb) {
        return respuestaEscritura(talonarioGrupoDao.registrarTalonarioGrupo(mb, "D"));
    }

    @PostMapping("/obtener-grupo")
    public ResponseEntity<ApiResponse<?>> obtenerGrupo(@RequestBody TalonarioGrupo mb) {
        return procesarLista(talonarioGrupoDao.obtenerTalonarioGrupo(mb.getCodGrupo()), SIN_DATOS);
    }

    @PostMapping("/listar-grupos")
    public ResponseEntity<ApiResponse<?>> listarGrupos() {
        return procesarLista(talonarioGrupoDao.listarTalonarioGrupo(), SIN_DATOS);
    }

    // ==================== TIPOS POR GRUPO ====================

    @PostMapping("/asignar-tipo-grupo")
    public ResponseEntity<ApiResponse<?>> asignarTipoGrupo(@RequestBody TalonarioPorGrupo mb) {
        return respuestaEscritura(talonarioPorGrupoDao.registrarTalonarioPorGrupo(mb, "I"));
    }

    /** La tabla no admite UPDATE: para mover un tipo de grupo es quitar + asignar. */
    @PostMapping("/quitar-tipo-grupo")
    public ResponseEntity<ApiResponse<?>> quitarTipoGrupo(@RequestBody TalonarioPorGrupo mb) {
        return respuestaEscritura(talonarioPorGrupoDao.registrarTalonarioPorGrupo(mb, "D"));
    }

    /** Con codGrupo == 0 devuelve todas las asignaciones. */
    @PostMapping("/listar-tipos-por-grupo")
    public ResponseEntity<ApiResponse<?>> listarTiposPorGrupo(@RequestBody TalonarioPorGrupo mb) {
        Long codGrupo = mb.getCodGrupo() == 0 ? null : mb.getCodGrupo();
        return procesarLista(talonarioPorGrupoDao.listarPorGrupo(codGrupo), SIN_DATOS);
    }

    /** Los tipos que TODAVIA NO estan en el grupo, para el combo de agregar. */
    @PostMapping("/listar-tipos-disponibles")
    public ResponseEntity<ApiResponse<?>> listarTiposDisponibles(@RequestBody TalonarioPorGrupo mb) {
        return procesarLista(talonarioPorGrupoDao.listarTiposDisponibles(mb.getCodGrupo()), SIN_DATOS);
    }

    // ==================== TALONARIOS ====================

    @PostMapping("/registrar-talonario")
    public ResponseEntity<ApiResponse<?>> registrarTalonario(@RequestBody Talonario mb) {
        return respuestaEscritura(
                talonarioDao.registrarTalonario(mb, mb.getCodTalonario() == 0 ? "I" : "U"));
    }

    /** Rebota si el talonario ya tiene movimientos. */
    @PostMapping("/eliminar-talonario")
    public ResponseEntity<ApiResponse<?>> eliminarTalonario(@RequestBody Talonario mb) {
        return respuestaEscritura(talonarioDao.registrarTalonario(mb, "D"));
    }

    @PostMapping("/obtener-talonario")
    public ResponseEntity<ApiResponse<?>> obtenerTalonario(@RequestBody Talonario mb) {
        return procesarLista(talonarioDao.obtenerTalonario(mb.getCodTalonario()), SIN_DATOS);
    }

    /** Todos los filtros del DTO son opcionales; null = sin filtro. */
    @PostMapping("/listar-talonarios")
    public ResponseEntity<ApiResponse<?>> listarTalonarios(@RequestBody TalonarioFiltroDto filtro) {
        return procesarLista(talonarioDao.listarTalonario(
                filtro.getCodTipoRecibo(), filtro.getCodEmpresa(),
                filtro.getCodGrupo(), filtro.getCodEstadoActual(),
                filtro.getFechaDesde(), filtro.getFechaHasta(),
                filtro.getIncluirCerrados()), SIN_DATOS);
    }

    /** Listos para entregar o reentregar: nunca cerrados y libres. */
    @PostMapping("/listar-disponibles")
    public ResponseEntity<ApiResponse<?>> listarDisponibles(@RequestBody TalonarioFiltroDto filtro) {
        return procesarLista(talonarioDao.listarDisponibles(filtro.getCodGrupo()), SIN_DATOS);
    }

    // ==================== ALTA MASIVA ====================

    /**
     * Previsualiza el lote SIN escribir nada. Devuelve el mismo DTO con
     * talonarios[] armado y duplicados[] con los nroTalonario que ya existen.
     *
     * El legacy avisaba de los duplicados a mitad del guardado y los salteaba;
     * aca el usuario los ve antes de confirmar.
     */
    @PostMapping("/simular-lote")
    public ResponseEntity<ApiResponse<?>> simularLote(@RequestBody TalonarioLoteDto dto) {
        List<Talonario> lote = generarLote(dto);

        List<String> duplicados = new ArrayList<>();
        for (Talonario t : lote) {
            if (!talonarioDao.buscarPorNroTalonario(t.getNroTalonario()).isEmpty()) {
                duplicados.add(t.getNroTalonario());
            }
        }

        dto.setTalonarios(lote);
        dto.setDuplicados(duplicados);
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, dto, HttpStatus.OK.value()));
    }

    /**
     * Graba el lote. TODO O NADA: si falla uno no queda ninguno.
     *
     * Usa los talonarios[] que devolvio simular-lote; si vienen vacios los
     * regenera con la misma cabecera. Devuelve la lista de ids generados,
     * asi que del lado Flutter se consume con postAndReturnList, no con
     * postAndReturnId.
     */
    @PostMapping("/aplicar-lote")
    @Transactional
    public ResponseEntity<ApiResponse<?>> aplicarLote(@RequestBody TalonarioLoteDto dto) {
        List<Talonario> lote = (dto.getTalonarios() == null || dto.getTalonarios().isEmpty())
                ? generarLote(dto)
                : dto.getTalonarios();

        if (lote.isEmpty()) {
            throw new SpBusinessException("No hay talonarios que registrar.");
        }

        List<Long> ids = new ArrayList<>(lote.size());
        for (Talonario t : lote) {
            if (t.getAudUsuario() == 0) t.setAudUsuario(dto.getAudUsuario());
            RespuestaSp res = talonarioDao.registrarTalonario(t, "I");
            ejecutarEnLote(res, "el talonario Nro " + t.getNroTalonario());
            ids.add(res.getIdGenerado());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                "Se registraron " + ids.size() + " talonarios.", ids, HttpStatus.CREATED.value()));
    }

    // ==================== EVENTOS ====================

    /**
     * Registra un evento: 2 Entregado, 3 Devuelto, 4 Cerrado.
     * El 1 (Adquirido) lo genera el alta del talonario, no se envia por aca.
     * El SP valida la transicion contra el estado actual.
     */
    @PostMapping("/registrar-evento")
    public ResponseEntity<ApiResponse<?>> registrarEvento(@RequestBody TalonarioDetalle mb) {
        return respuestaEscritura(
                talonarioDetalleDao.registrarTalonarioDetalle(mb, mb.getCodDetalle() == 0 ? "I" : "U"));
    }

    /** Solo se puede borrar el ULTIMO evento, y nunca el inicial. */
    @PostMapping("/eliminar-evento")
    public ResponseEntity<ApiResponse<?>> eliminarEvento(@RequestBody TalonarioDetalle mb) {
        return respuestaEscritura(talonarioDetalleDao.registrarTalonarioDetalle(mb, "D"));
    }

    /** Historial de un talonario, del evento mas viejo al mas nuevo. */
    @PostMapping("/listar-eventos")
    public ResponseEntity<ApiResponse<?>> listarEventos(@RequestBody TalonarioDetalle mb) {
        return procesarLista(talonarioDetalleDao.listarPorTalonario(mb.getCodTalonario()), SIN_DATOS);
    }

    // ==================== ENTREGA MASIVA ====================

    /**
     * Entrega varios talonarios al mismo destinatario. TODO O NADA.
     *
     * El destinatario es excluyente: sucursal O empleado. El SP lo valida
     * igual, pero se chequea aca para no arrancar el lote en vano.
     */
    @PostMapping("/entregar-lote")
    @Transactional
    public ResponseEntity<ApiResponse<?>> entregarLote(@RequestBody EntregaLoteDto dto) {
        if (dto.getCodTalonarios() == null || dto.getCodTalonarios().isEmpty()) {
            throw new SpBusinessException("Debe seleccionar al menos un talonario.");
        }
        if (dto.getFechaEvento() == null) {
            throw new SpBusinessException("La fecha del evento es obligatoria.");
        }

        boolean tieneSuc = dto.getCodSucursal() > 0;
        boolean tieneEmp = dto.getCodEmpleado() > 0;
        if (!tieneSuc && !tieneEmp) {
            throw new SpBusinessException("Debe indicar una sucursal o un empleado como destinatario.");
        }
        if (tieneSuc && tieneEmp) {
            throw new SpBusinessException("El destinatario debe ser sucursal o empleado, no ambos.");
        }

        List<Long> ids = new ArrayList<>(dto.getCodTalonarios().size());
        for (Long codTalonario : dto.getCodTalonarios()) {
            TalonarioDetalle ev = new TalonarioDetalle();
            ev.setCodTalonario(codTalonario);
            ev.setCodEstado(2);                       // 2 = Entregado
            ev.setFechaEvento(dto.getFechaEvento());
            ev.setCodSucursal(dto.getCodSucursal());
            ev.setCodEmpleado(dto.getCodEmpleado());
            ev.setObservacion(dto.getObservacion());
            ev.setAudUsuario(dto.getAudUsuario());

            RespuestaSp res = talonarioDetalleDao.registrarTalonarioDetalle(ev, "I");
            ejecutarEnLote(res, "el talonario con id " + codTalonario);
            ids.add(res.getIdGenerado());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                "Se entregaron " + ids.size() + " talonarios.", ids, HttpStatus.CREATED.value()));
    }

    // ==================== REPORTES ====================

    /**
     * Inventario: que talonarios hay, de quien son y en que estado estan.
     *
     * Reemplaza a RptTalMantFisico y RptTalMantEntregad del wizard viejo, que
     * eran el mismo stored procedure con la accion escrita a mano adentro del
     * .jrxml ('M' y 'O') y sin un solo filtro. Con codEstadoActual este unico
     * endpoint produce esos dos y cuatro mas.
     */
    @PostMapping("/reporte-inventario")
    public ResponseEntity<?> reporteInventario(@RequestBody ReporteTalonarioDto d) {
        Map<String, Object> p = new HashMap<>();
        p.put("codTipoRecibo", d.getCodTipoRecibo());
        p.put("codEmpresa", d.getCodEmpresa());
        p.put("codGrupo", d.getCodGrupo());
        p.put("codEstadoActual", d.getCodEstadoActual());
        p.put("fechaDesde", aSqlDate(d.getFechaDesde()));
        p.put("fechaHasta", aSqlDate(d.getFechaHasta()));
        p.put("incluirCerrados", Boolean.TRUE.equals(d.getIncluirCerrados()));
        p.put("subtitulo", subtitulo(d, "alta"));
        return pdf("RptTalInventario", p);
    }

    /**
     * Trazabilidad: una fila por ENTREGA, con su devolucion si volvio.
     *
     * El grano es lo que cambia respecto del legacy. RptTalMantEntregad daba un
     * talonario por fila con su ultima entrega, y asi un talonario que salio y
     * volvio tres veces mostraba solo la tercera. Este contesta la pregunta que
     * de verdad se le hacia: que le presto a esta sucursal y cuanto hace que no
     * vuelve.
     */
    @PostMapping("/reporte-trazabilidad")
    public ResponseEntity<?> reporteTrazabilidad(@RequestBody ReporteTalonarioDto d) {
        Map<String, Object> p = new HashMap<>();
        p.put("codTalonario", d.getCodTalonario());
        p.put("codTipoRecibo", d.getCodTipoRecibo());
        p.put("codEmpresa", d.getCodEmpresa());
        p.put("codSucursal", d.getCodSucursal());
        p.put("codEmpleado", d.getCodEmpleado());
        p.put("fechaDesde", aSqlDate(d.getFechaDesde()));
        p.put("fechaHasta", aSqlDate(d.getFechaHasta()));
        p.put("subtitulo", subtitulo(d, "entrega"));
        return pdf("RptTalTrazabilidad", p);
    }

    /**
     * Ficha de un talonario: su historial completo, para imprimir y adjuntar a
     * un descargo. No existia en el legacy; cuando faltaba un talonario habia
     * que mirar la grilla en pantalla y copiarla a mano.
     */
    @PostMapping("/reporte-ficha")
    public ResponseEntity<?> reporteFicha(@RequestBody ReporteTalonarioDto d) {
        if (d.getCodTalonario() == null || d.getCodTalonario() <= 0) {
            throw new SpBusinessException("Indique el talonario del que quiere la ficha.");
        }
        Map<String, Object> p = new HashMap<>();
        p.put("codTalonario", d.getCodTalonario());
        return pdf("RptTalFicha", p);
    }

    /**
     * Conciliacion contra SAP. Migra RptMntoTalGlobalV4 (cobros) y
     * RptMntoTalSalidaGlobalV2 (salidas), que siguen apoyados en los
     * p_SAP_Rpt_* de produccion; esos SPs no se tocaron.
     *
     * Son dos .jrxml y no uno porque son dos stored procedures distintos, con
     * nombres de parametro distintos y tipos de campo distintos, y un .jrxml
     * admite un unico queryString. El endpoint es uno solo y elige por origen.
     *
     * accionSap: 'A' conciliados respetando el filtro, 'B' conciliados sin
     * filtro, 'C' conciliados renumerados, 'D' documentos de SAP que no
     * matchean ningun talonario. 'D' es la que justifica el reporte.
     */
    @PostMapping("/reporte-conciliacion-sap")
    public ResponseEntity<?> reporteConciliacionSap(@RequestBody ReporteTalonarioDto d,
                                                    Authentication auth) {
        String accion = d.getAccionSap() == null ? "A" : d.getAccionSap().trim().toUpperCase();
        if (!ACCIONES_SAP.contains(accion)) {
            throw new SpBusinessException(
                    "Acción de conciliación no válida: " + accion + ". Use A, B, C o D.");
        }
        boolean salidas = "salidas".equalsIgnoreCase(d.getOrigen());

        // En la accion 'D' los filtros de talonario NO se aplican, y no es que
        // falte implementarlos: esa rama lee el volcado completo de SAP con un
        // anti-join y no nombra la lista de habilitados en ningun lado. Sus
        // filas ademas traen codEmpresa centinela, codTipoRecibo 0 y
        // nroTalonario vacio, porque son justamente los documentos sin
        // talonario. Resolverlos igual seria gastar una consulta para nada y,
        // peor, anunciar en el PDF un recorte que no ocurrio.
        boolean filtraPorTalonario = !"D".equals(accion);
        String seleccion = filtraPorTalonario ? seleccionDeTalonarios(d) : "";
        boolean porSeleccion = !seleccion.isEmpty();

        Map<String, Object> p = new HashMap<>();
        p.put("user", DatosToken.codUsuarioDe(auth));
        p.put("acc", accion);
        // Va 1 en los dos, y conviene explicar por que, porque el SP de salidas
        // invita a pensar otra cosa.
        //
        // Cobros ignora @codGrupo: sus dos ramas tienen 'codGrupo = 1' escrito
        // a mano. Salidas SI lo usa, pero solo en el join de la semilla (L53);
        // los bloques que arman los talonarios habilitados tienen 'codGrupo = 2'
        // escrito a mano (L88, L100, L267). Esa asimetria hace parecer que a
        // salidas hay que mandarle 2.
        //
        // No: en esta base el grupo 2 ("Notas de Pedido") existe pero no tiene
        // NINGUN tipo asignado, asi que alcanza cero talonarios. Mandarle 2
        // dejaria la semilla sin nada con que emparejar y TODO documento de SAP
        // saldria como huerfano. El grupo con los 7 tipos y los 1045 talonarios
        // es el 1.
        //
        // Efecto colateral de ese hardcode: como los habilitados salen del
        // grupo 2, esa tabla queda vacia y el bucle de relleno de folios del SP
        // de salidas nunca llega a ejecutarse. Si alguna vez se le asignan
        // tipos al grupo 2, ese bucle empieza a correr —y ahi si importa
        // tenerlo reescrito.
        p.put("codG", 1);
        p.put("gs", porSeleccion ? "S" : "X");
        // Con lista vacia el legacy manda "," y hay que respetarlo: el bucle
        // que la parsea hace LEFT(@cadSel, CHARINDEX(',', @cadSel) - 1), asi
        // que sin coma final CHARINDEX da 0 y LEFT con -1 revienta.
        p.put("cadS", porSeleccion ? seleccion + "," : ",");

        // Solo en la accion 'D' el rango de fechas significa "fecha del documento
        // en SAP". En las otras significa fecha de alta del talonario y ya se
        // aplico al armar la lista que se le manda al SP; volver a aplicarlo
        // sobre DocDate vaciaba el reporte, porque un talonario dado de alta
        // este año tiene documentos de hace cinco.
        p.put("fechaDocDesde", filtraPorTalonario ? null : d.getFechaDesde());
        p.put("fechaDocHasta", filtraPorTalonario ? null : d.getFechaHasta());

        // Idem la empresa: en 'D' el codEmpresa de las filas es un centinela,
        // asi que lo unico con que se puede acotar es el nombre que manda SAP.
        p.put("empresaSap", filtraPorTalonario ? "" : nombreDeEmpresa(d.getCodEmpresa()));

        String[] catalogo = filtraPorTalonario ? new String[]{"", ""} : catalogoDeTalonarios();
        p.put("talonariosConocidos", catalogo[0]);
        p.put("rangosConocidos", catalogo[1]);
        p.put("subtitulo", subtituloSap(d, salidas, accion, porSeleccion));

        return pdf(salidas ? "RptTalConciliacionSapSalidas" : "RptTalConciliacionSapCobros", p);
    }

    /**
     * La linea de filtros del PDF de conciliacion, que tiene que decir la
     * verdad sobre cada modo.
     *
     * Un usuario filtro por fecha de alta, vio la columna de fecha con valores
     * de 2020 y penso que el sistema estaba roto. No lo estaba: en la accion
     * 'D' ese filtro nunca se aplica, y ademas la fecha que se muestra es la
     * del documento en SAP, que no tiene por que caer en el mismo rango. El
     * subtitulo callaba las dos cosas.
     */
    private String subtituloSap(ReporteTalonarioDto d, boolean salidas,
                                String accion, boolean porSeleccion) {
        StringBuilder sb = new StringBuilder();
        sb.append(salidas ? "Salidas de materiales" : "Cobros")
          .append(" · ").append(descripcionAccionSap(accion));

        if ("D".equals(accion)) {
            String empresa = nombreDeEmpresa(d.getCodEmpresa());
            if (empresa.isEmpty()) {
                sb.append(" · todas las empresas");
            } else {
                sb.append(" · Empresa (según SAP): ").append(empresa);
            }
            sb.append(" · sin filtro por tipo de recibo: estos documentos no tienen "
                    + "talonario, así que tampoco tienen tipo");
            if (d.getFechaDesde() != null || d.getFechaHasta() != null) {
                sb.append(" · Fecha del documento en SAP: ")
                  .append(d.getFechaDesde() == null ? "sin límite" : FORMATO_FECHA.get().format(d.getFechaDesde()))
                  .append(" a ")
                  .append(d.getFechaHasta() == null ? "sin límite" : FORMATO_FECHA.get().format(d.getFechaHasta()));
            }
            return sb.toString();
        }

        sb.append(porSeleccion ? " · " + subtitulo(d, "alta") : " · sin filtros");
        return sb.toString();
    }

    /**
     * Todos los nroTalonario del Bosque, entre comas (",ER1001,ER1002,").
     *
     * Lo usa el reporte para distinguir dos huerfanos que se parecen y se
     * arreglan distinto: el codigo que SAP trae escrito no existe en el Bosque
     * —error de tipeo alla— o si existe, y entonces lo que fallo fue el rango
     * de folios o la empresa. Sin esta distincion el reporte dice que hay un
     * problema pero no cual.
     *
     * Las comas de los extremos no son adorno: permiten buscar ",X," y evitar
     * que 'ER10' matchee dentro de 'ER1001'.
     */
    /**
     * El nombre de una empresa, o cadena vacia si no se filtro por ninguna.
     *
     * Se resuelve desde los talonarios y no desde tb_empresa porque el modulo
     * no tiene DAO de empresas: el listado ya trae datoEmpresa resuelto, y
     * pedir uno solo alcanza. Si no hay ningun talonario de esa empresa no se
     * puede nombrar, y entonces vale mas no acotar que acotar por un nombre
     * inventado —el .jrxml trata la cadena vacia como "sin filtro".
     */
    private String nombreDeEmpresa(Long codEmpresa) {
        if (codEmpresa == null || codEmpresa <= 0) {
            return "";
        }
        List<Talonario> l = talonarioDao.listarTalonario(
                null, codEmpresa, null, null, null, null, Boolean.TRUE);
        if (l == null || l.isEmpty() || l.get(0).getDatoEmpresa() == null) {
            return "";
        }
        return l.get(0).getDatoEmpresa().trim();
    }

    /**
     * Los dos textos que el reporte de conciliacion necesita para explicar un
     * huerfano, resueltos en UNA sola consulta:
     *
     *   [0] existencia : ",IR1307,ER1001,"
     *   [1] rangos     : ",IR1307|10301 a 10350,ER1001|1 a 50,"
     *
     * Van juntos porque salen de la misma lista y pedirla dos veces era una ida
     * y vuelta a la base regalada.
     *
     * EN MAYUSCULAS y sin espacios a proposito. El collation de SQL Server no
     * distingue mayusculas —'ir1307' empareja con 'IR1307' sin problema— pero
     * Java si, y el reporte compara en Java: normalizando de este lado, el
     * .jrxml puede replicar lo que hace el SP y no acusar de "mal escrito" a un
     * codigo que en realidad concilio bien. Los espacios, en cambio, SI rompen
     * el match, y esos si valen la pena reportarse.
     *
     * Las comas de los extremos permiten buscar ",X," y evitar que 'ER10'
     * matchee dentro de 'ER1001'.
     */
    private String[] catalogoDeTalonarios() {
        List<Talonario> todos = talonarioDao.listarTalonario(
                null, null, null, null, null, null, Boolean.TRUE);
        if (todos == null || todos.isEmpty()) {
            return new String[]{"", ""};
        }
        StringBuilder existencia = new StringBuilder(",");
        StringBuilder rangos = new StringBuilder(",");
        for (Talonario t : todos) {
            if (t.getNroTalonario() == null) {
                continue;
            }
            String cod = t.getNroTalonario().replace(" ", "").toUpperCase();
            existencia.append(cod).append(',');
            rangos.append(cod).append('|')
                  .append(t.getNumeracionInicial()).append(" a ")
                  .append(t.getNumeracionFinal()).append(',');
        }
        return new String[]{existencia.toString(), rangos.toString()};
    }

    /**
     * Los codTalonario que el reporte de SAP tiene que recorrer, separados por
     * coma. Cadena vacia = sin acotar.
     *
     * **Por que hace falta.** El SP recorre un bucle POR CADA FOLIO de cada
     * talonario habilitado, y en cada vuelta cuenta sobre una tabla variable
     * que va creciendo —sin indices ni estadisticas—, asi que el costo es
     * cuadratico. Sobre los 1045 talonarios de hoy son 52.250 vueltas y el
     * reporte no termina ni en cinco minutos.
     *
     * El unico freno que el SP ofrece es @grupoSeleccion='S' con la lista de
     * codTalonario en @cadSel. Los filtros por empresa, tipo y fecha no
     * existen ahi, asi que se resuelven aca contra p_list_tmto_Talonario —que
     * si los tiene— y se traducen a esa lista. El SP de produccion no se toca.
     *
     * Si el llamador ya mando una seleccion explicita, esa manda.
     */
    private String seleccionDeTalonarios(ReporteTalonarioDto d) {
        String explicita = d.getSeleccion() == null ? "" : d.getSeleccion().trim();
        if (!explicita.isEmpty() && !",".equals(explicita)) {
            String limpia = explicita.endsWith(",")
                    ? explicita.substring(0, explicita.length() - 1) : explicita;
            exigirQueEntre(limpia, contarComas(limpia) + 1);
            return limpia;
        }

        boolean hayFiltro = d.getCodEmpresa() != null || d.getCodTipoRecibo() != null
                || d.getFechaDesde() != null || d.getFechaHasta() != null;
        if (!hayFiltro) {
            return "";
        }

        // incluirCerrados en TRUE a proposito: un talonario cerrado sigue
        // teniendo documentos en SAP que conciliar, y excluirlo escondería
        // justamente las diferencias viejas que este reporte busca.
        List<Talonario> lista = talonarioDao.listarTalonario(
                d.getCodTipoRecibo(), d.getCodEmpresa(), null, null,
                d.getFechaDesde(), d.getFechaHasta(), Boolean.TRUE);

        if (lista == null || lista.isEmpty()) {
            throw new SpBusinessException(
                    "Ningún talonario coincide con esos filtros, así que no hay nada que conciliar.");
        }

        StringBuilder sb = new StringBuilder();
        for (Talonario t : lista) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(t.getCodTalonario());
        }
        exigirQueEntre(sb.toString(), lista.size());
        return sb.toString();
    }

    /**
     * @cadSel es VARCHAR(400) en el SP. Pasarse no da error: SQL Server trunca
     * el parametro en silencio y el reporte sale con menos talonarios de los
     * pedidos, que es la peor forma de fallar —un informe de conciliacion al
     * que le faltan filas parece completo—. Por eso se rebota antes.
     */
    private void exigirQueEntre(String cadena, int cuantos) {
        if (cadena.length() + 1 > MAX_CADSEL) {
            throw new SpBusinessException(
                    "Los filtros seleccionan " + cuantos + " talonarios y el reporte de SAP "
                    + "admite hasta " + (MAX_CADSEL / 5) + " por vez. "
                    + "Acotá más: agregá la empresa, el tipo de recibo o un rango de fechas más corto.");
        }
    }

    private int contarComas(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ',') {
                n++;
            }
        }
        return n;
    }

    /**
     * Custodia: quien tiene talonarios ahora y quien registro la entrega.
     *
     * Trazabilidad ya decia que se entrego a quien y si volvio, pero devolvia
     * las 955 entregas historicas sin forma de quedarse con las abiertas: para
     * saber quien debe algo hoy habia que leer 955 filas. Aca el default son
     * las 364 vigentes.
     *
     * Y trae el dato que faltaba en las tres acciones anteriores: quien cargo
     * la entrega. Ojo con como se lee —el reporte lo aclara—: audUsuario es
     * quien tipeo el registro, y en seis anios un solo operador cargo el 97,8%.
     */
    @PostMapping("/reporte-custodia")
    public ResponseEntity<?> reporteCustodia(@RequestBody ReporteTalonarioDto d) {
        Map<String, Object> p = new HashMap<>();
        p.put("codTalonario", d.getCodTalonario());
        p.put("codTipoRecibo", d.getCodTipoRecibo());
        p.put("codEmpresa", d.getCodEmpresa());
        p.put("codSucursal", d.getCodSucursal());
        p.put("codEmpleado", d.getCodEmpleado());
        p.put("fechaDesde", aSqlDate(d.getFechaDesde()));
        p.put("fechaHasta", aSqlDate(d.getFechaHasta()));
        p.put("incluirCerrados", Boolean.TRUE.equals(d.getIncluirCerrados()));
        p.put("tipoDestinatario", normalizarTipoDestinatario(d.getTipoDestinatario()));
        p.put("diasMinimos", d.getDiasMinimos());
        p.put("fechaCorte", aSqlDate(d.getFechaCorte()));
        p.put("subtitulo", subtituloCustodia(d));
        return pdf("RptTalCustodia", p);
    }

    /** Null salvo que sea exactamente 'S' o 'E'; el SP trata null como ambos. */
    private String normalizarTipoDestinatario(String valor) {
        if (valor == null) {
            return null;
        }
        String v = valor.trim().toUpperCase();
        return ("S".equals(v) || "E".equals(v)) ? v : null;
    }

    private String subtituloCustodia(ReporteTalonarioDto d) {
        List<String> partes = new ArrayList<>();
        partes.add(Boolean.TRUE.equals(d.getIncluirCerrados())
                ? "Todas las entregas, incluidas las ya cerradas"
                : "Solo lo que sigue en poder de alguien");

        String tipo = normalizarTipoDestinatario(d.getTipoDestinatario());
        if ("S".equals(tipo)) {
            partes.add("Solo sucursales");
        } else if ("E".equals(tipo)) {
            partes.add("Solo personal");
        }
        if (d.getCodTipoRecibo() != null && d.getCodTipoRecibo() > 0) {
            partes.add("Tipo: " + nombreDeTipo(d.getCodTipoRecibo()));
        }
        if (d.getDiasMinimos() != null && d.getDiasMinimos() > 0) {
            partes.add("Con " + d.getDiasMinimos() + " días o más en poder");
        }
        if (d.getFechaDesde() != null || d.getFechaHasta() != null) {
            partes.add("Fecha de entrega: "
                    + (d.getFechaDesde() == null ? "sin límite" : FORMATO_FECHA.get().format(d.getFechaDesde()))
                    + " a "
                    + (d.getFechaHasta() == null ? "sin límite" : FORMATO_FECHA.get().format(d.getFechaHasta())));
        }
        return String.join("  ·  ", partes);
    }

    // ---------- helpers de reportes ----------

    /**
     * Los cuatro reportes salen por aca.
     *
     * Se usa exportPDFConSubreportes y no exportPDFStatic aunque ninguno tenga
     * subreportes: ese metodo compila el .jrxml en caliente, asi que editar el
     * XML se ve enseguida. exportPDFStatic exige un .jasper precompilado y
     * dejaria los cambios invisibles hasta regenerarlo.
     *
     * Sin Content-Disposition, que es la convencion del resto de los endpoints
     * Jasper del proyecto: los bytes salen pelados y el nombre lo pone el
     * front, que es quien sabe con que filtros lo pidio.
     */
    private ResponseEntity<?> pdf(String nombreReporte, Map<String, Object> params) {
        byte[] bytes = jasperReportExport.exportPDFConSubreportes(nombreReporte,
                SIN_SUBREPORTES, params);
        if (bytes == null || bytes.length == 0) {
            throw new SpBusinessException("El reporte no devolvió datos para los filtros indicados.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(bytes.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /** Los parametros de fecha del .jrxml son java.sql.Date; el DTO trae util.Date. */
    private java.sql.Date aSqlDate(java.util.Date d) {
        return d == null ? null : new java.sql.Date(d.getTime());
    }

    /**
     * La linea de filtros que va bajo el titulo del PDF.
     *
     * No nombra la empresa a proposito: los dos reportes que la aceptan como
     * filtro ya agrupan por empresa e imprimen su nombre como cabecera de
     * grupo, asi que repetirlo arriba seria redundante —y el backend no tiene
     * de donde sacar ese nombre sin una consulta extra.
     *
     * ambitoFecha aclara sobre que fecha se aplico el rango: en inventario es
     * el alta del talonario y en trazabilidad la fecha de la entrega. Es el
     * mismo par de parametros pero no la misma pregunta.
     */
    private String subtitulo(ReporteTalonarioDto d, String ambitoFecha) {
        List<String> partes = new ArrayList<>();

        if (d.getCodTipoRecibo() != null && d.getCodTipoRecibo() > 0) {
            partes.add("Tipo: " + nombreDeTipo(d.getCodTipoRecibo()));
        }
        if (d.getCodGrupo() != null && d.getCodGrupo() > 0) {
            partes.add("Grupo: " + nombreDeGrupo(d.getCodGrupo()));
        }
        if (d.getCodEstadoActual() != null && d.getCodEstadoActual() > 0) {
            partes.add("Estado: " + nombreDeEstado(d.getCodEstadoActual()));
        }
        if (d.getFechaDesde() != null || d.getFechaHasta() != null) {
            partes.add("Fecha de " + ambitoFecha + ": "
                    + (d.getFechaDesde() == null ? "sin límite" : FORMATO_FECHA.get().format(d.getFechaDesde()))
                    + " a "
                    + (d.getFechaHasta() == null ? "sin límite" : FORMATO_FECHA.get().format(d.getFechaHasta())));
        }
        if (Boolean.TRUE.equals(d.getIncluirCerrados())) {
            partes.add("incluye cerrados");
        }

        return partes.isEmpty() ? "Sin filtros: todo el inventario" : String.join("  ·  ", partes);
    }

    private String nombreDeTipo(Long codTipoRecibo) {
        List<TipoRecibo> l = tipoReciboDao.obtenerTipoRecibo(codTipoRecibo);
        if (l == null || l.isEmpty()) {
            return "#" + codTipoRecibo;
        }
        TipoRecibo t = l.get(0);
        return (t.getSigla() == null || t.getSigla().trim().isEmpty())
                ? t.getNombre() : t.getSigla() + " — " + t.getNombre();
    }

    private String nombreDeGrupo(Long codGrupo) {
        List<TalonarioGrupo> l = talonarioGrupoDao.obtenerTalonarioGrupo(codGrupo);
        return (l == null || l.isEmpty()) ? "#" + codGrupo : l.get(0).getNombre();
    }

    /**
     * Los cuatro estados de v_tipos grupo 45. Van escritos aca y no leidos de
     * la vista porque es una linea de texto de un PDF: una consulta mas por
     * reporte para cuatro literales que no cambian desde 2016 no se paga.
     */
    private String nombreDeEstado(Integer codEstado) {
        switch (codEstado) {
            case 1:  return "Adquirido";
            case 2:  return "Entregado";
            case 3:  return "Devuelto";
            case 4:  return "Cerrado";
            default: return "#" + codEstado;
        }
    }

    private String descripcionAccionSap(String accion) {
        switch (accion) {
            case "A": return "conciliados (con filtro)";
            case "B": return "conciliados (sin filtro)";
            case "C": return "conciliados (renumerados)";
            case "D": return "documentos sin talonario";
            default:  return accion;
        }
    }

    // ==================== HELPERS ====================

    /**
     * Arma el lote en memoria a partir de la cabecera. No toca la base.
     *
     * Replica la numeracion del legacy (cargarListTalGenerads):
     *   - cada talonario cubre 50 recibos correlativos
     *   - el primer folio sale de (bloqueInicial - 1) * 50 + 1
     *   - el nroTalonario es prefijo + correlativo con ceros a 3 digitos
     *   - el prefijo es la sigla del tipo, o anio + sigla si es por gestion
     *   - con COSTO_TOTAL el costo se divide entre la cantidad
     */
    private List<Talonario> generarLote(TalonarioLoteDto dto) {
        if (dto.getCantidad() <= 0) {
            throw new SpBusinessException("La cantidad de talonarios debe ser mayor a cero.");
        }
        if (dto.getCantidad() > MAX_CANTIDAD_LOTE) {
            throw new SpBusinessException(
                    "No se pueden generar más de " + MAX_CANTIDAD_LOTE + " talonarios por lote. "
                    + "El lote más grande registrado fue de 62.");
        }
        if (dto.getBloqueInicial() < 1 || dto.getBloqueInicial() > MAX_BLOQUE) {
            throw new SpBusinessException(
                    "El bloque de folios debe estar entre 1 y " + MAX_BLOQUE + ".");
        }
        if (dto.getCorrelativoInicial() < 1 || dto.getCorrelativoInicial() > MAX_CORRELATIVO) {
            throw new SpBusinessException(
                    "El correlativo inicial debe estar entre 1 y " + MAX_CORRELATIVO + ".");
        }
        if (dto.getCosto() < 0) {
            throw new SpBusinessException("El costo no puede ser negativo.");
        }

        List<TipoRecibo> tipos = tipoReciboDao.obtenerTipoRecibo(dto.getCodTipoRecibo());
        if (tipos.isEmpty()) {
            throw new SpBusinessException("No existe el tipo de recibo seleccionado.");
        }
        TipoRecibo tipo = tipos.get(0);

        String sigla = tipo.getSigla() == null ? "" : tipo.getSigla();
        String prefijo = dto.isPorGestion() ? dto.getAnio() + sigla : sigla;

        int folio = dto.getBloqueInicial() >= 2
                ? ((dto.getBloqueInicial() - 1) * TalonarioLoteDto.RECIBOS_POR_TALONARIO) + 1
                : 1;

        double costoUnitario = dto.getTipoCosto() == TalonarioLoteDto.COSTO_TOTAL
                ? dto.getCosto() / dto.getCantidad()
                : dto.getCosto();

        List<Talonario> lote = new ArrayList<>(dto.getCantidad());
        for (int i = 0; i < dto.getCantidad(); i++) {
            Talonario t = new Talonario();
            t.setCodTipoRecibo(dto.getCodTipoRecibo());
            t.setCodEmpresa(dto.getCodEmpresa());
            t.setNroTalonario(prefijo + String.format("%03d", dto.getCorrelativoInicial() + i));
            t.setNumeracionInicial(folio);
            t.setNumeracionFinal(folio + TalonarioLoteDto.RECIBOS_POR_TALONARIO - 1);
            t.setCostoBs(costoUnitario);
            t.setEstado("1");
            t.setObservacion(dto.getObservacion());
            t.setAudUsuario(dto.getAudUsuario());
            lote.add(t);

            folio += TalonarioLoteDto.RECIBOS_POR_TALONARIO;
        }
        return lote;
    }

    /**
     * Corta el lote al primer error.
     *
     * El mensaje dice explicitamente que no se guardo nada: la @Transactional
     * revierte todo, y el texto del legacy ("Problema(s) al registrar N")
     * hacia creer que se habia guardado una parte.
     */
    private void ejecutarEnLote(RespuestaSp res, String contexto) {
        if (res.getError() != 0) {
            throw new SpBusinessException(
                    "No se guardó ningún registro. Falló " + contexto + ": " + res.getErrormsg());
        }
    }

    private ResponseEntity<ApiResponse<?>> respuestaEscritura(RespuestaSp res) {
        HttpStatus status = res.getError() == 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), status.value()));
    }

    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, String mensajeVacio) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}
