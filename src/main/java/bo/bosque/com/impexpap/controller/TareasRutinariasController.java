// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\controller\TareasRutinariasController.java
package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.GeneracionAlInstante;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.dto.CochesDelDiaRequest;
import bo.bosque.com.impexpap.dto.DependientesJefeRequest;
import bo.bosque.com.impexpap.dto.MarcarLlegadaCocheRequest;
import bo.bosque.com.impexpap.dto.CajaChicaListarRequest;
import bo.bosque.com.impexpap.dto.CerrarLoteCajaChicaRequest;
import bo.bosque.com.impexpap.dto.ConfirmarCierreOperacionesRequest;
import bo.bosque.com.impexpap.dto.ConfirmarVerificacionRequest;
import bo.bosque.com.impexpap.dto.FinalizarCajaChicaRequest;
import bo.bosque.com.impexpap.dto.MarcarArqueoRevisadoRequest;
import bo.bosque.com.impexpap.dto.MarcarLlegadaVerificadaRequest;
import bo.bosque.com.impexpap.dto.ObtenerDeHoyRequest;
import bo.bosque.com.impexpap.dto.RegistrarArqueoRequest;
import bo.bosque.com.impexpap.dto.RegistrarCajaFuerteRequest;
import bo.bosque.com.impexpap.dto.RegistrarEgresoCajaChicaRequest;
import bo.bosque.com.impexpap.dto.RegistrarTareaRutinariaRequest;
import bo.bosque.com.impexpap.dto.VerificarTraspasoRequest;
import bo.bosque.com.impexpap.dto.BitacoraCumplimientoDto;
import bo.bosque.com.impexpap.dto.BitacoraFiltroRequest;
import bo.bosque.com.impexpap.dto.CerrarTraspasoTesBaseRequest;
import bo.bosque.com.impexpap.dto.DiaRevisadoTesBaseDto;
import bo.bosque.com.impexpap.dto.DiagnosticoGeneracionDto;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.ListadoConEstado;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Controlador REST para el módulo de Tareas Rutinarias (tac_*): definición y
 * asignación de tareas recurrentes por cargo, arqueo de caja, caja chica,
 * caja fuerte (llegadas), control de coches y catálogos asociados.
 * <p>
 * <b>Arquitectura:</b> Sin JPA/Hibernate. Toda la persistencia se realiza
 * mediante Stored Procedures de SQL Server, invocados a través de
 * JdbcTemplate + SimpleJdbcCall vía la utilidad SpHelper, igual que el resto
 * del sistema (ver PagosExtranjerosController para el mismo patrón aplicado
 * al módulo tpex).
 * <p>
 * <b>Convención de SPs (nuevos, prefijo "tac_", NO tocan los procs legacy
 * que sigue usando Bosque v2/JSF):</b>
 * <ul>
 *   <li>ABM: {@code p_abm_tac_<Entidad>} — ACCION: "I"=Insert, "U"=Update, "D"=Delete</li>
 *   <li>Listado: {@code p_list_tac_<Entidad>} — ACCION: "L"=filtro opcional por cualquier columna (null = todas)</li>
 * </ul>
 * <p>
 * Dos endpoints especiales, fuera del patrón CRUD estándar, para el caso de
 * uso "un jefe de área/gerente programa tareas rutinarias a sus
 * dependientes": {@link #listarDependientesJefe} y
 * {@link #registrarTareaRutinariaConCargos} — ver p_list_tac_dependientesJefe
 * / p_registrar_tac_tareaRutinariaConCargos.
 * <p>
 * <b>Seguridad:</b> Todos los endpoints requieren JWT válido — cubierto
 * globalmente por {@code anyRequest().authenticated()} en MainSecurity, no
 * por una anotación de clase (hallazgo de code-review 2026-09-07: un
 * {@code @PreAuthorize} de clase con ROLE_ADM/ROLE_LIM devolvía 403 en la
 * mayoría de los endpoints, incluyendo flujos operativos normales — Coches,
 * Caja Fuerte, Caja Chica, Arqueo, Cierre/Verificar Cierre — que deben ser
 * usables por cualquier usuario autenticado, como ya documentan los
 * comentarios de esos métodos). Los endpoints que sí requieren ROLE_ADM lo
 * declaran explícitamente con su propio {@code @PreAuthorize} (ver
 * {@link #registrarTarRuXCargo} y el resto de los CRUD genéricos de alto
 * riesgo).
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/tareas-rutinarias")
public class TareasRutinariasController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    // El umbral de nivel jerárquico (trh_cargo.codNivel) para considerar a
    // alguien "jefe de área/gerente" ya NO vive en Spring (application.properties)
    // — se movió a dbo.tac_configuracion (clave='nivelMaximoJefe'), sin
    // override desde Java. Los SPs (p_list_tac_dependientesJefe,
    // p_registrar_tac_tareaRutinariaConCargos) lo resuelven ellos mismos.
    // Ver 2026-09-02_tac_tareaRutinaria_08/09/10.

    // ==================== DEPENDENCIAS (19 DAOs estándar + 2 especiales) ====================

    private final IDocumentacion documentacionDao;
    private final ITareaRutinaria tareaRutinariaDao;
    private final ILlegada llegadaDao;
    private final ITraspasoMovCaja traspasoMovCajaDao;
    private final IAccionTareaRutinaria accionTareaRutinariaDao;
    private final IArqueoCajaSucursales arqueoCajaSucursalesDao;
    private final IMontoCajaChicaXSuc montoCajaChicaXSucDao;
    private final ICajaChica cajaChicaDao;
    private final ISucXMovCaja sucXMovCajaDao;
    private final IBitTareaRuti bitTareaRutiDao;
    private final IMovCaja movCajaDao;
    private final IFrecuencia frecuenciaDao;
    private final ICorte corteDao;
    private final ITarRuXCargo tarRuXCargoDao;
    private final IDetArqueoCajaSucursales detArqueoCajaSucursalesDao;
    private final IVale valeDao;
    private final IDetDocumentacion detDocumentacionDao;
    private final ICocheLlegadas cocheLlegadasDao;
    private final ICoche cocheDao;

    /** Lista los cargos dependientes del jefe autenticado (tac_tarRuXCargo, p_list_tac_dependientesJefe). */
    private final IDependientesJefe dependientesJefeDao;
    /** Crea una tarea rutinaria y la asigna a uno o más cargos en una transacción (p_registrar_tac_tareaRutinariaConCargos). */
    private final ITareaRutinariaConCargos tareaRutinariaConCargosDao;
    /** Flujo "Coches" (idATR=6): siembra+lista del día (p_list_tac_CocheLlegadas ACCION='D') y marca de llegada con cierre automático (p_abm_tac_CocheLlegadas ACCION='M'). */
    private final ICoches cochesDao;
    /** Flujo "Caja Fuerte" (idATR=4): alta transaccional de llegadas + cierre de la tarea (p_abm_tac_Llegada ACCION='R'). */
    private final ICajaFuerte cajaFuerteDao;
    /** Flujo "Arqueo de Caja" (idATR=2): alta transaccional de cabecera+detalle+cierre (p_abm_tac_ArqueoCajaSucursales ACCION='R'). */
    private final IArqueoRegistro arqueoRegistroDao;
    /** Flujo "Caja Chica" (idATR=7): lote por sucursal (p_list_tac_CajaChica ACCION='D'), egresos con saldo corrido (p_abm_tac_CajaChica ACCION='R'), cierre de la tarea del día (p_abm_tac_CajaChica ACCION='F'), cierre de lote + apertura de uno nuevo (p_abm_tac_CajaChica ACCION='C'), historial de lotes (p_list_tac_CajaChica ACCION='H'). */
    private final ICajaChicaFlujo cajaChicaFlujoDao;
    /** Flujo "Cierre de Operaciones" (idATR=3): cierra la ocurrencia (p_abm_tac_BitTareaRuti ACCION='C') y lee los cheques del día (p_SAP_Rpt_ImpChequesPR ACCION='A'). */
    private final ICierreOperaciones cierreOperacionesDao;
    /** Flujo "Verificar Cierre de Operaciones" (idATR=5, paso supervisor): revisa arqueos/llegadas del día (p_abm_tac_ArqueoCajaSucursales ACCION='V', p_abm_tac_Llegada ACCION='V') y cierra (p_abm_tac_BitTareaRuti ACCION='V'). */
    private final IVerificarCierreOperaciones verificarCierreOperacionesDao;
    /** Flujo "Verificar Traspaso de Efectivo Entre Sistemas" (idATR=11, TesBase): pendientes (p_list_TesBase ACCION='B'), cerrar una (p_abm_TesBase ACCION='C') y sin pendientes (ACCION='S'). Archivo SQL 56. */
    private final ITesBase tesBaseDao;
    /** Resuelve el codEmpleado de quien llama desde el JWT — nunca se confía en uno mandado por el cliente. */
    private final AccesoModuloHelper accesoModuloHelper;
    /** Búsqueda de empleados para el picker "Empleado Destino" de Caja Chica (reusa el DAO real de RRHH, sin el gate ROLE_ADM/ROLE_LIM de /rrhh/obtenerLstEmpleados — un cajero normal no tiene esos roles). */
    private final IEmpleado empleadoDao;
    /** Genera el PDF de RptArqueoDeCaja (y el resto de reportes Jasper del módulo) — ver {@link #reporteArqueoDeCajaPdf}. */
    private final JasperReportExport jasperReportExport;
    /** Corre el generador apenas se asigna o se edita una tarea, sin esperar al Job (2026-10-05). */
    private final GeneracionAlInstante generacionAlInstante;
    /** Las pantallas que cada usuario tiene en el menú ({@code p_list_VistaUsuario 'M'}) — ver {@link #tienePantalla}. */
    private final IVistaDao vistaDao;

    public TareasRutinariasController(
            IDocumentacion documentacionDao,
            ITareaRutinaria tareaRutinariaDao,
            ILlegada llegadaDao,
            ITraspasoMovCaja traspasoMovCajaDao,
            IAccionTareaRutinaria accionTareaRutinariaDao,
            IArqueoCajaSucursales arqueoCajaSucursalesDao,
            IMontoCajaChicaXSuc montoCajaChicaXSucDao,
            ICajaChica cajaChicaDao,
            ISucXMovCaja sucXMovCajaDao,
            IBitTareaRuti bitTareaRutiDao,
            IMovCaja movCajaDao,
            IFrecuencia frecuenciaDao,
            ICorte corteDao,
            ITarRuXCargo tarRuXCargoDao,
            IDetArqueoCajaSucursales detArqueoCajaSucursalesDao,
            IVale valeDao,
            IDetDocumentacion detDocumentacionDao,
            ICocheLlegadas cocheLlegadasDao,
            ICoche cocheDao,
            IDependientesJefe dependientesJefeDao,
            ITareaRutinariaConCargos tareaRutinariaConCargosDao,
            ICoches cochesDao,
            ICajaFuerte cajaFuerteDao,
            IArqueoRegistro arqueoRegistroDao,
            AccesoModuloHelper accesoModuloHelper,
            ICajaChicaFlujo cajaChicaFlujoDao,
            ICierreOperaciones cierreOperacionesDao,
            IVerificarCierreOperaciones verificarCierreOperacionesDao,
            IEmpleado empleadoDao,
            ITesBase tesBaseDao,
            JasperReportExport jasperReportExport,
            GeneracionAlInstante generacionAlInstante,
            IVistaDao vistaDao) {
        this.vistaDao = vistaDao;
        this.documentacionDao = documentacionDao;
        this.tareaRutinariaDao = tareaRutinariaDao;
        this.llegadaDao = llegadaDao;
        this.traspasoMovCajaDao = traspasoMovCajaDao;
        this.accionTareaRutinariaDao = accionTareaRutinariaDao;
        this.arqueoCajaSucursalesDao = arqueoCajaSucursalesDao;
        this.montoCajaChicaXSucDao = montoCajaChicaXSucDao;
        this.cajaChicaDao = cajaChicaDao;
        this.sucXMovCajaDao = sucXMovCajaDao;
        this.bitTareaRutiDao = bitTareaRutiDao;
        this.movCajaDao = movCajaDao;
        this.frecuenciaDao = frecuenciaDao;
        this.corteDao = corteDao;
        this.tarRuXCargoDao = tarRuXCargoDao;
        this.detArqueoCajaSucursalesDao = detArqueoCajaSucursalesDao;
        this.valeDao = valeDao;
        this.detDocumentacionDao = detDocumentacionDao;
        this.cocheLlegadasDao = cocheLlegadasDao;
        this.cocheDao = cocheDao;
        this.dependientesJefeDao = dependientesJefeDao;
        this.tareaRutinariaConCargosDao = tareaRutinariaConCargosDao;
        this.cochesDao = cochesDao;
        this.cajaFuerteDao = cajaFuerteDao;
        this.arqueoRegistroDao = arqueoRegistroDao;
        this.accesoModuloHelper = accesoModuloHelper;
        this.cajaChicaFlujoDao = cajaChicaFlujoDao;
        this.cierreOperacionesDao = cierreOperacionesDao;
        this.verificarCierreOperacionesDao = verificarCierreOperacionesDao;
        this.empleadoDao = empleadoDao;
        this.tesBaseDao = tesBaseDao;
        this.jasperReportExport = jasperReportExport;
        this.generacionAlInstante = generacionAlInstante;
    }

    // ==================== ENDPOINTS ESPECIALES (jefe → dependientes) ====================

    /**
     * Lista los cargos dependientes del cargo vigente del usuario autenticado
     * (jefe de área/gerente), para armar el selector de "a quién le asigno
     * esta tarea". codUsuario se resuelve del JWT, no del body. El SP valida
     * server-side que el cargo del caller tenga codNivel &lt;= nivelMaximoJefe;
     * si no está autorizado, responde 403 con el motivo.
     */
    @PostMapping("/listar-dependientes-jefe")
    public ResponseEntity<ApiResponse<?>> listarDependientesJefe(
            @RequestBody(required = false) DependientesJefeRequest req, Authentication auth) {

        DependientesJefeRequest r = req != null ? req : new DependientesJefeRequest();
        long codUsuario = DatosToken.codUsuarioDe(auth);

        // Defaults del DTO ("T"/"A") solo aplican si el campo viene AUSENTE
        // del JSON — un cliente que mande profundidad/alcanceSucursal en
        // null explícito pisa el default de Java al deserializar. Se
        // resuelve aquí para no depender de eso.
        String profundidad = r.getProfundidad() != null ? r.getProfundidad() : "T";
        String alcanceSucursal = r.getAlcanceSucursal() != null ? r.getAlcanceSucursal() : "A";

        ListadoConEstado<DependienteCargo> resultado = dependientesJefeDao.listarDependientes(
                codUsuario, profundidad, alcanceSucursal);

        if (!resultado.isAutorizado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(resultado.getErrormsg(), null, HttpStatus.FORBIDDEN.value()));
        }
        return procesarLista(resultado.getFilas(), "No se encontraron dependientes.");
    }

    /**
     * Crea una tarea rutinaria y la asigna a uno o más cargos en una sola
     * transacción, en modo jefe por defecto (valida server-side que cada
     * cargo destino esté en el subárbol del caller). codUsuario se resuelve
     * del JWT, no del body — evita que alguien registre a nombre de otro
     * usuario.
     */
    @PostMapping("/registrar-tarea-rutinaria-con-cargos")
    public ResponseEntity<ApiResponse<?>> registrarTareaRutinariaConCargos(
            @RequestBody RegistrarTareaRutinariaRequest req, Authentication auth) {

        req.setCodUsuario(DatosToken.codUsuarioDe(auth));
        // CRÍTICO (hallazgo de code-review): modoJefe llegaba como campo
        // libre del cliente — cualquier caller autenticado podía mandar
        // "modoJefe": false y saltarse por completo la validación de
        // subárbol/nivel del SP, asignando tareas a cualquier cargo de la
        // organización. Este endpoint es específicamente el de autoservicio
        // del jefe: se fuerza SIEMPRE a true, sin excepción. Un eventual
        // modo admin/RRHH sin restricción de subárbol debe ser un endpoint
        // aparte, con su propio gate de rol — no una bandera en este mismo
        // body.
        req.setModoJefe(true);
        return escrituraQueGenera(tareaRutinariaConCargosDao.registrar(req), "tarea nueva con cargos");
    }

    /**
     * Admin "Tareas Rutinarias por Cargo" (reemplaza dlgTarFunXCargo de
     * WizardEstOrg.java — solo el lado de Tareas Rutinarias; la pestaña
     * FUNCIONES del legacy es un subsistema aparte, tb_funcion/
     * tb_cargoXFuncion, fuera de este alcance). "Agregar Tarea Rutinaria"
     * desde el organigrama: mismo proc ACID que
     * {@link #registrarTareaRutinariaConCargos}, pero SIN la restricción de
     * subárbol — un admin/RRHH puede asignar a cualquier cargo de la
     * empresa, no solo a los suyos. Por eso va detrás del botón real
     * btnTareasRutXCargo (vista 12, Estructura Organizacional) — ROLE_ADM
     * pasa siempre por el fallback de exigirBoton; Marcelo puede otorgar el
     * botón a un ROLE_LIM puntual insertando en tb_usuarioBtn, sin
     * redeploy (2026-09-07, mismo criterio que cboFueRevisado/plCajaFuerte
     * en Verificar Cierre — rol grueso arriba, botón fino adentro).
     */
    @PostMapping("/admin/registrar-tarea-rutinaria-por-cargo")
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    public ResponseEntity<ApiResponse<?>> registrarTareaRutinariaPorCargoAdmin(
            @RequestBody RegistrarTareaRutinariaRequest req, Authentication auth) {
        accesoModuloHelper.exigirBoton(auth, 12, "btnTareasRutXCargo");
        req.setCodUsuario(DatosToken.codUsuarioDe(auth));
        req.setModoJefe(false);
        return escrituraQueGenera(tareaRutinariaConCargosDao.registrar(req), "tarea nueva con cargos");
    }

    /**
     * Lista las tareas rutinarias asignadas a un cargo, con los datos de
     * despliegue de cada tarea (descripción/frecuencia/fecha de partida) —
     * la lista principal del diálogo "Tareas Rutinarias por Cargo". Requiere
     * el botón btnTareasRutXCargo, mismo motivo que
     * {@link #registrarTareaRutinariaPorCargoAdmin}.
     */
    @PostMapping("/admin/obtener-tareas-rutinarias-por-cargo")
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    public ResponseEntity<ApiResponse<?>> obtenerTareasRutinariasPorCargo(
            @RequestBody TarRuXCargo mb, Authentication auth) {
        accesoModuloHelper.exigirBoton(auth, 12, "btnTareasRutXCargo");
        return procesarLista(
                tarRuXCargoDao.listarPorCargoConDetalle(mb.getCodCargo()),
                "No se encontraron tareas rutinarias asignadas a este cargo.");
    }

    // ==================== FLUJO ESPECIAL: COCHES (idATR=6) ====================
    // Reemplaza dlgCoches de WizardTareas.java. Corrige un bug real del legacy
    // (confirmado leyendo WizardTareas.java#registrarRevisionCoche): ahí la
    // tarea se cerraba al marcar CUALQUIER coche, no cuando se marcaban
    // todos — ver p_coches_marcarLlegada, que solo cierra cuando ya no queda
    // ningún llego pendiente para esa ocurrencia.

    /**
     * Lista los coches a revisar para una ocurrencia puntual (siembra una
     * fila por coche activo de la sucursal la primera vez que se pide, si
     * todavía no existe ninguna para ese idBitTarea).
     */
    @PostMapping("/coches/listar-del-dia")
    public ResponseEntity<ApiResponse<?>> listarCochesDelDia(
            @RequestBody CochesDelDiaRequest req, Authentication auth) {
        long codUsuario = DatosToken.codUsuarioDe(auth);
        return procesarLista(
                cochesDao.listarDelDia(req.getIdTarRuti(), req.getIdBitTarea(), codUsuario),
                "No se encontraron coches para esta ocurrencia.");
    }

    /**
     * Marca si un coche llegó o no. Cierra automáticamente la tarea rutinaria
     * completa cuando esta marca deja a todos los coches de la ocurrencia con
     * respuesta — el caller no necesita (ni puede) forzar el cierre aparte.
     */
    @PostMapping("/coches/marcar-llegada")
    public ResponseEntity<ApiResponse<?>> marcarLlegadaCoche(
            @RequestBody MarcarLlegadaCocheRequest req, Authentication auth) {
        long codUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(
                cochesDao.marcarLlegada(req.getIdCo(), req.getLlego(), req.getObs(), codUsuario));
    }

    // ==================== FLUJO ESPECIAL: CAJA FUERTE (idATR=4) ====================
    // Reemplaza dlgCajaFuerte de WizardTareas.java (el lado del submitter —
    // el panel plCajaFuerte de idATR=5, solo lectura + verificación del
    // supervisor, es un flujo aparte, no construido todavía).

    /**
     * Registra el lote de llegadas del día para caja fuerte y cierra la
     * tarea, en una sola transacción. audUsuario se resuelve del JWT.
     */
    @PostMapping("/caja-fuerte/registrar")
    public ResponseEntity<ApiResponse<?>> registrarCajaFuerte(
            @RequestBody RegistrarCajaFuerteRequest req, Authentication auth) {
        long codUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(cajaFuerteDao.registrar(req, codUsuario));
    }

    /**
     * Lo que YA se registró hoy en caja fuerte, en la sucursal de esta
     * ocurrencia (Marcelo, 2026-10-05: "que me aparezcan ya los registrados en
     * el día"). Antes la pantalla solo mostraba el formulario vacío, así que
     * después de guardar no quedaba rastro en pantalla de lo cargado.
     * <p>
     * Es la misma consulta del panel del supervisor
     * ({@code p_list_tac_Llegada} ACCION='B'), con otra puerta: aquel exige el
     * botón plCajaFuerte, que el chofer que carga las llegadas no tiene. Aquí
     * alcanza con que la ocurrencia sea suya, igual que para registrarlas.
     * <p>
     * Siempre su sucursal y siempre hoy: esta pantalla es la carga del día, no
     * un histórico — por eso no toma {@code fecha} ni el toggle de todas las
     * sucursales, aunque el SP los acepte.
     */
    @PostMapping("/caja-fuerte/del-dia")
    public ResponseEntity<ApiResponse<?>> llegadasDeCajaFuerteDelDia(
            @RequestBody ObtenerDeHoyRequest req, Authentication auth) {
        exigirOcurrenciaPropia(auth, req.getIdBitTarea());
        return procesarLista(
                llegadaDao.obtenerDelDia(req.getIdBitTarea(), false, null),
                "Todavía no hay llegadas registradas hoy.");
    }

    // ==================== FLUJO ESPECIAL: ARQUEO DE CAJA (idATR=2) ====================
    // Reemplaza dlgArqCaja de WizardTareas.java. Los catálogos de cortes y
    // documentación para armar el formulario ya se sirven con los endpoints
    // genéricos existentes (/obtener-corte, /obtener-documentacion) — no
    // hace falta un endpoint de listado aparte aquí.

    /**
     * Registra el arqueo (cabecera + detalle de cortes/documentación/vales)
     * y cierra la tarea, todo en una transacción. El total y la diferencia
     * se recalculan server-side — nunca se confía en lo que mande el
     * cliente. codEmpleadoEncargado se resuelve del JWT, no del body.
     */
    @PostMapping("/arqueo-caja/registrar")
    public ResponseEntity<ApiResponse<?>> registrarArqueoCaja(
            @RequestBody RegistrarArqueoRequest req, Authentication auth) {
        long codUsuario = DatosToken.codUsuarioDe(auth);
        Long codEmpleado = accesoModuloHelper.codEmpleadoDelToken(auth);
        // Falla cerrado (hallazgo de code-review, 2026-09-03): un login sin
        // codEmpleado asociado (cuenta de sistema, usuario mal aprovisionado)
        // no debe poder registrar un arqueo — antes caía a 0, un
        // codEmpleadoEncargado falso en un registro de conciliación de caja.
        if (codEmpleado == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>("Tu usuario no tiene un empleado asociado; no se puede registrar el arqueo.",
                            null, HttpStatus.BAD_REQUEST.value()));
        }
        return respuestaEscritura(arqueoRegistroDao.registrar(req, codEmpleado, codUsuario));
    }

    /** Subreportes que declara RptArqueoDeCaja.jrxml, sin extensión — ver JasperReportExport.exportPDFConSubreportes. */
    private static final String[] SUBREPORTES_ARQUEO_DE_CAJA = {
            "subRptMovCaja", "subRptArqueoDeCajaCorteYDocumentacion", "subRptVales"
    };

    /** RptCajaChica.jrxml no declara subreportes — el arreglo vacío lo pide igual JasperReportExport.exportPDFConSubreportes. */
    private static final String[] SIN_SUBREPORTES_CAJA_CHICA = new String[0];

    /**
     * PDF de un arqueo de caja ya registrado — reemplaza
     * {@code WizardTareas.cargarPdfArqueosDeCaja(idAC)}/
     * {@code cargarPdfArqueosDeCajaDesdePrincipal(idBitTar)} (RptArqueoDeCaja
     * del legacy, con sus 3 subreportes: movimiento de caja SAP, cortes +
     * documentación, y vales sin cerrar).
     * <p>
     * {@code idAC} es el id real del arqueo, no algo que se resuelva de la
     * sesión — lo devuelve {@code idGenerado} al llamar
     * {@link #registrarArqueoCaja} (mismo criterio que
     * "resolver contexto del registro, no de la sesión"). Sin
     * {@code @PreAuthorize}: mismo criterio que el resto de este flujo
     * (registrar/saldo-sap/tipo-cambio/anterior) — cualquier usuario
     * autenticado puede pedir el comprobante del arqueo que acaba de cerrar.
     * <p>
     * También acepta {@code idBitTarea} en lugar de {@code idAC}: "Mis tareas"
     * ofrece el PDF del arqueo ya hecho y conoce la tarea, no el arqueo
     * (Marcelo, 2026-10-05: "en arqueo de caja que pueda imprimir el pdf
     * solamente de que lo hizo"). Por ese camino la tarea tiene que ser de
     * quien pide.
     */
    @PostMapping("/arqueo-caja/reporte-pdf")
    public ResponseEntity<?> reporteArqueoDeCajaPdf(@RequestBody Map<String, Object> body,
                                                    Authentication auth) {
        int idAC;
        if (body.get("idAC") == null && body.get("idBitTarea") != null) {
            long idBitTarea = ((Number) body.get("idBitTarea")).longValue();
            exigirOcurrenciaPropia(auth, idBitTarea);
            ArqueoCajaSucursales arqueo = arqueoCajaSucursalesDao.obtenerPorOcurrencia(idBitTarea);
            if (arqueo == null) {
                throw new SpBusinessException("Esta tarea no tiene un arqueo registrado.");
            }
            idAC = (int) arqueo.getIdAC();
        } else {
            idAC = ((Number) body.get("idAC")).intValue();
        }
        Map<String, Object> p = new HashMap<>();
        p.put("idAC", idAC);
        // Montos como 40,000.00: coma en los miles y punto en los decimales
        // (Marcelo, 2026-10-05). Sin esto el PDF toma el idioma del servidor,
        // que es español (40.000,00). Los subreportes heredan el mismo.
        p.put(net.sf.jasperreports.engine.JRParameter.REPORT_LOCALE, java.util.Locale.US);
        byte[] bytes = jasperReportExport.exportPDFConSubreportes(
                "RptArqueoDeCaja", SUBREPORTES_ARQUEO_DE_CAJA, p);
        if (bytes == null || bytes.length == 0) {
            throw new SpBusinessException("El reporte no devolvió datos para el arqueo indicado.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(bytes.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    // ==================== FLUJO ESPECIAL: CAJA CHICA (idATR=7) ====================
    // Reemplaza dlgCajaChica de WizardTareas.java. El lote se resuelve solo
    // por sucursal (el más alto existente, sembrando el 1 con el saldo
    // inicial si no hay ninguno). "Cerrar lote y empezar uno nuevo" (abajo,
    // cerrarLoteCajaChica) es la operación de cierre de período — reemplaza
    // al legacy "Generar PDF" (WizardTareas.generarReporteYLoteCajaChica())
    // en su mitad de rotación de lote; la generación del PDF en sí queda
    // fuera de este endpoint, la está migrando un esfuerzo aparte.

    /** Lista las filas del lote vigente de la sucursal, sembrando la fila de saldo inicial si hace falta. */
    @PostMapping("/caja-chica/listar-del-lote")
    public ResponseEntity<ApiResponse<?>> listarCajaChicaDelLote(
            @RequestBody CajaChicaListarRequest req, Authentication auth) {
        long codUsuario = DatosToken.codUsuarioDe(auth);
        return procesarLista(
                cajaChicaFlujoDao.listarDelLote(req.getIdBitTarea(), codUsuario),
                "No se encontraron movimientos de caja chica.");
    }

    /** Registra un egreso, validando saldo suficiente antes de guardar (nunca en silencio). */
    @PostMapping("/caja-chica/registrar-egreso")
    public ResponseEntity<ApiResponse<?>> registrarEgresoCajaChica(
            @RequestBody RegistrarEgresoCajaChicaRequest req, Authentication auth) {
        long codUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(cajaChicaFlujoDao.registrarEgreso(req, codUsuario));
    }

    /** Marca la tarea de caja chica del día como completada. */
    @PostMapping("/caja-chica/finalizar")
    public ResponseEntity<ApiResponse<?>> finalizarCajaChica(
            @RequestBody FinalizarCajaChicaRequest req, Authentication auth) {
        long codUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(cajaChicaFlujoDao.finalizar(req.getIdBitTarea(), codUsuario));
    }

    /**
     * Cierra el lote vigente de la sucursal (del cargo actual del empleado
     * dueño de la ocurrencia) y abre el siguiente con su saldo inicial ya
     * sembrado desde tac_montoCajaChicaXSuc — el gap real que el legacy
     * cubría con "Generar PDF" ("esto reiniciara su caja chica"), sin la
     * parte de reporte (migrada aparte). Acción independiente de
     * {@link #finalizarCajaChica}: no cierra la tarea del día.
     */
    @PostMapping("/caja-chica/cerrar-lote")
    public ResponseEntity<ApiResponse<?>> cerrarLoteCajaChica(
            @RequestBody CerrarLoteCajaChicaRequest req, Authentication auth) {
        long codUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(cajaChicaFlujoDao.cerrarLote(req.getIdBitTarea(), codUsuario));
    }

    /** "Ver Cajas Chicas" del legacy: histórico de lotes de la sucursal (lote/desde/hasta/total egresos). */
    @PostMapping("/caja-chica/historial-lotes")
    public ResponseEntity<ApiResponse<?>> obtenerHistorialLotesCajaChica(@RequestBody CajaChicaListarRequest req) {
        return procesarLista(
                cajaChicaFlujoDao.obtenerHistorialLotes(req.getIdBitTarea()),
                "No hay lotes de caja chica anteriores para esta sucursal.");
    }

    /**
     * Búsqueda de empleados para el picker "Empleado Destino" de Caja Chica
     * — el legacy usa un `&lt;p:selectOneMenu filter="true"&gt;` con nombre+cargo,
     * no un id crudo tipeado a mano (confirmado leyendo Tareas.xhtml). Sin
     * el gate ROLE_ADM/ROLE_LIM del endpoint RRHH original: cualquier
     * usuario autenticado de este módulo puede buscar a quién le entregó
     * el dinero.
     */
    @PostMapping("/caja-chica/buscar-empleados")
    public ResponseEntity<ApiResponse<?>> buscarEmpleadosCajaChica(@RequestBody Empleado filtro) {
        String search = filtro.getSearch();
        List<Empleado> resultado = empleadoDao.obtenerLstEmpleados(search, 1, 0, 20, filtro.getCodEmpresa());
        return procesarLista(resultado, "No se encontraron empleados.");
    }

    /**
     * PDF "Caja Chica" (RptCajaChica.jrxml, sin subreportes) — reemplaza el
     * botón "Generar PDF" de la fila de "Ver Cajas Chicas" del legacy
     * ({@code WizardTareas.generarPDFCajaChica(lote)}, líneas 303-310: arma
     * {@code lote}/{@code codSucursal} y apunta a RptCajaChica). El reporte
     * sigue trayendo su propio {@code <queryString>} embebido
     * ({@code execute p_list_cajaChica @lote=..., @codSucursal=...,
     * @ACCION='B'} — el mismo proc legacy que ya usaba, sin cambios; se
     * confirmó vía sqlcmd de solo lectura que sigue existiendo y que sus
     * columnas coinciden 1 a 1 con los {@code <field>} del .jrxml).
     * <p>
     * {@code lote} y {@code codSucursal} vienen ECHOED desde la fila del
     * histórico que este mismo backend ya devolvió en
     * {@link #obtenerHistorialLotesCajaChica} — la sucursal se resuelve ahí
     * server-side a partir del cargo vigente del empleado dueño de la
     * ocurrencia; aquí no se vuelve a resolver, solo se hace el round-trip de
     * ese mismo valor (mismo criterio "contexto del registro, no de la
     * sesión" que ya usa {@link #reporteArqueoDeCajaPdf}). Sin
     * {@code @PreAuthorize}: mismo criterio que el resto de este flujo —
     * cualquier usuario autenticado puede generar el PDF de un lote de SU
     * sucursal.
     */
    @PostMapping("/caja-chica/reporte-pdf")
    public ResponseEntity<?> reporteCajaChicaPdf(@RequestBody Map<String, Object> body) {
        int lote = ((Number) body.get("lote")).intValue();
        String codSucursal = String.valueOf(((Number) body.get("codSucursal")).longValue());
        Map<String, Object> p = new HashMap<>();
        p.put("lote", lote);
        p.put("codSucursal", codSucursal);
        byte[] bytes = jasperReportExport.exportPDFConSubreportes(
                "RptCajaChica", SIN_SUBREPORTES_CAJA_CHICA, p);
        if (bytes == null || bytes.length == 0) {
            throw new SpBusinessException("El reporte no devolvió datos para el lote indicado.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(bytes.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /** RptTareaXDia.jrxml no declara subreportes. */
    private static final String[] SIN_SUBREPORTES_TAREA_X_DIA = new String[0];

    /**
     * Historial de tareas rutinarias de un empleado — reemplaza
     * {@code WizardTareas.prepararTareaRutinariaXEmpleado(mb)} (RptTareaXDia
     * del legacy). Consulta {@code p_list_bitTareaRuti @ACCION='A'}: cada
     * ocurrencia con su frecuencia, si se realizó o no, la observación y las
     * fechas.
     * <p>
     * A quién se le puede pedir el reporte: por defecto a uno mismo, resuelto
     * del JWT y nunca del body. Pedir el de OTRO empleado exige el botón real
     * {@code btnEmpAll} de la vista 78 ("Reporte de todos los emp mod tareas"),
     * el mismo que ya gatea en el cliente la opción "Ver de todos los
     * empleados" — esconderla en Flutter no alcanza, el historial de tareas de
     * una persona es dato de RR.HH.
     * <p>
     * Sin rango de fechas, igual que el legacy: el proc devuelve el historial
     * completo del empleado. Con {@code tac_bitTareaRuti} en 70k+ filas
     * abiertas eso puede dar un PDF largo para alguien con muchos años de
     * antigüedad — si molesta, el arreglo es agregar @fechaDesde/@fechaHasta a
     * la ACCION 'A' existente (no un proc nuevo), pero se deja en paridad con
     * el legacy hasta que alguien lo pida.
     */
    @PostMapping("/mis-tareas/reporte-pdf")
    public ResponseEntity<?> reporteTareasRutinariasPdf(
            @RequestBody(required = false) Map<String, Object> body, Authentication auth) {
        Long miCodEmpleado = accesoModuloHelper.codEmpleadoDelToken(auth);
        Object pedido = (body == null) ? null : body.get("codEmpleado");

        long codEmpleado;
        if (pedido == null) {
            if (miCodEmpleado == null) {
                throw new SpBusinessException(
                        "Tu usuario no tiene un empleado asociado; no se puede generar el reporte.");
            }
            codEmpleado = miCodEmpleado;
        } else {
            codEmpleado = ((Number) pedido).longValue();
            if (miCodEmpleado == null || codEmpleado != miCodEmpleado.longValue()) {
                accesoModuloHelper.exigirBoton(auth, 78, "btnEmpAll");
            }
        }

        Map<String, Object> p = new HashMap<>();
        // El .jrxml declara codEmpleado como java.lang.Integer: pasar un Long
        // hace que Jasper lo rechace al llenar el reporte.
        p.put("codEmpleado", (int) codEmpleado);
        byte[] bytes = jasperReportExport.exportPDFConSubreportes(
                "RptTareaXDia", SIN_SUBREPORTES_TAREA_X_DIA, p);
        if (bytes == null || bytes.length == 0) {
            throw new SpBusinessException("El empleado no tiene tareas rutinarias registradas.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(bytes.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    // ==================== FLUJO ESPECIAL: CIERRE DE OPERACIONES (idATR 3 y 5) ====================
    // Reemplaza dlgRevArqueo de WizardTareas.java: la revisión de un día de lo
    // que hicieron los demás — arqueos, traspasos de Caja AXA, caja fuerte,
    // cheques y las tareas que genera el Job. El sistema anterior usaba el
    // mismo diálogo para las dos tareas de la cadena y solo cambiaba el botón
    // de abajo:
    //
    //   - Cierre de Operaciones (2) y "Verficar Arqueo de Caja" (3), idATR 3:
    //     cierran su propia ocurrencia (confirmarCierreOperaciones) y marcan
    //     los traspasos (archivo SQL 60).
    //   - Verificar Cierre de Operaciones (39), idATR 5: cierra todas las
    //     ocurrencias del día (confirmarVerificacion).
    //
    // **La tarea es el permiso** (decisión de Marcelo, 2026-09-11): quien tiene
    // la ocurrencia del día de una tarea de revisión ve y marca las cinco
    // secciones. Los botones de la vista 78 (plArqCaja, plMovCaja,
    // plCajaFuerte, plCheques, cboFueRevisado, chkSuc) siguen siendo el gate
    // de los llamados que NO traen ocurrencia — la app vieja, por ejemplo —,
    // porque el ACL real de esos paneles nunca se mantuvo: de los 8 usuarios
    // con plArqCaja, solo 2 tienen plMovCaja.
    //
    // 2026-09-11: los traspasos vuelven a marcarse desde aquí. El 2026-09-10
    // habían quedado solo en la tarea 295, y Marcelo preguntó con un traspaso
    // "Sin revisar" en pantalla: "¿por qué no me aparece para ponerlo como
    // verificado?".

    /** Las tareas que cierran su propia ocurrencia desde la revisión (idATR 3). */
    private static final Set<Long> TAREAS_CIERRE = new HashSet<>(Arrays.asList(2L, 3L));

    /** La tarea del supervisor, que cierra el día (idATR 5). */
    private static final Set<Long> TAREAS_VERIFICAR_CIERRE = Collections.singleton(39L);

    /** Todas las que abren la revisión. */
    private static final Set<Long> TAREAS_REVISION_CIERRE = new HashSet<>(Arrays.asList(2L, 3L, 39L));

    /** "Verificar traspaso Caja AXA contra movimiento de caja" (idATR 12), la del cajero. */
    private static final long TAREA_CAJA_AXA = 295L;

    /** Cierra la ocurrencia de Cierre de Operaciones (o de Verficar Arqueo de Caja) de quien llama. */
    @PostMapping("/cierre-operaciones/confirmar")
    public ResponseEntity<ApiResponse<?>> confirmarCierreOperaciones(
            @RequestBody ConfirmarCierreOperacionesRequest req, Authentication auth) {
        // Hasta el 2026-09-11 no se miraba de quién era: cualquier usuario podía
        // cerrar la ocurrencia de otro mandando su id.
        exigirOcurrenciaDeCierre(auth, req.getIdBitTarea(), TAREAS_CIERRE);
        long codUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(cierreOperacionesDao.confirmarTraspasos(req, codUsuario));
    }

    /**
     * Los cheques del día: el panel "Cheques" (plCheques) del sistema anterior.
     *
     * <p>Mismo SP y misma acción que {@code ValeDao.lstChequeCO}:
     * {@code p_SAP_Rpt_ImpChequesPR 'A'}, que cruza los cheques registrados en
     * Bosque con los cobrados en SAP. Pregunta a SAP en vivo y falla si no
     * contesta, a propósito: una lista vacía diría "no hubo cheques".
     */
    @PostMapping("/cierre-operaciones/cheques")
    public ResponseEntity<ApiResponse<?>> chequesDelCierre(
            @RequestBody ObtenerDeHoyRequest req, Authentication auth) {
        exigirRevisionOBoton(auth, req.getIdBitTarea(), "plCheques");
        if (req.getFecha() == null) {
            throw new SpBusinessException("Indica el día de los cheques.");
        }
        return procesarLista(cierreOperacionesDao.cheques(req.getFecha()),
                "No hubo cheques en la fecha indicada.");
    }

    /**
     * Las tareas que generó el Job ese día en la sucursal de la ocurrencia: lo
     * que quien cierra revisa que los demás hayan hecho.
     *
     * <p>La sucursal la resuelve el SP desde la ocurrencia
     * ({@code p_list_tac_BitTareaRuti 'R'}, archivo SQL 60), nunca el cliente.
     * Todas las sucursales exige chkSuc: son nombres y cumplimiento de toda la
     * empresa, no filas de caja.
     */
    @PostMapping("/cierre-operaciones/tareas-del-dia")
    public ResponseEntity<ApiResponse<?>> tareasDelDiaDelCierre(
            @RequestBody ObtenerDeHoyRequest req, Authentication auth) {
        // Toda la empresa no pide chkSuc: quien tiene la tarea de revisión la
        // revisa entera, y el alcance ya está acotado a esa ocurrencia.
        BitTareaRuti ocurrencia = exigirOcurrenciaDeCierre(auth, req.getIdBitTarea(), TAREAS_REVISION_CIERRE);
        Date fecha = req.getFecha() != null ? req.getFecha() : ocurrencia.getFechaPresentacion();
        return procesarLista(
                bitTareaRutiDao.tareasDelCierre(req.getIdBitTarea(), fecha, req.isTodasSucursales()),
                "No hubo tareas rutinarias ese día en la sucursal.");
    }

    // ==================== REVISIÓN DE CIERRE: PANELES Y CIERRE DEL SUPERVISOR ====================
    // Las rutas siguen bajo /verificar-cierre/ porque nacieron para la tarea 39
    // y la app desplegada las llama así. Desde el 2026-09-11 las usa también
    // Cierre de Operaciones (ver el bloque de arriba). Los gates de escritura
    // son el ACL real de botones de la vista 78: cboFueRevisado (VoBo de
    // arqueo) y plCajaFuerte (panel de caja fuerte).

    /**
     * Arqueos de un día, enriquecidos (empleado/sucursal/tarea) — el panel
     * "plArqCaja" del legacy. Sin {@code fecha}, hoy (archivo SQL 60).
     * {@code todasSucursales}=true solo tiene efecto real si el caller tiene
     * el botón chkSuc (Flutter gatea el toggle con {@code PermissionWidget};
     * aquí no se revalida el botón porque el peor caso es ver MÁS filas de
     * solo-lectura, no escribir nada fuera de alcance).
     */
    @PostMapping("/verificar-cierre/arqueos-de-hoy")
    public ResponseEntity<ApiResponse<?>> obtenerArqueosDeHoy(
            @RequestBody ObtenerDeHoyRequest req, Authentication auth) {
        exigirRevisionOBoton(auth, req.getIdBitTarea(), "plArqCaja");
        return procesarLista(
                arqueoCajaSucursalesDao.obtenerDelDia(req.getIdBitTarea(), req.isTodasSucursales(), req.getFecha()),
                "No hay arqueos de caja registrados ese día.");
    }

    /** Llegadas de caja fuerte de un día (sin fecha, hoy), enriquecidas (sucursal) — el panel "plCajaFuerte" del legacy. */
    @PostMapping("/verificar-cierre/llegadas-de-hoy")
    public ResponseEntity<ApiResponse<?>> obtenerLlegadasDeHoy(
            @RequestBody ObtenerDeHoyRequest req, Authentication auth) {
        exigirRevisionOBoton(auth, req.getIdBitTarea(), "plCajaFuerte");
        return procesarLista(
                llegadaDao.obtenerDelDia(req.getIdBitTarea(), req.isTodasSucursales(), req.getFecha()),
                "No hay llegadas de caja fuerte registradas ese día.");
    }

    /**
     * Marca un arqueo de caja del día como revisado (VoBo), o le quita la marca
     * con fueRevisado = 0 — "a veces se equivocan y tiene que volver a como
     * estaba" (Marcelo, 2026-10-05). Con la ocurrencia de la revisión, o con el
     * botón cboFueRevisado: el mismo permiso para las dos cosas.
     */
    @PostMapping("/verificar-cierre/marcar-arqueo-revisado")
    public ResponseEntity<ApiResponse<?>> marcarArqueoRevisado(
            @RequestBody MarcarArqueoRevisadoRequest req, Authentication auth) {
        exigirRevisionOBoton(auth, req.getIdBitTarea(), "cboFueRevisado");
        long codUsuario = DatosToken.codUsuarioDe(auth);
        if (req.getFueRevisado() != 0 && req.getFueRevisado() != 1) {
            throw new SpBusinessException("fueRevisado tiene que ser 1 (revisado) o 0 (quitar la marca).");
        }
        return respuestaEscritura(verificarCierreOperacionesDao.marcarArqueoRevisado(
                req.getIdAC(), req.getFueRevisado(), codUsuario));
    }

    /**
     * Marca una llegada de caja fuerte del día como verificada, o le quita la
     * marca con fueVerificado = 0 (archivo SQL 71: al quitarla no queda
     * verificador). Con la ocurrencia de la revisión, o con el botón
     * plCajaFuerte.
     */
    @PostMapping("/verificar-cierre/marcar-llegada-verificada")
    public ResponseEntity<ApiResponse<?>> marcarLlegadaVerificada(
            @RequestBody MarcarLlegadaVerificadaRequest req, Authentication auth) {
        exigirRevisionOBoton(auth, req.getIdBitTarea(), "plCajaFuerte");
        if (req.getFueVerificado() != 0 && req.getFueVerificado() != 1) {
            throw new SpBusinessException("fueVerificado tiene que ser 1 (verificada) o 0 (quitar la marca).");
        }
        long codUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(
                verificarCierreOperacionesDao.marcarLlegadaVerificada(req.getIdRp(), req.getFueVerificado(), codUsuario));
    }

    /**
     * Cierra la verificación del día — cierra TODAS las ocurrencias de esta
     * tarea de verificación para la fecha, no solo la que se está mirando
     * (replica ACCION='E' del legacy, resolviendo idTarRuti dinámicamente
     * en vez del 39 hardcodeado que usa Bosque v2).
     */
    @PostMapping("/verificar-cierre/confirmar")
    public ResponseEntity<ApiResponse<?>> confirmarVerificacion(
            @RequestBody ConfirmarVerificacionRequest req, Authentication auth) {
        // Con una ocurrencia propia de la 39 (o btnEmpAll): cierra las de todos
        // para ese día, así que no puede llegar con el id de cualquier tarea.
        exigirOcurrenciaDeCierre(auth, req.getIdBitTarea(), TAREAS_VERIFICAR_CIERRE);
        long codUsuario = DatosToken.codUsuarioDe(auth);
        return respuestaEscritura(verificarCierreOperacionesDao.confirmarVerificacion(req.getIdBitTarea(), codUsuario));
    }

    /** Subreportes que declara RptCierreOperaciones.jrxml, sin extensión — ver JasperReportExport.exportPDFConSubreportes. */
    private static final String[] SUBREPORTES_CIERRE_OPERACIONES = {
            "subRptArqueosDeCaja", "subRptValesCO", "subRptCajaFuerte",
            "subRptTraspasoMovCaja", "subRptChequesCO", "subRptCocheCO"
    };

    /**
     * PDF consolidado "Cierre de Operaciones" — reemplaza el botón de
     * cabecera "Cargar Cierre de Operaciones PDF" de dlgRevArqueo
     * (WizardTareas.preparaRptCierreOperaciones, visible solo para
     * idATR=5), la umbrella que junta los 6 flujos del día en un solo PDF
     * (arqueos, vales, caja fuerte, traspasos, cheques SAP y coches).
     * <p>
     * Mismo cuerpo que {@link #obtenerArqueosDeHoy}/{@link #obtenerLlegadasDeHoy}
     * ({@code ObtenerDeHoyRequest}: idBitTarea + todasSucursales) — el
     * parámetro {@code codSucursal} del reporte se resuelve de idBitTarea
     * server-side ({@link IVerificarCierreOperaciones#resolverCodSucursal}),
     * nunca de la sesión de quien llama (0 = todas, si todasSucursales=true
     * o si no se puede resolver un cargo vigente). {@code fecha} es el día
     * que se está revisando (el selector de WizardTareas.fechaArqueo); sin
     * fecha, hoy.
     * <p>
     * Sin {@code @PreAuthorize} ni {@code exigirBoton}: mismo criterio que
     * {@link #obtenerArqueosDeHoy}/{@link #obtenerLlegadasDeHoy} — es una
     * lectura, no una escritura, así que no repite el gate de botón de
     * {@link #marcarArqueoRevisado}/{@link #marcarLlegadaVerificada}.
     * <p>
     * <b>Heredado del legacy:</b> {@code p_list_bitTareaRuti} (@ACCION='G',
     * la consulta del reporte principal) busca "quién cerró primero" con
     * {@code idTarRuti = 39} fijo ("tarea rutinaria estática", comentario del
     * propio SP). El subreporte de cheques ({@code subRptChequesCO} →
     * {@code p_SAP_Rpt_ImpChequesPR}) pregunta a SAP en vivo por el servidor
     * enlazado SRV_2022 (archivo SQL 48) con {@code @audUsuario=34} fijo: si
     * SAP no contesta, falla el PDF completo.
     */
    @PostMapping("/verificar-cierre/reporte-cierre-operaciones-pdf")
    public ResponseEntity<?> reporteCierreOperacionesPdf(@RequestBody ObtenerDeHoyRequest req) {
        int codSucursal = verificarCierreOperacionesDao.resolverCodSucursal(req.getIdBitTarea(), req.isTodasSucursales());
        Map<String, Object> p = new HashMap<>();
        p.put("fecha", req.getFecha() != null ? req.getFecha() : new java.util.Date());
        p.put("codSucursal", codSucursal);
        byte[] bytes = jasperReportExport.exportPDFConSubreportes(
                "RptCierreOperaciones", SUBREPORTES_CIERRE_OPERACIONES, p);
        if (bytes == null || bytes.length == 0) {
            throw new SpBusinessException("El reporte no devolvió datos para la fecha indicada.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(bytes.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /** RptCajaFuerteCO no declara subreportes. */
    private static final String[] SIN_SUBREPORTES_CAJA_FUERTE = new String[0];

    /**
     * PDF "Caja Fuerte" (RptCajaFuerteCO) — reemplaza el commandLink
     * "DESCARGAR PDF" del panel plCajaFuerte dentro de dlgRevArqueo
     * (WizardTareas.cargarDatosRevisarArqueo/cargarListArqueoDeCaja). El
     * reporte llama directamente al proc LEGACY {@code p_list_llegada}
     * (@ACCION='B', SQL embebido en RptCajaFuerteCO.jrxml) — sin
     * subreportes (a diferencia de {@link #reporteCierreOperacionesPdf}).
     * Mismo cuerpo que {@link #reporteCierreOperacionesPdf}: {@code fecha}
     * el día que se revisa (sin fecha, hoy) y {@code codSucursal} resuelto de
     * idBitTarea via {@link IVerificarCierreOperaciones#resolverCodSucursal}
     * (0 = todas, nunca de la sesión de quien llama). A diferencia de la
     * umbrella de Cierre de Operaciones, este endpoint SÍ requiere el
     * botón plCajaFuerte (igual que {@link #marcarLlegadaVerificada}): es
     * el mismo panel del legacy, no una lectura suelta.
     */
    @PostMapping("/verificar-cierre/reporte-caja-fuerte-pdf")
    public ResponseEntity<?> reporteCajaFuerteVerificarCierre(
            @RequestBody ObtenerDeHoyRequest req, Authentication auth) {
        exigirRevisionOBoton(auth, req.getIdBitTarea(), "plCajaFuerte");
        int codSucursal = verificarCierreOperacionesDao.resolverCodSucursal(req.getIdBitTarea(), req.isTodasSucursales());
        Map<String, Object> p = new HashMap<>();
        p.put("fecha", req.getFecha() != null ? req.getFecha() : new java.util.Date());
        p.put("codSucursal", codSucursal);
        byte[] bytes = jasperReportExport.exportPDFConSubreportes(
                "RptCajaFuerteCO", SIN_SUBREPORTES_CAJA_FUERTE, p);
        if (bytes == null || bytes.length == 0) {
            throw new SpBusinessException("El reporte no devolvió datos para los filtros indicados.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(bytes.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * Mismo reporte de Caja Fuerte (RptCajaFuerteCO), pero para el CHOFER que
     * acaba de registrar su kardex — no para el supervisor.
     * <p>
     * En el legacy el PDF era el paso con el que el movimiento entraba a
     * archivo, y hoy Caja Fuerte solo lo ofrecía desde el panel del supervisor
     * (plCajaFuerte), que un chofer no tiene (Marcelo, 2026-09-07: "mismo
     * tratamiento para la caja fuerte que hacen algunos choferes"). Este
     * endpoint es el mismo cuerpo sin ese botón, pero NO queda abierto: exige
     * ser el dueño de la ocurrencia, o admin.
     * <p>
     * Se comprueba contra el REGISTRO ({@code tac_bitTareaRuti.codEmpleado} de
     * ese {@code idBitTarea}) y no contra la sucursal de la sesión — mismo
     * criterio que {@link #registrarBitTareaRuti}: sin eso, cualquiera podría
     * pedir el kardex de caja fuerte de otra sucursal mandando un idBitTarea
     * ajeno. La sucursal del reporte también sale del registro, vía
     * {@code resolverCodSucursal}.
     */
    @PostMapping("/caja-fuerte/reporte-pdf")
    public ResponseEntity<?> reporteCajaFuertePropio(
            @RequestBody ObtenerDeHoyRequest req, Authentication auth) {
        BitTareaRuti ocurrencia = bitTareaRutiDao.obtenerPorId(req.getIdBitTarea());
        if (ocurrencia == null) {
            throw new SpBusinessException("No existe la bitácora indicada.");
        }
        if (!accesoModuloHelper.esAdmin(auth)) {
            Long miCodEmpleado = accesoModuloHelper.codEmpleadoDelToken(auth);
            if (miCodEmpleado == null || !miCodEmpleado.equals(ocurrencia.getCodEmpleado())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new ApiResponse<>("Esta ocurrencia no está asignada a tu usuario.",
                                null, HttpStatus.FORBIDDEN.value()));
            }
        }
        // false: nunca "todas las sucursales" desde aquí — eso es del panel del
        // supervisor, que tiene su propio endpoint y su propio botón.
        int codSucursal = verificarCierreOperacionesDao.resolverCodSucursal(req.getIdBitTarea(), false);
        Map<String, Object> p = new HashMap<>();
        p.put("fecha", new java.util.Date());
        p.put("codSucursal", codSucursal);
        byte[] bytes = jasperReportExport.exportPDFConSubreportes(
                "RptCajaFuerteCO", SIN_SUBREPORTES_CAJA_FUERTE, p);
        if (bytes == null || bytes.length == 0) {
            throw new SpBusinessException("No hay movimientos de caja fuerte de hoy para esta sucursal.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(bytes.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    // ==================== TRASPASO DE TAREAS POR CAMBIO DE CARGO ====================
    // El generador solo mira el cargo MÁS RECIENTE del empleado
    // (WHERE ec.fechaInicio = MAX(fechaInicio)), así que el día que RR.HH.
    // carga un cargo nuevo la persona deja de recibir las tareas del anterior
    // y nadie se entera. Estos dos endpoints hacen visible esa pérdida y
    // dejan decidirla — ver el archivo SQL 39.
    //
    // Mismo botón que el resto del ABM de tareas por cargo
    // (btnTareasRutXCargo, vista 12): quien puede asignar tareas a un cargo es
    // exactamente quien tiene que poder traspasarlas.

    /**
     * Traspasos pendientes: empleados con cambio de cargo reciente y, por cada
     * uno, las tareas de su cargo anterior marcando cuáles el cargo nuevo ya
     * tiene ({@code yaEstaEnCargoNuevo}) y a cuánta gente le caerían si se
     * copian ({@code personasEnCargoNuevo}).
     * <p>
     * {@code desde} opcional (ISO yyyy-MM-dd); sin él, el proc mira los
     * últimos 90 días.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/admin/traspasos-pendientes")
    public ResponseEntity<ApiResponse<?>> traspasosPendientes(
            @RequestBody(required = false) Map<String, Object> body, Authentication auth) {
        accesoModuloHelper.exigirBoton(auth, 12, "btnTareasRutXCargo");
        java.time.LocalDate desde = null;
        Object crudo = (body == null) ? null : body.get("desde");
        // .trim().isEmpty() y no .isBlank(): el proyecto compila contra Java 8.
        if (crudo != null && !String.valueOf(crudo).trim().isEmpty()) {
            desde = java.time.LocalDate.parse(String.valueOf(crudo));
        }
        return procesarLista(
                tarRuXCargoDao.listarTraspasosPendientes(desde),
                "No hay traspasos pendientes en el período indicado.");
    }

    /**
     * Copia UNA tarea del cargo anterior al cargo nuevo.
     * <p>
     * El proc es idempotente y se niega a copiar a un cargo sin gente activa
     * (error 24) — la guarda que pidió Marcelo: "que el Job no genere tareas a
     * un cargo que no tiene ninguna persona". El {@code audUsuario} sale del
     * token, nunca del body: es quien queda registrado como responsable de la
     * decisión.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/admin/traspasar-tarea")
    public ResponseEntity<ApiResponse<?>> traspasarTarea(
            @RequestBody Map<String, Object> body, Authentication auth) {
        accesoModuloHelper.exigirBoton(auth, 12, "btnTareasRutXCargo");
        long idTarRuti = ((Number) body.get("idTarRuti")).longValue();
        long codCargoDestino = ((Number) body.get("codCargoDestino")).longValue();

        // Revalidación contra la lista viva antes de escribir.
        //
        // La pantalla se abre, alguien se va a almorzar, y mientras tanto otro
        // operador le cambia el cargo a esa persona otra vez. Al volver y tocar
        // "Copiar", el par (tarea, cargo) que la pantalla mostraba ya no es el
        // vigente y la tarea terminaría en un cargo intermedio que la persona
        // ya no ocupa — y ahí queda, para todos los que ocupen ese cargo.
        //
        // Va aquí y no en el proc a propósito: agregarle un @codEmpleado a
        // p_abm_tac_TarRuXCargo rompería registrar(), que usa
        // SpHelper.ejecutarAbm — el binding por metadatos exige un valor para
        // CADA parámetro declarado, que es exactamente lo que tiró abajo
        // p_abm_tac_BitTareaRuti cuando el archivo 27 le agregó parámetros.
        boolean sigueVigente = tarRuXCargoDao.listarTraspasosPendientes(null).stream()
                .anyMatch(f -> {
                    Number tarea = (Number) f.get("idTarRuti");
                    Number cargo = (Number) f.get("codCargoNuevo");
                    Number ya = (Number) f.get("yaEstaEnCargoNuevo");
                    return tarea != null && cargo != null
                            && tarea.longValue() == idTarRuti
                            && cargo.longValue() == codCargoDestino
                            && (ya == null || ya.intValue() == 0);
                });

        if (!sigueVigente) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(
                    "Ese traspaso ya no está vigente (el cargo cambió de nuevo, o la tarea ya fue copiada). Actualizá la pantalla.",
                    null, HttpStatus.CONFLICT.value()));
        }

        return escrituraQueGenera(tarRuXCargoDao.traspasarTarea(
                idTarRuti, codCargoDestino, DatosToken.codUsuarioDe(auth)), "traspaso de tarea a otro cargo");
    }

    // ==================== SUBMÓDULOS A REQUERIMIENTO (vista 87) ====================

    /**
     * La pantalla de cada submódulo a requerimiento ({@code tb_vista.direccion},
     * creadas por el archivo SQL 44 bajo la 87). Es el único permiso: habilitar
     * la vista "Caja Chica" desde "Permisos del usuario" es lo que pone el ítem
     * en el menú y lo que deja abrirla. Es, además, la lista blanca: un
     * idTarRuti que no esté aquí se rechaza sin llegar al SP.
     *
     * <p>Por dirección y no por codVista: {@code tb_vista.codVista} es IDENTITY
     * y cambia de una base a otra.
     *
     * <p>Hasta el 2026-10-05 se exigía además un botón de la 87
     * ({@code btnModCajaChica} y compañía), un segundo permiso que la pantalla
     * de Permisos no muestra junto a la vista. Marcelo, con un usuario al que le
     * había dado la vista: "le di permisos pero no funciona" — el menú se la
     * mostraba y el servidor la rechazaba por el botón. Y después: "en vez de
     * botón no sería mejor una vista y listo". Los botones los borra el archivo
     * SQL 76, que antes pasa a la vista a quien los tuviera.
     *
     * <p>Cierre de Operaciones (tarea 2) no está: salió del menú con el archivo
     * SQL 63 y se abre con la ocurrencia de "Verificar Cierre de Operaciones".
     */
    private static final Map<Long, String> PANTALLA_POR_FLUJO;
    static {
        Map<Long, String> m = new HashMap<>();
        m.put(40L, "tacTareas/CajaFuerte");
        m.put(41L, "tacTareas/Coches");
        m.put(42L, "tacTareas/CajaChica");
        PANTALLA_POR_FLUJO = Collections.unmodifiableMap(m);
    }

    /**
     * Si quien llama tiene la pantalla en su menú, con la misma regla que usa
     * {@code AccesoPantallaPrecios}: {@code p_list_VistaUsuario 'M'}. Falla
     * cerrado. El administrador pasa siempre, igual que en {@code exigirBoton}.
     */
    private boolean tienePantalla(Authentication auth, String direccion) {
        if (direccion == null) return false;
        if (accesoModuloHelper.esAdmin(auth)) return true;
        int codUsuario = DatosToken.codUsuarioDe(auth);
        if (codUsuario <= 0) return false;
        List<Vista> rutas = vistaDao.obtainRoutes(codUsuario);
        boolean tiene = rutas != null && rutas.stream()
                .anyMatch(v -> v.getDireccion() != null && direccion.equalsIgnoreCase(v.getDireccion().trim()));
        if (!tiene) {
            // Mismo aviso que deja exigirBoton: es lo que hay que buscar en el log
            // cuando alguien dice "le di permisos y no entra".
            log.warn("ACL: codUsuario={} sin la pantalla '{}' en su menu; se niega el acceso",
                    codUsuario, direccion);
        }
        return tiene;
    }

    /**
     * Abre (o recupera) la ocurrencia de hoy de un flujo a requerimiento y
     * devuelve su {@code idBitTarea} para que la pantalla trabaje con él.
     *
     * <p>Caja Fuerte, Coches, Caja Chica y Cierre de Operaciones dejaron de ser
     * tareas rutinarias: el Job ya no las genera todas las noches para todo el
     * mundo. Ahora son submódulos con entrada propia en el menú, y la ocurrencia
     * nace cuando alguien entra a hacer el trabajo. Todo lo que cuelga de ella
     * —{@code tac_llegada}, {@code tac_cocheLlegadas}, {@code tac_cajaChica}—
     * sigue funcionando igual, porque esas tablas usan el idBitTarea solamente
     * para saber de quién es el trabajo.
     *
     * <p><b>El empleado sale del token, nunca del body.</b> Es la ocurrencia de
     * quien está entrando: aceptar un codEmpleado del cliente dejaría abrir —y
     * después completar— el flujo en nombre de otra persona.
     *
     * <p>Es idempotente: entrar dos veces el mismo día devuelve la misma
     * ocurrencia. Y no sirve para adelantar las tareas que sí genera el Job — el
     * SP las rechaza con error 31.
     */
    @PostMapping("/abrir-flujo")
    public ResponseEntity<ApiResponse<?>> abrirFlujoARequerimiento(
            @RequestBody Map<String, Object> body, Authentication auth) {

        Number idTarRutiRaw = body == null ? null : (Number) body.get("idTarRuti");
        if (idTarRutiRaw == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "Falta indicar qué flujo abrir.", null, HttpStatus.BAD_REQUEST.value()));
        }
        long idTarRuti = idTarRutiRaw.longValue();

        String pantalla = PANTALLA_POR_FLUJO.get(idTarRuti);
        if (pantalla == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "Ese flujo no se abre a requerimiento.", null, HttpStatus.BAD_REQUEST.value()));
        }
        if (!tienePantalla(auth, pantalla)) {
            throw new AccessDeniedException("No tiene habilitada la pantalla '" + pantalla + "'.");
        }

        Long codEmpleado = accesoModuloHelper.codEmpleadoDelToken(auth);
        if (codEmpleado == null) {
            // Un usuario sin empleado asociado no puede tener una ocurrencia: la
            // bitácora es por persona. Es un dato mal cargado, no un permiso.
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "Tu usuario no tiene un empleado asociado, así que no se puede abrir el flujo.",
                    null, HttpStatus.BAD_REQUEST.value()));
        }

        return respuestaEscritura(bitTareaRutiDao.abrirFlujoARequerimiento(
                idTarRuti, codEmpleado, DatosToken.codUsuarioDe(auth)));
    }

    // ==================== CATÁLOGOS Y ENTIDADES ESTÁNDAR ====================
    // Cada entidad sigue el mismo patrón:
    //   - Registrar: idX == 0 → INSERT ("I"), idX > 0 → UPDATE ("U")
    //   - Eliminar: siempre DELETE ("D")
    //   - Obtener: body null/vacío → todas; body con filtros → filtradas
    //     (ver SpHelper.ejecutarListado: model==null usa un Map vacío, sin
    //     el riesgo de que un primitivo en 0 viaje como filtro real)
    //
    // Estos endpoints NO llevan @Transactional porque cada operación es
    // atómica (un solo SP, con su propia transacción interna) y el manejador
    // global de excepciones ya cubre los errores.

    /** Registra o actualiza una documentación. */
    @PostMapping("/registrar-documentacion")
    public ResponseEntity<ApiResponse<?>> registrarDocumentacion(@RequestBody Documentacion mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(documentacionDao.registrar(mb, mb.getIdDoc() == 0 ? "I" : "U"));
    }

    /** Elimina una documentación por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-documentacion")
    public ResponseEntity<ApiResponse<?>> eliminarDocumentacion(@RequestBody Documentacion mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(documentacionDao.registrar(mb, "D"));
    }

    /** Obtiene documentaciones. Body null/vacío → retorna todas. */
    @PostMapping("/obtener-documentacion")
    public ResponseEntity<ApiResponse<?>> obtenerDocumentacion(@RequestBody(required = false) Documentacion mb) {
        return procesarLista(documentacionDao.listar(mb), "No se encontraron documentaciones.");
    }

    /** Registra o actualiza una tarea rutinaria. */
    @PostMapping("/registrar-tarea-rutinaria")
    public ResponseEntity<ApiResponse<?>> registrarTareaRutinaria(@RequestBody TareaRutinaria mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escrituraQueGenera(
                tareaRutinariaDao.registrar(mb, mb.getIdTarRuti() == 0 ? "I" : "U"), "tarea rutinaria editada");
    }

    /** Elimina una tarea rutinaria por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-tarea-rutinaria")
    public ResponseEntity<ApiResponse<?>> eliminarTareaRutinaria(@RequestBody TareaRutinaria mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(tareaRutinariaDao.registrar(mb, "D"));
    }

    /** Obtiene tareas rutinarias. Body null/vacío → retorna todas. */
    @PostMapping("/obtener-tarea-rutinaria")
    public ResponseEntity<ApiResponse<?>> obtenerTareaRutinaria(@RequestBody(required = false) TareaRutinaria mb) {
        return procesarLista(tareaRutinariaDao.listar(mb), "No se encontraron tareas rutinarias.");
    }

    /**
     * Registra o actualiza una llegada (caja fuerte). Solo ROLE_ADM: este
     * endpoint genérico se salta la validación de {@link #marcarLlegadaVerificada}
     * (botón plCajaFuerte) — mismo motivo que {@link #registrarTarRuXCargo}.
     */
    @PostMapping("/registrar-llegada")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> registrarLlegada(@RequestBody Llegada mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(llegadaDao.registrar(mb, mb.getIdRp() == 0 ? "I" : "U"));
    }

    /**
     * Elimina una llegada por su ID. Solo ROLE_ADM (mismo motivo que
     * {@link #registrarLlegada}). audUsuario se resuelve del JWT, no del body.
     */
    @PostMapping("/eliminar-llegada")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> eliminarLlegada(@RequestBody Llegada mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(llegadaDao.registrar(mb, "D"));
    }

    /** Obtiene llegadas. Body null/vacío → retorna todas. */
    @PostMapping("/obtener-llegada")
    public ResponseEntity<ApiResponse<?>> obtenerLlegada(@RequestBody(required = false) Llegada mb) {
        return procesarLista(llegadaDao.listar(mb), "No se encontraron llegadas.");
    }

    /**
     * Registra o actualiza un traspaso de movimiento de caja. Solo ROLE_ADM:
     * este endpoint genérico se salta el flujo de asociación-y-cierre de
     * {@link #confirmarCierreOperaciones} — mismo motivo que
     * {@link #registrarTarRuXCargo}.
     */
    @PostMapping("/registrar-traspaso-mov-caja")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> registrarTraspasoMovCaja(@RequestBody TraspasoMovCaja mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(traspasoMovCajaDao.registrar(mb, mb.getIdTrasp() == 0 ? "I" : "U"));
    }

    /**
     * Elimina un traspaso de movimiento de caja por su ID. Solo ROLE_ADM
     * (mismo motivo que {@link #registrarTraspasoMovCaja}). audUsuario se
     * resuelve del JWT, no del body.
     */
    @PostMapping("/eliminar-traspaso-mov-caja")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> eliminarTraspasoMovCaja(@RequestBody TraspasoMovCaja mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(traspasoMovCajaDao.registrar(mb, "D"));
    }

    /** Obtiene traspasos de movimiento de caja. Body null/vacío → retorna todos. */
    @PostMapping("/obtener-traspaso-mov-caja")
    public ResponseEntity<ApiResponse<?>> obtenerTraspasoMovCaja(@RequestBody(required = false) TraspasoMovCaja mb) {
        return procesarLista(traspasoMovCajaDao.listar(mb), "No se encontraron traspasos de movimiento de caja.");
    }

    // ========= FLUJO ESPECIAL: TRASPASO ENTRE SISTEMAS (idATR=11, tarea 289) =========
    //
    // La tarea existe desde 2023 y el Job la genera a diario para el cargo
    // CAJERO, pero hasta hoy no tenia pantalla: la verificacion de traspasos
    // colgaba de Cierre de Operaciones. Resultado medido el 2026-09-10: 1372
    // ocurrencias generadas y CERO respondidas en tres anos y medio.
    //
    // Los tres endpoints de abajo son esa pantalla.

    /**
     * Los traspasos del dia para verificar.
     *
     * <p>No lee la tabla: el SP consulta SAP y cruza contra lo ya verificado.
     * Puede tardar y puede fallar si el servidor enlazado no responde; eso
     * ultimo se deja fallar a proposito, porque una lista vacia se leeria
     * como "no hubo traspasos" y esa es una afirmacion falsa sobre plata.
     */
    @PostMapping("/traspaso-entre-sistemas/del-dia")
    public ResponseEntity<ApiResponse<?>> traspasosDelDia(
            @RequestBody TraspasoMovCaja filtro) {
        return procesarLista(
                traspasoMovCajaDao.listarDelDia(filtro.getFecha()),
                "No hay traspasos entre sistemas en la fecha indicada.");
    }

    /**
     * Marca un traspaso como verificado, o como que no cuadra.
     *
     * <p>audUsuario sale del token. Si no cuadra, la observacion es
     * obligatoria y la exige el SP (error 23), no esta capa: es el unico
     * punto por el que pasan todos los caminos de escritura.
     *
     * <p>Se marca desde dos lados: la tarea 295 del cajero y la revisión de
     * Cierre de Operaciones (tareas 2, 3 y 39 — archivos SQL 60 y 64). En los
     * dos casos alcanza con tener la ocurrencia: es la tarea la que manda.
     * Quien llegue con la ocurrencia de otra cosa necesita plMovCaja, el botón
     * con el que el sistema anterior mostraba ese panel.
     */
    @PostMapping("/traspaso-entre-sistemas/verificar")
    public ResponseEntity<ApiResponse<?>> verificarTraspaso(
            @RequestBody VerificarTraspasoRequest req, Authentication auth) {
        BitTareaRuti ocurrencia = exigirOcurrenciaPropia(auth, req.getIdBitTarRuti());
        Long tarea = ocurrencia.getIdTarRuti();
        if (!Long.valueOf(TAREA_CAJA_AXA).equals(tarea) && !TAREAS_REVISION_CIERRE.contains(tarea)) {
            accesoModuloHelper.exigirBoton(auth, 78, "plMovCaja");
        }
        return respuestaEscritura(
                traspasoMovCajaDao.verificar(req, DatosToken.codUsuarioDe(auth)));
    }

    /**
     * Cierra el dia como "sin novedad" cuando no hubo ningun traspaso.
     *
     * <p>El SP vuelve a preguntarle a SAP antes de aceptar. Si hay traspasos
     * en esa fecha, rechaza con error 24 en vez de cerrar: sin esa guardia,
     * "sin novedad" seria una afirmacion del cliente y no un hecho.
     */
    @PostMapping("/traspaso-entre-sistemas/sin-novedad")
    public ResponseEntity<ApiResponse<?>> traspasoSinNovedad(
            @RequestBody VerificarTraspasoRequest req, Authentication auth) {
        exigirOcurrenciaPropia(auth, req.getIdBitTarRuti());
        return respuestaEscritura(traspasoMovCajaDao.sinNovedad(
                req.getIdBitTarRuti(), req.getFecha(), DatosToken.codUsuarioDe(auth)));
    }

    // ==================== FLUJO ESPECIAL: TRASPASO DE EFECTIVO ENTRE SISTEMAS — TesBase (idATR=11) ====================
    // La tarea 289 "Verificar Traspaso de Efectivo Entre Sistemas". NO es la de
    // Caja AXA de arriba (idATR 12, tac_traspasoMovCaja): esta es la del
    // subsistema de tesorería TesBase (ttes_TesBase), donde Cobranza registra,
    // todavía en el sistema anterior, las transferencias de un cliente entre
    // bases.
    //
    // Solo se migra la verificación (decisión de Marcelo, 2026-09-11). En el
    // sistema anterior el diálogo cambiaba el estado de cada transferencia pero
    // nunca cerraba la ocurrencia, y la 289 acumuló 1372 sin responder. Aquí,
    // igual que en Caja AXA, "sin pendientes" cierra el día y el SP vuelve a
    // contar antes de aceptar.

    /**
     * Las transferencias pendientes (estado PEN).
     *
     * <p>No dependen de la fecha de la tarea: una transferencia que quedó
     * pendiente ayer sigue pendiente hoy, y es hoy cuando hay que cerrarla.
     */
    @PostMapping("/traspaso-efectivo-tesbase/pendientes")
    public ResponseEntity<ApiResponse<?>> traspasosTesBasePendientes() {
        return procesarLista(tesBaseDao.listarPendientes(),
                "No hay transferencias de efectivo entre sistemas pendientes.");
    }

    /**
     * Cierra una transferencia pendiente (PEN a CER).
     *
     * <p>El SP rechaza si la ocurrencia no es de una tarea con idATR 11 (error
     * 22) o si otra persona ya la cerró (error 23).
     */
    @PostMapping("/traspaso-efectivo-tesbase/cerrar")
    public ResponseEntity<ApiResponse<?>> cerrarTraspasoTesBase(
            @RequestBody CerrarTraspasoTesBaseRequest req, Authentication auth) {
        if (req.getCodTes() == null || req.getIdBitTarRuti() == null) {
            throw new SpBusinessException("Faltan la transferencia o la ocurrencia de la tarea.");
        }
        exigirOcurrenciaPropia(auth, req.getIdBitTarRuti());
        return respuestaEscritura(tesBaseDao.cerrar(
                req.getCodTes(), req.getIdBitTarRuti(), DatosToken.codUsuarioDe(auth)));
    }

    /**
     * Da el día por revisado cuando no queda ninguna transferencia pendiente.
     *
     * <p>El SP vuelve a contar las registradas hasta el día que revisa la
     * ocurrencia (el hábil anterior, archivo SQL 58) y rechaza con error 24 si
     * queda alguna: si el cliente pudiera afirmarlo por su cuenta, el registro
     * no valdría nada.
     */
    @PostMapping("/traspaso-efectivo-tesbase/sin-pendientes")
    public ResponseEntity<ApiResponse<?>> traspasoTesBaseSinPendientes(
            @RequestBody CerrarTraspasoTesBaseRequest req, Authentication auth) {
        if (req.getIdBitTarRuti() == null) {
            throw new SpBusinessException("Falta la ocurrencia de la tarea.");
        }
        exigirOcurrenciaPropia(auth, req.getIdBitTarRuti());
        return respuestaEscritura(tesBaseDao.sinPendientes(
                req.getIdBitTarRuti(), DatosToken.codUsuarioDe(auth)));
    }

    /**
     * Qué día revisa la ocurrencia: el hábil anterior a su fecha, con los
     * feriados de la sucursal de quien la tiene (p_list_TesBase 'H', archivo
     * SQL 58). Marcelo: "tiene que aparecer de un día anterior por defecto y si
     * es feriado, domingo o lunes, de dos días antes".
     *
     * <p>Lo decide el servidor y no la pantalla: "Sin pendientes" cuenta hasta
     * ese mismo día, y la regla tiene que ser una sola.
     */
    @PostMapping("/traspaso-efectivo-tesbase/dia-revisado")
    public ResponseEntity<ApiResponse<?>> traspasoTesBaseDiaRevisado(
            @RequestBody CerrarTraspasoTesBaseRequest req, Authentication auth) {
        if (req.getIdBitTarRuti() == null) {
            throw new SpBusinessException("Falta la ocurrencia de la tarea.");
        }
        exigirOcurrenciaPropia(auth, req.getIdBitTarRuti());
        DiaRevisadoTesBaseDto dia = tesBaseDao.diaRevisado(req.getIdBitTarRuti());
        if (dia == null || dia.getFechaRevisada() == null) {
            throw new SpBusinessException(
                    "La ocurrencia indicada no es de \"Verificar Traspaso de Efectivo Entre Sistemas\".");
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, dia, HttpStatus.OK.value()));
    }

    /**
     * Las transferencias registradas un día, en cualquier estado y con las
     * pendientes primero (p_list_TesBase 'D', archivo SQL 58).
     */
    @PostMapping("/traspaso-efectivo-tesbase/del-dia")
    public ResponseEntity<ApiResponse<?>> traspasosTesBaseDelDia(
            @RequestBody CerrarTraspasoTesBaseRequest req) {
        if (req.getFecha() == null) {
            throw new SpBusinessException("Falta el día a consultar.");
        }
        return procesarLista(tesBaseDao.listarDelDia(req.getFecha()),
                "No se registraron transferencias de efectivo entre sistemas ese día.");
    }

    // ==================== BITÁCORAS (vista tacTareas/Bitacora, archivo SQL 55) ====================
    // Cumplimiento: qué tareas se hicieron y cuáles no. Generación: por qué el
    // Job generó o no una tarea para alguien.
    //
    // Alcance (decisión de Marcelo, 2026-09-10): Sistemas y RR.HH. ven toda la
    // empresa; cualquier otra persona ve su equipo (el subárbol de su cargo) y
    // a sí misma. Se resuelve aquí desde el token y viaja al SP como
    // @codEmpleadoJefe: nada del cuerpo del pedido puede ampliarlo.

    /**
     * Tope de filas del PDF. Un año de toda la empresa son unas 25.000 filas,
     * unas 700 páginas que nadie va a leer; la pantalla sí puede mostrarlas.
     */
    private static final int MAX_FILAS_PDF_BITACORA = 5000;

    /** Bitácora de cumplimiento: una fila por ocurrencia del rango. */
    @PostMapping("/bitacora/cumplimiento")
    public ResponseEntity<ApiResponse<?>> bitacoraCumplimiento(
            @RequestBody BitacoraFiltroRequest filtro, Authentication auth) {
        validarFiltroBitacora(filtro);
        return procesarLista(
                bitTareaRutiDao.bitacoraCumplimiento(filtro, alcanceBitacora(auth)),
                "No hay tareas rutinarias en ese rango con esos filtros.");
    }

    /**
     * La misma bitácora en PDF, con los totales arriba.
     *
     * <p>El porcentaje de cumplimiento es realizadas / (realizadas + no
     * realizadas). "No aplica" y "En plazo" no cuentan: ninguna de las dos es
     * algo que se haya dejado de hacer.
     */
    @PostMapping("/bitacora/cumplimiento-pdf")
    public ResponseEntity<?> bitacoraCumplimientoPdf(
            @RequestBody BitacoraFiltroRequest filtro, Authentication auth) {
        validarFiltroBitacora(filtro);
        Long jefe = alcanceBitacora(auth);
        List<BitacoraCumplimientoDto> filas = bitTareaRutiDao.bitacoraCumplimiento(filtro, jefe);
        if (filas.isEmpty()) {
            throw new SpBusinessException("No hay tareas rutinarias en ese rango con esos filtros.");
        }
        if (filas.size() > MAX_FILAS_PDF_BITACORA) {
            throw new SpBusinessException("Son " + filas.size() + " filas. Para el PDF acota el rango "
                    + "o los filtros (máximo " + MAX_FILAS_PDF_BITACORA + ").");
        }

        int realizadas = 0, noRealizadas = 0, noAplica = 0, enPlazo = 0;
        for (BitacoraCumplimientoDto f : filas) {
            String c = f.getCumplimiento();
            if ("R".equals(c)) realizadas++;
            else if ("N".equals(c)) noRealizadas++;
            else if ("A".equals(c)) noAplica++;
            else enPlazo++;
        }

        SimpleDateFormat dmy = new SimpleDateFormat("dd/MM/yyyy");
        dmy.setTimeZone(TimeZone.getTimeZone("America/La_Paz"));
        StringBuilder totales = new StringBuilder()
                .append("Realizadas ").append(realizadas)
                .append("   ·   No realizadas ").append(noRealizadas)
                .append("   ·   No aplica ").append(noAplica)
                .append("   ·   En plazo ").append(enPlazo);
        if (realizadas + noRealizadas > 0) {
            totales.append("   ·   Cumplimiento ")
                    .append(Math.round(realizadas * 100.0 / (realizadas + noRealizadas))).append(" %");
        }

        Map<String, Object> p = new HashMap<>();
        p.put("fechaGeneracion", new Date());
        p.put("rango", "Del " + dmy.format(filtro.getFechaIni()) + " al " + dmy.format(filtro.getFechaFin())
                + (jefe == null ? "   ·   Toda la empresa" : "   ·   Tu equipo"));
        p.put("totales", totales.toString());

        byte[] bytes = jasperReportExport.exportPDFDesdeColeccion("RptBitacoraCumplimiento", filas, p);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(bytes.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * Resumen de generación: cuántos candidatos cayeron en cada motivo, por
     * corrida del Job.
     *
     * <p>Sin alcance por equipo: son conteos de toda la empresa y no nombran a
     * nadie. Para una persona concreta está {@link #bitacoraPorQue}.
     */
    @PostMapping("/bitacora/generacion")
    public ResponseEntity<ApiResponse<?>> bitacoraGeneracion(
            @RequestBody(required = false) BitacoraFiltroRequest filtro) {
        BitacoraFiltroRequest f = filtro != null ? filtro : new BitacoraFiltroRequest();
        return procesarLista(
                bitTareaRutiDao.resumenGeneracion(f.getFechaIni(), f.getFechaFin()),
                "No hay corridas del generador registradas en ese rango.");
    }

    /**
     * Por qué a una persona le llegó o no cada tarea de sus cargos en una fecha.
     *
     * <p>Sin {@code codEmpleado} se pregunta por uno mismo. Si la persona no es
     * del equipo de quien pregunta, el SP contesta con una fila de filtro -2 y
     * aquí se convierte en un error con ese mensaje.
     */
    @PostMapping("/bitacora/por-que")
    public ResponseEntity<ApiResponse<?>> bitacoraPorQue(
            @RequestBody(required = false) BitacoraFiltroRequest filtro, Authentication auth) {
        BitacoraFiltroRequest f = filtro != null ? filtro : new BitacoraFiltroRequest();
        Long persona = f.getCodEmpleado() != null
                ? f.getCodEmpleado()
                : accesoModuloHelper.codEmpleadoDelToken(auth);
        if (persona == null) {
            throw new SpBusinessException("Tu usuario no tiene un empleado asociado: indica por quién preguntas.");
        }

        List<DiagnosticoGeneracionDto> filas = bitTareaRutiDao.porQue(
                persona, f.getFecha(), f.getIdTarRuti(), alcanceBitacora(auth));
        if (filas.size() == 1 && Integer.valueOf(-2).equals(filas.get(0).getFiltro())) {
            throw new SpBusinessException(filas.get(0).getMotivo());
        }
        return procesarLista(filas,
                "Ninguno de los cargos de esta persona tiene tareas rutinarias asignadas.");
    }

    /** Registra o actualiza una acción de tarea rutinaria (catálogo de idATR). */
    @PostMapping("/registrar-accion-tarea-rutinaria")
    public ResponseEntity<ApiResponse<?>> registrarAccionTareaRutinaria(@RequestBody AccionTareaRutinaria mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(accionTareaRutinariaDao.registrar(mb, mb.getIdATR() == 0 ? "I" : "U"));
    }

    /** Elimina una acción de tarea rutinaria por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-accion-tarea-rutinaria")
    public ResponseEntity<ApiResponse<?>> eliminarAccionTareaRutinaria(@RequestBody AccionTareaRutinaria mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(accionTareaRutinariaDao.registrar(mb, "D"));
    }

    /** Obtiene acciones de tarea rutinaria. Body null/vacío → retorna todas. */
    @PostMapping("/obtener-accion-tarea-rutinaria")
    public ResponseEntity<ApiResponse<?>> obtenerAccionTareaRutinaria(@RequestBody(required = false) AccionTareaRutinaria mb) {
        return procesarLista(accionTareaRutinariaDao.listar(mb), "No se encontraron acciones de tarea rutinaria.");
    }

    /**
     * Registra o actualiza un arqueo de caja de sucursales. Solo ROLE_ADM:
     * este endpoint genérico se salta el recálculo server-side de
     * total/diferencia y el check de botón de {@link #marcarArqueoRevisado} —
     * mismo motivo que {@link #registrarTarRuXCargo}.
     */
    @PostMapping("/registrar-arqueo-caja-sucursales")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> registrarArqueoCajaSucursales(@RequestBody ArqueoCajaSucursales mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(arqueoCajaSucursalesDao.registrar(mb, mb.getIdAC() == 0 ? "I" : "U"));
    }

    /**
     * Elimina un arqueo de caja de sucursales por su ID. Solo ROLE_ADM
     * (mismo motivo que {@link #registrarArqueoCajaSucursales}). audUsuario
     * se resuelve del JWT, no del body.
     */
    @PostMapping("/eliminar-arqueo-caja-sucursales")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> eliminarArqueoCajaSucursales(@RequestBody ArqueoCajaSucursales mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(arqueoCajaSucursalesDao.registrar(mb, "D"));
    }

    /** Obtiene arqueos de caja de sucursales. Body null/vacío → retorna todos. */
    @PostMapping("/obtener-arqueo-caja-sucursales")
    public ResponseEntity<ApiResponse<?>> obtenerArqueoCajaSucursales(@RequestBody(required = false) ArqueoCajaSucursales mb) {
        return procesarLista(arqueoCajaSucursalesDao.listar(mb), "No se encontraron arqueos de caja de sucursales.");
    }

    // ==================== ARQUEO DE CAJA: contexto real (SAP/tc/historial) ====================
    // Cierra el hueco frente al legacy que Marcelo señaló ("falta mucha
    // información") — saldoMovSap/tc NO son manuales en el legacy, y el
    // arqueo anterior se muestra como referencia. Ver
    // sql/2026-09-03_tac_tareaRutinaria_26_arqueo_sap_tc_historial.sql.

    /** Desglose de saldo SAP por caja, para la sucursal de la ocurrencia ({@code idBitTarea} resuelve la sucursal server-side). */
    @PostMapping("/arqueo-caja/saldo-sap")
    public ResponseEntity<ApiResponse<?>> obtenerSaldoSapArqueo(@RequestBody Map<String, Object> body) {
        long idBitTarea = ((Number) body.get("idBitTarea")).longValue();
        return procesarLista(
                sucXMovCajaDao.listarSaldoSapPorOcurrencia(idBitTarea),
                "No se encontró saldo SAP para esta sucursal.");
    }

    /** Tipo de cambio Hoy/Ayer (misma fuente real del legacy — servidor enlazado). */
    @PostMapping("/arqueo-caja/tipo-cambio")
    public ResponseEntity<ApiResponse<?>> obtenerTipoCambioArqueo() {
        return procesarLista(arqueoCajaSucursalesDao.obtenerTipoCambio(), "No se pudo obtener el tipo de cambio.");
    }

    /** El arqueo anterior (más reciente antes de hoy) de la sucursal de la ocurrencia, como contexto/comparación. */
    @PostMapping("/arqueo-caja/anterior")
    public ResponseEntity<ApiResponse<?>> obtenerArqueoAnterior(@RequestBody ArqueoCajaSucursales mb) {
        return procesarLista(
                arqueoCajaSucursalesDao.obtenerAnterior(mb.getIdBitTarea()),
                "No hay un arqueo anterior para esta sucursal.");
    }

    /** Registra o actualiza el monto de caja chica por sucursal. */
    @PostMapping("/registrar-monto-caja-chica-x-suc")
    public ResponseEntity<ApiResponse<?>> registrarMontoCajaChicaXSuc(@RequestBody MontoCajaChicaXSuc mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(montoCajaChicaXSucDao.registrar(mb, mb.getIdCS() == 0 ? "I" : "U"));
    }

    /** Elimina un monto de caja chica por sucursal por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-monto-caja-chica-x-suc")
    public ResponseEntity<ApiResponse<?>> eliminarMontoCajaChicaXSuc(@RequestBody MontoCajaChicaXSuc mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(montoCajaChicaXSucDao.registrar(mb, "D"));
    }

    /** Obtiene montos de caja chica por sucursal. Body null/vacío → retorna todos. */
    @PostMapping("/obtener-monto-caja-chica-x-suc")
    public ResponseEntity<ApiResponse<?>> obtenerMontoCajaChicaXSuc(@RequestBody(required = false) MontoCajaChicaXSuc mb) {
        return procesarLista(montoCajaChicaXSucDao.listar(mb), "No se encontraron montos de caja chica por sucursal.");
    }

    /**
     * Registra o actualiza un movimiento de caja chica. Solo ROLE_ADM: este
     * endpoint genérico se salta la validación de saldo suficiente de
     * {@link #registrarEgresoCajaChica} — mismo motivo que
     * {@link #registrarTarRuXCargo}.
     */
    @PostMapping("/registrar-caja-chica")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> registrarCajaChica(@RequestBody CajaChica mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(cajaChicaDao.registrar(mb, mb.getIdCC() == 0 ? "I" : "U"));
    }

    /**
     * Elimina un movimiento de caja chica por su ID. Solo ROLE_ADM (mismo
     * motivo que {@link #registrarCajaChica}). audUsuario se resuelve del
     * JWT, no del body.
     */
    @PostMapping("/eliminar-caja-chica")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> eliminarCajaChica(@RequestBody CajaChica mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(cajaChicaDao.registrar(mb, "D"));
    }

    /** Obtiene movimientos de caja chica. Body null/vacío → retorna todos. */
    @PostMapping("/obtener-caja-chica")
    public ResponseEntity<ApiResponse<?>> obtenerCajaChica(@RequestBody(required = false) CajaChica mb) {
        return procesarLista(cajaChicaDao.listar(mb), "No se encontraron movimientos de caja chica.");
    }

    /** Registra o actualiza la relación sucursal × cuenta de movimiento de caja SAP. */
    @PostMapping("/registrar-suc-x-mov-caja")
    public ResponseEntity<ApiResponse<?>> registrarSucXMovCaja(@RequestBody SucXMovCaja mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(sucXMovCajaDao.registrar(mb, mb.getIdSxMC() == 0 ? "I" : "U"));
    }

    /** Elimina una relación sucursal × cuenta de movimiento de caja por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-suc-x-mov-caja")
    public ResponseEntity<ApiResponse<?>> eliminarSucXMovCaja(@RequestBody SucXMovCaja mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(sucXMovCajaDao.registrar(mb, "D"));
    }

    /** Obtiene relaciones sucursal × cuenta de movimiento de caja. Body null/vacío → retorna todas. */
    @PostMapping("/obtener-suc-x-mov-caja")
    public ResponseEntity<ApiResponse<?>> obtenerSucXMovCaja(@RequestBody(required = false) SucXMovCaja mb) {
        return procesarLista(sucXMovCajaDao.listar(mb), "No se encontraron relaciones sucursal × cuenta de caja.");
    }

    /**
     * Registra o actualiza una bitácora de tarea rutinaria (ocurrencia del
     * día). Abierto a cualquier usuario autenticado a propósito: este es el
     * endpoint real detrás del diálogo simple Sí/No/No aplica de
     * {@code MisTareasRutinariasScreen} (caso idATR por defecto, el más
     * común — cada usuario cierra sus propias ocurrencias del día), no un
     * flujo administrativo. No confundir con {@link #eliminarBitTareaRuti},
     * que sí queda restringido.
     *
     * <p>Guardia de dueño agregada 2026-09-07 (hallazgo de auditoría): en una
     * actualización (idBitTarea != 0) se exige que la ocurrencia sea del
     * empleado del caller — antes el único freno era un filtro client-side
     * en Flutter, fail-open mientras {@code userProvider} todavía carga
     * (ventana real de "ve y puede cerrar cualquier ocurrencia del org
     * entero"). ROLE_ADM se exceptúa (mismo criterio que
     * {@code tienePermisoDeBoton} del lado Flutter: el admin siempre pasa).
     */
    @PostMapping("/registrar-bit-tarea-ruti")
    public ResponseEntity<ApiResponse<?>> registrarBitTareaRuti(@RequestBody BitTareaRuti mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        if (mb.getIdBitTarea() != 0 && !accesoModuloHelper.esAdmin(auth)) {
            BitTareaRuti existente = bitTareaRutiDao.obtenerPorId(mb.getIdBitTarea());
            if (existente == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new ApiResponse<>("No existe la bitácora indicada.", null, HttpStatus.BAD_REQUEST.value()));
            }
            Long miCodEmpleado = accesoModuloHelper.codEmpleadoDelToken(auth);
            if (miCodEmpleado == null || !miCodEmpleado.equals(existente.getCodEmpleado())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new ApiResponse<>("Esta ocurrencia no está asignada a tu usuario.", null, HttpStatus.FORBIDDEN.value()));
            }
            // Una tarea con pantalla propia (arqueo, traspasos, revisión del
            // cierre…) que ya se hizo no vuelve a pendiente por este camino: sería
            // la puerta para hacerla de nuevo. Lo único que admite es una
            // observación (/agregar-observacion). Las simples no cambian: no
            // tienen una segunda escritura detrás.
            if (Integer.valueOf(13).equals(existente.getFueRealizado())
                    && TIPOS_CON_PANTALLA_PROPIA.contains(existente.getIdATR())
                    && !Integer.valueOf(13).equals(mb.getFueRealizado())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new ApiResponse<>("Esta tarea ya está hecha y no se vuelve a hacer. Solo puedes agregarle una observación.",
                                null, HttpStatus.BAD_REQUEST.value()));
            }
        }
        return respuestaEscritura(bitTareaRutiDao.registrar(mb, mb.getIdBitTarea() == 0 ? "I" : "U"));
    }

    /**
     * Agrega una observación a una tarea ya respondida — lo único que se puede
     * hacer con ella (Marcelo, 2026-10-05: "una vez que realiza esas tareas que
     * ya no la vuelva hacer. Máximo agregar una observación").
     *
     * <p>Del body se leen solo {@code idBitTarea} y {@code obs}. La ocurrencia
     * tiene que ser de quien llama (o tener btnEmpAll). Que esté respondida, el
     * largo y el formato los resuelve el SP (p_abm_tac_BitTareaRuti 'O',
     * archivo SQL 75): agrega al final, con fecha y hora, sin borrar lo que
     * había.
     */
    @PostMapping("/agregar-observacion")
    public ResponseEntity<ApiResponse<?>> agregarObservacion(@RequestBody BitTareaRuti mb, Authentication auth) {
        String obs = mb.getObs() == null ? "" : mb.getObs().trim();
        if (obs.isEmpty()) {
            throw new SpBusinessException("Escribe la observación que quieres agregar.");
        }
        if (obs.length() > MAX_OBSERVACION) {
            throw new SpBusinessException("La observación puede tener hasta " + MAX_OBSERVACION + " caracteres.");
        }
        exigirOcurrenciaPropia(auth, mb.getIdBitTarea());
        return respuestaEscritura(
                bitTareaRutiDao.agregarObservacion(mb.getIdBitTarea(), obs, DatosToken.codUsuarioDe(auth)));
    }

    /** Largo máximo de una observación agregada; la columna admite 2000 en total. */
    private static final int MAX_OBSERVACION = 500;

    /**
     * Los idATR que se responden en una pantalla propia y no con Sí/No — los
     * mismos que {@code TareaPendienteTile.tipoDeAccion} en la app: arqueo (2),
     * cierre (3), caja fuerte (4), verificar cierre (5), coches (6), caja chica
     * (7), TesBase (11) y Caja AXA (12).
     */
    private static final Set<Integer> TIPOS_CON_PANTALLA_PROPIA =
            new HashSet<>(Arrays.asList(2, 3, 4, 5, 6, 7, 11, 12));

    /**
     * Elimina una bitácora de tarea rutinaria por su ID. Solo ROLE_ADM: a
     * diferencia de {@link #registrarBitTareaRuti} (el cierre normal de una
     * ocurrencia), este borra el registro por completo y no tiene un caller
     * legítimo fuera de un flujo administrativo. audUsuario se resuelve del
     * JWT, no del body.
     */
    @PostMapping("/eliminar-bit-tarea-ruti")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> eliminarBitTareaRuti(@RequestBody BitTareaRuti mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(bitTareaRutiDao.registrar(mb, "D"));
    }

    /** Obtiene bitácoras de tarea rutinaria. Body null/vacío → retorna todas. */
    @PostMapping("/obtener-bit-tarea-ruti")
    public ResponseEntity<ApiResponse<?>> obtenerBitTareaRuti(@RequestBody(required = false) BitTareaRuti mb) {
        return procesarLista(bitTareaRutiDao.listar(mb), "No se encontraron bitácoras de tarea rutinaria.");
    }

    /** Registra o actualiza un movimiento de caja (SAP, ligado a un arqueo). */
    @PostMapping("/registrar-mov-caja")
    public ResponseEntity<ApiResponse<?>> registrarMovCaja(@RequestBody MovCaja mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(movCajaDao.registrar(mb, mb.getIdMC() == 0 ? "I" : "U"));
    }

    /** Elimina un movimiento de caja por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-mov-caja")
    public ResponseEntity<ApiResponse<?>> eliminarMovCaja(@RequestBody MovCaja mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(movCajaDao.registrar(mb, "D"));
    }

    /** Obtiene movimientos de caja. Body null/vacío → retorna todos. */
    @PostMapping("/obtener-mov-caja")
    public ResponseEntity<ApiResponse<?>> obtenerMovCaja(@RequestBody(required = false) MovCaja mb) {
        return procesarLista(movCajaDao.listar(mb), "No se encontraron movimientos de caja.");
    }

    /** Registra o actualiza una frecuencia (catálogo: diaria/semanal/mensual/semestral/anual). */
    @PostMapping("/registrar-frecuencia")
    public ResponseEntity<ApiResponse<?>> registrarFrecuencia(@RequestBody Frecuencia mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(frecuenciaDao.registrar(mb, mb.getIdFrec() == 0 ? "I" : "U"));
    }

    /** Elimina una frecuencia por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-frecuencia")
    public ResponseEntity<ApiResponse<?>> eliminarFrecuencia(@RequestBody Frecuencia mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(frecuenciaDao.registrar(mb, "D"));
    }

    /** Obtiene frecuencias. Body null/vacío → retorna todas. */
    @PostMapping("/obtener-frecuencia")
    public ResponseEntity<ApiResponse<?>> obtenerFrecuencia(@RequestBody(required = false) Frecuencia mb) {
        return procesarLista(frecuenciaDao.listar(mb), "No se encontraron frecuencias.");
    }

    /** Registra o actualiza un corte (catálogo: denominación de billete/moneda para arqueo). */
    @PostMapping("/registrar-corte")
    public ResponseEntity<ApiResponse<?>> registrarCorte(@RequestBody Corte mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(corteDao.registrar(mb, mb.getIdCorte() == 0 ? "I" : "U"));
    }

    /** Elimina un corte por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-corte")
    public ResponseEntity<ApiResponse<?>> eliminarCorte(@RequestBody Corte mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(corteDao.registrar(mb, "D"));
    }

    /** Obtiene cortes. Body null/vacío → retorna todos. */
    @PostMapping("/obtener-corte")
    public ResponseEntity<ApiResponse<?>> obtenerCorte(@RequestBody(required = false) Corte mb) {
        return procesarLista(corteDao.listar(mb), "No se encontraron cortes.");
    }

    /**
     * Registra o actualiza una asignación de cargo a tarea rutinaria — CRUD
     * estándar. Distinto de {@link #registrarTareaRutinariaConCargos}: este
     * endpoint es la edición simple de UNA fila ya existente (o alta suelta
     * sin el flujo de autorización por subárbol); el otro es el alta
     * transaccional con validación de dependientes.
     * <p>
     * <b>Por qué requiere el botón btnTareasRutXCargo (encontrado en
     * code-review, 2026-09-02; convertido de ROLE_ADM hardcodeado a botón
     * real el 2026-09-07):</b> al no pasar por la validación de subárbol,
     * cualquiera sin este botón podía asignar CUALQUIER tarea a CUALQUIER
     * cargo de la organización, sin las restricciones que sí aplica
     * {@link #registrarTareaRutinariaConCargos} — un bypass real del flujo
     * de autorización jefe→dependientes. Igual que en el legacy, la
     * asignación directa de cargo↔tarea es una operación administrativa
     * (WizardEstOrg), no de autoservicio — el botón (vista 12) es el mismo
     * que gatea el resto de la pantalla "Tareas Rutinarias por Cargo".
     */
    @PostMapping("/registrar-tar-ru-x-cargo")
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    public ResponseEntity<ApiResponse<?>> registrarTarRuXCargo(@RequestBody TarRuXCargo mb, Authentication auth) {
        accesoModuloHelper.exigirBoton(auth, 12, "btnTareasRutXCargo");
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escrituraQueGenera(
                tarRuXCargoDao.registrar(mb, mb.getIdTarXCargo() == 0 ? "I" : "U"), "asignacion de tarea a cargo");
    }

    /**
     * Elimina una asignación de cargo a tarea rutinaria por su ID. Requiere
     * el botón btnTareasRutXCargo — mismo motivo que
     * {@link #registrarTarRuXCargo}. audUsuario se resuelve del JWT, no del
     * body.
     */
    @PostMapping("/eliminar-tar-ru-x-cargo")
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    public ResponseEntity<ApiResponse<?>> eliminarTarRuXCargo(@RequestBody TarRuXCargo mb, Authentication auth) {
        accesoModuloHelper.exigirBoton(auth, 12, "btnTareasRutXCargo");
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(tarRuXCargoDao.registrar(mb, "D"));
    }

    /** Obtiene asignaciones de cargo a tarea rutinaria. Body null/vacío → retorna todas. */
    @PostMapping("/obtener-tar-ru-x-cargo")
    public ResponseEntity<ApiResponse<?>> obtenerTarRuXCargo(@RequestBody(required = false) TarRuXCargo mb) {
        return procesarLista(tarRuXCargoDao.listar(mb), "No se encontraron asignaciones de cargo a tarea rutinaria.");
    }

    /** Registra o actualiza un detalle de arqueo de caja de sucursales (desglose de cortes). */
    @PostMapping("/registrar-det-arqueo-caja-sucursales")
    public ResponseEntity<ApiResponse<?>> registrarDetArqueoCajaSucursales(@RequestBody DetArqueoCajaSucursales mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(detArqueoCajaSucursalesDao.registrar(mb, mb.getIdDetAS() == 0 ? "I" : "U"));
    }

    /** Elimina un detalle de arqueo de caja de sucursales por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-det-arqueo-caja-sucursales")
    public ResponseEntity<ApiResponse<?>> eliminarDetArqueoCajaSucursales(@RequestBody DetArqueoCajaSucursales mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(detArqueoCajaSucursalesDao.registrar(mb, "D"));
    }

    /** Obtiene detalles de arqueo de caja de sucursales. Body null/vacío → retorna todos. */
    @PostMapping("/obtener-det-arqueo-caja-sucursales")
    public ResponseEntity<ApiResponse<?>> obtenerDetArqueoCajaSucursales(@RequestBody(required = false) DetArqueoCajaSucursales mb) {
        return procesarLista(detArqueoCajaSucursalesDao.listar(mb), "No se encontraron detalles de arqueo de caja de sucursales.");
    }

    /** Registra o actualiza un vale (voucher contado como parte de un arqueo). */
    @PostMapping("/registrar-vale")
    public ResponseEntity<ApiResponse<?>> registrarVale(@RequestBody Vale mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(valeDao.registrar(mb, mb.getIdVale() == 0 ? "I" : "U"));
    }

    /** Elimina un vale por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-vale")
    public ResponseEntity<ApiResponse<?>> eliminarVale(@RequestBody Vale mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(valeDao.registrar(mb, "D"));
    }

    /** Obtiene vales. Body null/vacío → retorna todos. */
    @PostMapping("/obtener-vale")
    public ResponseEntity<ApiResponse<?>> obtenerVale(@RequestBody(required = false) Vale mb) {
        return procesarLista(valeDao.listar(mb), "No se encontraron vales.");
    }

    /** Registra o actualiza un detalle de documentación (monto adjunto a un arqueo). */
    @PostMapping("/registrar-det-documentacion")
    public ResponseEntity<ApiResponse<?>> registrarDetDocumentacion(@RequestBody DetDocumentacion mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(detDocumentacionDao.registrar(mb, mb.getIdDetDoc() == 0 ? "I" : "U"));
    }

    /** Elimina un detalle de documentación por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-det-documentacion")
    public ResponseEntity<ApiResponse<?>> eliminarDetDocumentacion(@RequestBody DetDocumentacion mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(detDocumentacionDao.registrar(mb, "D"));
    }

    /** Obtiene detalles de documentación. Body null/vacío → retorna todos. */
    @PostMapping("/obtener-det-documentacion")
    public ResponseEntity<ApiResponse<?>> obtenerDetDocumentacion(@RequestBody(required = false) DetDocumentacion mb) {
        return procesarLista(detDocumentacionDao.listar(mb), "No se encontraron detalles de documentación.");
    }

    /**
     * Registra o actualiza una llegada de coche (control de vehículos por
     * tarea rutinaria). Solo ROLE_ADM: este endpoint genérico se salta el
     * auto-cierre por-último-coche de {@link #marcarLlegadaCoche} — mismo
     * motivo que {@link #registrarTarRuXCargo}.
     */
    @PostMapping("/registrar-coche-llegadas")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> registrarCocheLlegadas(@RequestBody CocheLlegadas mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(cocheLlegadasDao.registrar(mb, mb.getIdCo() == 0 ? "I" : "U"));
    }

    /**
     * Elimina una llegada de coche por su ID. Solo ROLE_ADM (mismo motivo
     * que {@link #registrarCocheLlegadas}). audUsuario se resuelve del JWT,
     * no del body.
     */
    @PostMapping("/eliminar-coche-llegadas")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> eliminarCocheLlegadas(@RequestBody CocheLlegadas mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(cocheLlegadasDao.registrar(mb, "D"));
    }

    /** Obtiene llegadas de coche. Body null/vacío → retorna todas. */
    @PostMapping("/obtener-coche-llegadas")
    public ResponseEntity<ApiResponse<?>> obtenerCocheLlegadas(@RequestBody(required = false) CocheLlegadas mb) {
        return procesarLista(cocheLlegadasDao.listar(mb), "No se encontraron llegadas de coche.");
    }

    /** Registra o actualiza un coche (catálogo de vehículos por sucursal). */
    @PostMapping("/registrar-coche")
    public ResponseEntity<ApiResponse<?>> registrarCoche(@RequestBody Coche mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(cocheDao.registrar(mb, mb.getIdCoche() == 0 ? "I" : "U"));
    }

    /** Elimina un coche por su ID. audUsuario se resuelve del JWT, no del body. */
    @PostMapping("/eliminar-coche")
    public ResponseEntity<ApiResponse<?>> eliminarCoche(@RequestBody Coche mb, Authentication auth) {
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return respuestaEscritura(cocheDao.registrar(mb, "D"));
    }

    /** Obtiene coches. Body null/vacío → retorna todos. */
    @PostMapping("/obtener-coche")
    public ResponseEntity<ApiResponse<?>> obtenerCoche(@RequestBody(required = false) Coche mb) {
        return procesarLista(cocheDao.listar(mb), "No se encontraron coches.");
    }

    // ==================== HELPERS DE ALCANCE ====================

    /**
     * Corta si la ocurrencia no es de quien llama.
     *
     * <p>Los procs de Caja AXA y TesBase validan que la ocurrencia sea del tipo
     * de tarea correcto, pero no de quién es: sin esto, cualquier usuario
     * autenticado podía responder la tarea de otra persona mandando su id.
     * Quien tiene btnEmpAll (vista 78) sí puede, igual que para ver las tareas
     * de otros.
     */
    private BitTareaRuti exigirOcurrenciaPropia(Authentication auth, long idBitTarRuti) {
        BitTareaRuti ocurrencia = bitTareaRutiDao.obtenerPorId(idBitTarRuti);
        if (ocurrencia == null) {
            throw new SpBusinessException("La ocurrencia indicada no existe.");
        }
        Long yo = accesoModuloHelper.codEmpleadoDelToken(auth);
        boolean propia = yo != null && ocurrencia.getCodEmpleado() != null
                && yo.longValue() == ocurrencia.getCodEmpleado().longValue();
        if (!propia) {
            accesoModuloHelper.exigirBoton(auth, 78, "btnEmpAll");
        }
        return ocurrencia;
    }

    /**
     * La tarea es el permiso: con la ocurrencia de una tarea de revisión
     * alcanza para leer y marcar las secciones del cierre. Sin ella (un
     * llamado viejo, que no manda idBitTarea) se cae al ACL de siempre, el
     * botón de la vista 78.
     */
    private void exigirRevisionOBoton(Authentication auth, long idBitTarea, String boton) {
        if (idBitTarea > 0) {
            exigirOcurrenciaDeCierre(auth, idBitTarea, TAREAS_REVISION_CIERRE);
            return;
        }
        accesoModuloHelper.exigirBoton(auth, 78, boton);
    }

    /**
     * {@link #exigirOcurrenciaPropia}, y además que sea de una de {@code tareas}.
     * Sin esto, la ocurrencia propia de cualquier otra tarea servía para abrir
     * la revisión del cierre o para cerrarla.
     */
    private BitTareaRuti exigirOcurrenciaDeCierre(Authentication auth, long idBitTarea, Set<Long> tareas) {
        BitTareaRuti ocurrencia = exigirOcurrenciaPropia(auth, idBitTarea);
        if (ocurrencia.getIdTarRuti() == null || !tareas.contains(ocurrencia.getIdTarRuti())) {
            throw new SpBusinessException("La ocurrencia indicada no es de Cierre de Operaciones.");
        }
        return ocurrencia;
    }

    /**
     * El alcance de las bitácoras: null = toda la empresa (Sistemas o RR.HH.);
     * si no, el codEmpleado de quien llama, que el SP expande a su subárbol.
     *
     * <p>Falla cerrado: un usuario sin empleado asociado no tiene equipo, y
     * devolver null aquí le mostraría la empresa entera.
     */
    private Long alcanceBitacora(Authentication auth) {
        if (accesoModuloHelper.esAdminORrhh(auth)) return null;
        Long yo = accesoModuloHelper.codEmpleadoDelToken(auth);
        if (yo == null) {
            throw new SpBusinessException("Tu usuario no tiene un empleado asociado: no hay un equipo que mostrar.");
        }
        return yo;
    }

    /**
     * Lo mismo que valida el SP, pero antes de llamarlo: un RAISERROR del SP
     * llega como error de base de datos (500), no como un mensaje para quien
     * eligió mal las fechas.
     */
    private void validarFiltroBitacora(BitacoraFiltroRequest f) {
        if (f == null || f.getFechaIni() == null || f.getFechaFin() == null) {
            throw new SpBusinessException("Indica el rango de fechas.");
        }
        if (f.getFechaFin().before(f.getFechaIni())) {
            throw new SpBusinessException("La fecha final es anterior a la inicial.");
        }
        if (TimeUnit.MILLISECONDS.toDays(f.getFechaFin().getTime() - f.getFechaIni().getTime()) > 366) {
            throw new SpBusinessException("El rango no puede pasar de un año.");
        }
        // @cumplimiento es CHAR(1): "RN" llegaría truncado a "R" sin error.
        String c = f.getCumplimiento();
        if (c != null && !c.trim().isEmpty()
                && !(c.trim().length() == 1 && "RNAP".indexOf(c.trim().charAt(0)) >= 0)) {
            throw new SpBusinessException("Estado de cumplimiento no válido: " + c);
        }
    }

    // ==================== HELPERS DE RESPUESTA (calcados de PagosExtranjerosController) ====================

    /**
     * Lanza SpBusinessException si el SP devolvió un error de negocio.
     * Capturada por GlobalExceptionHandler → HTTP 400.
     */
    private void ejecutar(RespuestaSp res, String contexto) {
        if (res.getError() != 0) {
            throw new bo.bosque.com.impexpap.config.SpBusinessException(contexto + ": " + res.getErrormsg());
        }
    }

    /** 201/400 según el resultado del SP, con el mensaje específico del SP. */
    /**
     * {@link #respuestaEscritura} para las escrituras que cambian QUÉ se
     * genera —asignar una tarea a un cargo, activarla, editarla—: si salió
     * bien, pide una corrida del generador para que la ocurrencia de hoy
     * aparezca ya y no a las 00:05. No espera a que termine: ver
     * {@link GeneracionAlInstante}.
     */
    private ResponseEntity<ApiResponse<?>> escrituraQueGenera(RespuestaSp res, String motivo) {
        if (res != null && res.getError() == 0) {
            generacionAlInstante.pedir(motivo);
        }
        return respuestaEscritura(res);
    }

    private ResponseEntity<ApiResponse<?>> respuestaEscritura(RespuestaSp res) {
        HttpStatus status = res.getError() == 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), status.value()));
    }

    /** 200 con la lista si tiene elementos, 204 con mensaje informativo si está vacía/null. */
    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, String mensajeVacio) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}
