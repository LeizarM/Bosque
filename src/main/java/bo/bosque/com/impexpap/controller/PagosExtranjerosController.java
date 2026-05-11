package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.service.FileStorageService;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.dto.ConfirmarPagoRequest;
import bo.bosque.com.impexpap.dto.FiltroFechasDto;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Controlador REST para la gestión completa de pagos al exterior.
 * <p>
 * Este controlador maneja todo el ciclo de vida de un pago internacional:
 * <ol>
 *   <li><b>FASE 1 - Solicitud:</b> Se crea una solicitud de pago con proveedores y facturas</li>
 *   <li><b>FASE 2 - Cotización:</b> Los bancos ofertan tipos de cambio y cargos</li>
 *   <li><b>FASE 3 - Aceptación:</b> Se elige la cotización ganadora</li>
 *   <li><b>FASE 4 - Transacción:</b> Se ejecuta el pago con el banco ganador</li>
 *   <li><b>FASE 5 - Confirmación:</b> Se confirma el pago y se cierra la solicitud</li>
 * </ol>
 * <p>
 * <b>Arquitectura:</b> Sin JPA/Hibernate. Toda la persistencia se realiza mediante
 * Stored Procedures de SQL Server, invocados a través de JdbcTemplate + SimpleJdbcCall
 * vía la utilidad SpHelper que serializa los modelos a Map<String,Object> con Jackson.
 * <p>
 * <b>Convención de SPs:</b>
 * <ul>
 *   <li>ABM: {@code p_abm_tpex_<Tabla>} — Reciben ACCION: "I"=Insert, "U"=Update, "D"=Delete</li>
 *   <li>Listado: {@code p_list_tpex_<Tabla>} — Reciben ACCION: "L"=por ID, "B"=rango/banco, "S"=por solicitud, etc.</li>
 * </ul>
 * <p>
 * <b>Seguridad:</b> Todos los endpoints requieren JWT válido con rol ROLE_ADM o ROLE_LIM.
 * <p>
 * <b>Estados del flujo:</b>
 * <pre>
 * SolicitudPago:  PENDIENTE → APROBADA → PAGADA
 * Cotizaciones:   VIGENTE → ACEPTADA / RECHAZADA
 * Transacciones:  PENDIENTE → PROCESADO → CONFIRMADO
 * </pre>
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/pagos-extranjeros")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class PagosExtranjerosController {

    /** Mensaje estándar de éxito retornado en todas las respuestas exitosas. */
    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    /** Extensiones de archivo permitidas para los vouchers de transacción. */
    private static final Set<String> EXTENSIONES_VOUCHER = new HashSet<>(Arrays.asList("pdf", "jpg", "jpeg", "png"));

    // ==================== DEPENDENCIAS (13 DAOs + FileStorageService) ====================
    // Cada DAO encapsula llamadas a Stored Procedures vía SpHelper.
    // La separación por interfaz permite testeo y desacoplamiento.

    /** DAO para la cabecera de solicitudes de pago (tpex_SolicitudPago). */
    private final ISolicitudPago solicitudPagoDao;

    /** DAO para los proveedores dentro de una solicitud (tpex_SolicitudProveedor). */
    private final ISolicitudProveedor solicitudProveedorDao;

    /** DAO para las facturas/detalles de cada proveedor (tpex_DetalleSolicitud). */
    private final IDetalleSolicitud detalleSolicitudDao;

    /** DAO para catálogo de canales de pago (tpex_CanalesPago). */
    private final ICanalesPago canalesPagoDao;

    /** DAO para catálogo de monedas (tpex_Monedas). */
    private final IMonedas monedasDao;

    /** DAO para tipos de cambio por banco (tpex_TiposCambio). */
    private final ITiposCambio tiposCambioDao;

    /** DAO para catálogo de tipos de cargo (tpex_TiposCargo). */
    private final ITiposCargo tiposCargoDao;

    /** DAO para catálogo de tipos de transacción (tpex_TiposTransaccion). */
    private final ITiposTransaccion tiposTransaccionDao;

    /** DAO para las transacciones bancarias ejecutadas (tpex_Transacciones). */
    private final ITransacciones transaccionesDao;

    /** DAO para el log de cambios de estado (tpex_LogEstados). */
    private final ILogEstados logEstadosDao;

    /** DAO para las cotizaciones ofertadas por bancos (tpex_Cotizaciones). */
    private final ICotizaciones cotizacionesDao;

    /** DAO para la configuración de comisiones por banco (tpex_ConfigComisionesBanco). */
    private final IConfigComisionesBanco configComisionesBancoDao;

    /** DAO para los cargos aplicados a cotizaciones o transacciones (tpex_Cargos). */
    private final ICargoPago cargoPagoDao;

    /** DAO para los asientos contables de una transacción (tpex_Asientos). */
    private final IAsientos asientosDao;

    /** Servicio para guardar/leer archivos de voucher en el filesystem. */
    private final FileStorageService fileStorageService;

    public PagosExtranjerosController(
            ISolicitudPago solicitudPagoDao, ISolicitudProveedor solicitudProveedorDao,
            IDetalleSolicitud detalleSolicitudDao, ICanalesPago canalesPagoDao,
            IMonedas monedasDao, ITiposCambio tiposCambioDao, ITiposCargo tiposCargoDao,
            ITiposTransaccion tiposTransaccionDao, ITransacciones transaccionesDao,
            ILogEstados logEstadosDao, ICotizaciones cotizacionesDao,
            IConfigComisionesBanco configComisionesBancoDao, ICargoPago cargoPagoDao,
            IAsientos asientosDao, FileStorageService fileStorageService) {
        this.solicitudPagoDao = solicitudPagoDao;
        this.solicitudProveedorDao = solicitudProveedorDao;
        this.detalleSolicitudDao = detalleSolicitudDao;
        this.canalesPagoDao = canalesPagoDao;
        this.monedasDao = monedasDao;
        this.tiposCambioDao = tiposCambioDao;
        this.tiposCargoDao = tiposCargoDao;
        this.tiposTransaccionDao = tiposTransaccionDao;
        this.transaccionesDao = transaccionesDao;
        this.logEstadosDao = logEstadosDao;
        this.cotizacionesDao = cotizacionesDao;
        this.configComisionesBancoDao = configComisionesBancoDao;
        this.cargoPagoDao = cargoPagoDao;
        this.asientosDao = asientosDao;
        this.fileStorageService = fileStorageService;
    }

    // ==================== FASE 1: SOLICITUD DE PAGO ====================

    /**
     * Guarda una solicitud de pago completa (cabecera + proveedores + facturas).
     * <p>
     * Este es el punto de entrada principal del flujo. Recibe un objeto anidado
     * que contiene toda la estructura: SolicitudPago → SolicitudProveedor[] → DetalleSolicitud[].
     * <p>
     * <b>Orden de operaciones (importante para integridad referencial):</b>
     * <ol>
     *   <li>Elimina facturas marcadas en {@code detallesAEliminar} de cada proveedor</li>
     *   <li>Elimina proveedores marcados en {@code proveedoresAEliminar}</li>
     *   <li>Inserta/actualiza la cabecera SolicitudPago</li>
     *   <li>Por cada proveedor: inserta/actualiza SolicitudProveedor</li>
     *   <li>Por cada factura del proveedor: inserta/actualiza DetalleSolicitud</li>
     * </ol>
     * <p>
     * <b>Lógica de ACCION:</b> Si {@code idSolicitud == 0} → INSERT ("I"),
     * si {@code idSolicitud > 0} → UPDATE ("U"). Igual para proveedores y detalles.
     * <p>
     * <b>Transaccional:</b> Si cualquier SP falla, se hace rollback de toda la operación.
     *
     * @param payload Objeto SolicitudPago con proveedores y detalles anidados
     * @return 201 Created con el ID de la solicitud
     */
    @PostMapping("/guardar-solicitud-completa")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardarSolicitudCompleta(@RequestBody SolicitudPago payload) {

        // --- PASO 1: Eliminar facturas marcadas para eliminación ---
        // Cada proveedor puede tener una lista de IDs de detalles a eliminar
        if (payload.getProveedores() != null) {
            for (SolicitudProveedor prov : payload.getProveedores()) {
                if (prov.getDetallesAEliminar() != null) {
                    for (Long idDetalleDel : prov.getDetallesAEliminar()) {
                        DetalleSolicitud detDel = new DetalleSolicitud();
                        detDel.setIdDetalle(idDetalleDel);
                        detDel.setAudUsuario(payload.getAudUsuario());
                        // ACCION "D" = Delete en el SP p_abm_tpex_DetalleSolicitud
                        ejecutar(detalleSolicitudDao.registrarDetalleSolicitud(detDel, "D"), "Error eliminando factura ID=" + idDetalleDel);
                    }
                }
            }
        }

        // --- PASO 2: Eliminar proveedores marcados para eliminación ---
        if (payload.getProveedoresAEliminar() != null) {
            for (Long idProvDel : payload.getProveedoresAEliminar()) {
                SolicitudProveedor provDel = new SolicitudProveedor();
                provDel.setIdSolicitudProveedor(idProvDel);
                provDel.setAudUsuario(payload.getAudUsuario());
                // ACCION "D" = Delete en el SP p_abm_tpex_SolicitudProveedor
                ejecutar(solicitudProveedorDao.registrarSolicitudProveedor(provDel, "D"), "Error eliminando proveedor ID=" + idProvDel);
            }
        }

        // --- PASO 3: Insertar/actualizar la cabecera de la solicitud ---
        // Si idSolicitud == 0 es nueva (INSERT), si > 0 es edición (UPDATE)
        String accionSol = payload.getIdSolicitud() == 0 ? "I" : "U";
        RespuestaSp resSol = solicitudPagoDao.registrarSolicitudPago(payload, accionSol);
        ejecutar(resSol, "Error en cabecera de solicitud");

        // El SP retorna idGenerado con el nuevo ID si fue INSERT;
        // si fue UPDATE, idGenerado viene 0 y usamos el ID del payload
        long idSolicitud = resSol.getIdGenerado() > 0 ? resSol.getIdGenerado() : payload.getIdSolicitud();

        // --- PASO 4: Insertar/actualizar cada proveedor y sus facturas ---
        if (payload.getProveedores() != null) {
            for (SolicitudProveedor prov : payload.getProveedores()) {
                // Vincular el proveedor a la solicitud (especialmente importante en INSERT)
                prov.setIdSolicitud(idSolicitud);
                // Heredar el usuario de auditoría si no fue especificado en el proveedor
                if (prov.getAudUsuario() == 0) prov.setAudUsuario(payload.getAudUsuario());

                String accionProv = prov.getIdSolicitudProveedor() == 0 ? "I" : "U";
                RespuestaSp resProv = solicitudProveedorDao.registrarSolicitudProveedor(prov, accionProv);
                ejecutar(resProv, "Error en proveedor " + prov.getCardCode());

                long idProveedor = resProv.getIdGenerado() > 0 ? resProv.getIdGenerado() : prov.getIdSolicitudProveedor();

                // --- PASO 5: Insertar/actualizar cada factura del proveedor ---
                if (prov.getDetalles() != null) {
                    for (DetalleSolicitud det : prov.getDetalles()) {
                        det.setIdSolicitudProveedor(idProveedor);
                        if (det.getAudUsuario() == 0) det.setAudUsuario(payload.getAudUsuario());
                        String accionDet = det.getIdDetalle() == 0 ? "I" : "U";
                        RespuestaSp resDet = detalleSolicitudDao.registrarDetalleSolicitud(det, accionDet);
                        ejecutar(resDet, "Error en factura " + det.getNumeroDocumento());
                    }
                }
            }
        }

        return respuestaCreada(idSolicitud);
    }

    /**
     * Aprueba una solicitud de pago cambiando su estado.
     * <p>
     * Carga la solicitud actual desde la BD, actualiza el estado y el usuario
     * de auditoría, y guarda vía SP con ACCION "U".
     * <p>
     * El SP internamente registra el cambio en LogEstados (PENDIENTE → nuevo estado).
     * <p>
     * <b>Nota:</b> Se carga la solicitud completa desde BD para asegurar que todos
     * los campos lleguen al SP, no solo los que vienen en el payload.
     *
     * @param payload Objeto con idSolicitud, estado destino y audUsuario
     * @return 201 Created con el ID de la solicitud
     */
    @PostMapping("/aprobar-solicitud")
    @Transactional
    public ResponseEntity<ApiResponse<?>> aprobarSolicitud(@RequestBody SolicitudPago payload) {
        // Cargar la solicitud completa desde BD para no perder campos al hacer UPDATE
        SolicitudPago actual = solicitudPagoDao.obtenerSolicitudPagoPorId(payload.getIdSolicitud());
        if (actual == null) throw new SpBusinessException("No se encontró la solicitud con ID: " + payload.getIdSolicitud());

        // Solo actualizar el estado y el usuario de auditoría
        actual.setEstado(payload.getEstado());
        actual.setAudUsuario(payload.getAudUsuario());

        // El SP p_abm_tpex_SolicitudPago con "U" actualiza y registra LogEstados internamente
        RespuestaSp res = solicitudPagoDao.registrarSolicitudPago(actual, "U");
        ejecutar(res, "Error cambiando estado de solicitud");

        return respuestaCreada(payload.getIdSolicitud());
    }

    // ==================== FASE 2: COTIZACIÓN BANCARIA ====================

    /**
     * Guarda una cotización bancaria completa con sus cargos asociados.
     * <p>
     * Una cotización representa la oferta de tipo de cambio de un banco para
     * una solicitud de pago específica. Incluye los cargos (comisiones, ITF, etc.)
     * que el banco aplica.
     * <p>
     * <b>Regla de cargos:</b> Los cargos solo se insertan cuando la cotización es nueva
     * (idCotizacion == 0). En UPDATE de cotización, los cargos no se modifican desde aquí.
     * <p>
     * <b>Exclusividad FK en cargos:</b> Cada cargo pertenece a una cotización O a una
     * transacción, nunca a ambas. Aquí se asigna idCotizacion y se fuerza idTransaccion = 0.
     *
     * @param payload Objeto Cotizaciones con cargos[] anidados
     * @return 201 Created con el ID de la cotización
     */
    @PostMapping("/guardar-cotizacion-completa")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardarCotizacionCompleta(@RequestBody Cotizaciones payload) {
        boolean esNueva = payload.getIdCotizacion() == 0;
        String accion = esNueva ? "I" : "U";

        // Guardar la cotización (cabecera)
        RespuestaSp res = cotizacionesDao.registrarCotizaciones(payload, accion);
        ejecutar(res, "Error registrando cotización");

        long idCotizacion = res.getIdGenerado() > 0 ? res.getIdGenerado() : payload.getIdCotizacion();

        // Solo insertar cargos si es una cotización nueva
        if (esNueva && payload.getCargos() != null) {
            int ordenAuto = 1;
            for (CargoPago cargo : payload.getCargos()) {
                cargo.setIdCotizacion(idCotizacion);   // Vincular a esta cotización
                cargo.setIdTransaccion(0L);             // Exclusividad: no pertenece a transacción
                cargo.setAudUsuario(payload.getAudUsuario());
                // Auto-numerar el orden si no fue especificado
                if (cargo.getOrden() == 0) cargo.setOrden(ordenAuto);
                ordenAuto++;

                RespuestaSp resCargo = cargoPagoDao.registrarCargoPago(cargo, "I");
                ejecutar(resCargo, "Error registrando cargo en cotización");
            }
        }

        return respuestaCreada(idCotizacion);
    }

    // ==================== FASE 3: ACEPTAR COTIZACIÓN GANADORA ====================

    /**
     * Acepta una cotización como la ganadora de una solicitud.
     * <p>
     * Al aceptar una cotización, el SP internamente:
     * <ul>
     *   <li>Marca esta cotización como {@code esGanadora = 1} y estado "ACEPTADA"</li>
     *   <li>Rechaza automáticamente las demás cotizaciones de la misma solicitud (estado "RECHAZADA")</li>
     *   <li>Registra los cambios de estado en LogEstados</li>
     * </ul>
     *
     * @param payload Objeto Cotizaciones con idCotizacion, estado "ACEPTADA" y audUsuario
     * @return 201 Created con el ID de la cotización
     */
    @PostMapping("/aceptar-cotizacion")
    @Transactional
    public ResponseEntity<ApiResponse<?>> aceptarCotizacion(@RequestBody Cotizaciones payload) {
        // El SP p_abm_tpex_Cotizaciones con "U" maneja toda la lógica:
        // marca ganadora, rechaza demás cotizaciones, registra logs
        RespuestaSp res = cotizacionesDao.registrarCotizaciones(payload, "U");
        ejecutar(res, "Error aceptando cotización");

        return respuestaCreada(payload.getIdCotizacion());
    }

    // ==================== FASE 4: TRANSACCIÓN DE PAGO ====================

    /**
     * Guarda una transacción bancaria completa con sus cargos asociados.
     * <p>
     * La transacción representa la ejecución real del pago a través del banco
     * cuya cotización fue aceptada. Contiene los datos definitivos: monto,
     * tipo de cambio aplicado, número de referencia bancaria, etc.
     * <p>
     * <b>Regla de cargos:</b> Similar a cotizaciones, los cargos solo se insertan
     * cuando la transacción es nueva (idTransaccion == 0).
     * <p>
     * <b>Exclusividad FK en cargos:</b> Aquí se asigna idTransaccion y se fuerza
     * idCotizacion = 0 (el cargo pertenece a la transacción, no a la cotización).
     *
     * @param payload Objeto Transacciones con cargos[] anidados
     * @return 201 Created con el ID de la transacción
     */
    @PostMapping("/guardar-transaccion-completa")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardarTransaccionCompleta(@RequestBody Transacciones payload) {
        String accion = payload.getIdTransaccion() == 0 ? "I" : "U";

        // Guardar la transacción (cabecera)
        RespuestaSp res = transaccionesDao.registrarTransacciones(payload, accion);
        ejecutar(res, "Error registrando transacción");

        long idTransaccion = res.getIdGenerado() > 0 ? res.getIdGenerado() : payload.getIdTransaccion();

        // Solo insertar cargos si es una transacción nueva
        if (payload.getIdTransaccion() == 0 && payload.getCargos() != null) {
            int ordenAuto = 1;
            for (CargoPago cargo : payload.getCargos()) {
                cargo.setIdTransaccion(idTransaccion);  // Vincular a esta transacción
                cargo.setIdCotizacion(0L);              // Exclusividad: no pertenece a cotización
                cargo.setAudUsuario(payload.getAudUsuario());
                if (cargo.getOrden() == 0) cargo.setOrden(ordenAuto);
                ordenAuto++;

                RespuestaSp resCargo = cargoPagoDao.registrarCargoPago(cargo, "I");
                ejecutar(resCargo, "Error registrando cargo en transacción");
            }
        }

        return respuestaCreada(idTransaccion);
    }

    /**
     * Cambia el estado de una transacción bancaria.
     * <p>
     * Carga la transacción actual desde BD, actualiza el estado y opcionalmente
     * el número de referencia bancaria y la fecha valor.
     * <p>
     * El SP internamente registra el cambio en LogEstados.
     * <p>
     * <b>Nota:</b> Se carga la transacción completa desde BD para no perder campos
     * al hacer UPDATE. Los campos numeroTransaccion y fechaValor solo se actualizan
     * si vienen con valor en el payload.
     *
     * @param payload Objeto con idTransaccion, estado destino, y opcionalmente numeroTransaccion y fechaValor
     * @return 201 Created con el ID de la transacción
     */
    @PostMapping("/cambiar-estado-transaccion")
    @Transactional
    public ResponseEntity<ApiResponse<?>> cambiarEstadoTransaccion(@RequestBody Transacciones payload) {
        // Cargar la transacción completa desde BD para no perder campos al hacer UPDATE
        Transacciones actual = transaccionesDao.obtenerTransaccionPorId(payload.getIdTransaccion(), payload.getCodEmpresa());
        if (actual == null) throw new SpBusinessException("No se encontró la transacción con ID: " + payload.getIdTransaccion());

        // Actualizar campos del payload
        actual.setEstado(payload.getEstado());
        actual.setAudUsuario(payload.getAudUsuario());
        // Solo actualizar estos campos si vienen con valor (son opcionales en este endpoint)
        if (payload.getNumeroTransaccion() != null) actual.setNumeroTransaccion(payload.getNumeroTransaccion());
        if (payload.getFechaValor() != null) actual.setFechaValor(payload.getFechaValor());

        RespuestaSp res = transaccionesDao.registrarTransacciones(actual, "U");
        ejecutar(res, "Error cambiando estado de transacción");

        return respuestaCreada(payload.getIdTransaccion());
    }

    // ==================== FASE 5: CONFIRMAR PAGO (Operación más crítica) ====================

    /**
     * Confirma el pago de una transacción y cierra la solicitud asociada.
     * <p>
     * Esta es la operación más crítica del flujo: en una sola transacción Java (ACID)
     * cierra DOS entidades:
     * <ol>
     *   <li>La transacción pasa a estado "CONFIRMADO"</li>
     *   <li>La solicitud de pago pasa a estado "PAGADA"</li>
     * </ol>
     * <p>
     * Si cualquier paso falla, se hace rollback de AMBAS actualizaciones, garantizando
     * que nunca quede una transacción confirmada con una solicitud sin pagar (o viceversa).
     * <p>
     * Usa el DTO {@link ConfirmarPagoRequest} que agrupa datos de ambas entidades
     * (Transacción + SolicitudPago) en un solo body.
     * <p>
     * El SP internamente registra los cambios en LogEstados:
     * (PROCESADO → CONFIRMADO) para la transacción y (APROBADA → PAGADA) para la solicitud.
     *
     * @param payload DTO con idTransaccion, idSolicitud, numeroTransaccion, fechaValor, audUsuario, codEmpresa, montoTotalSolicitud
     * @return 201 Created con el ID de la solicitud
     */
    @PostMapping("/confirmar-pago")
    @Transactional
    public ResponseEntity<ApiResponse<?>> confirmarPago(@RequestBody ConfirmarPagoRequest payload) {

        // --- PASO 1: Confirmar la transacción ---
        Transacciones trxActual = transaccionesDao.obtenerTransaccionPorId(payload.getIdTransaccion(), payload.getCodEmpresa());
        if (trxActual == null) throw new SpBusinessException("No se encontró la transacción con ID: " + payload.getIdTransaccion());

        trxActual.setEstado("CONFIRMADO");
        trxActual.setAudUsuario(payload.getAudUsuario());
        // Actualizar datos bancarios definitivos si vienen en el payload
        if (payload.getNumeroTransaccion() != null) trxActual.setNumeroTransaccion(payload.getNumeroTransaccion());
        if (payload.getFechaValor() != null) trxActual.setFechaValor(payload.getFechaValor());

        RespuestaSp resTrx = transaccionesDao.registrarTransacciones(trxActual, "U");
        ejecutar(resTrx, "Error confirmando transacción");

        // --- PASO 2: Cerrar la solicitud como PAGADA ---
        SolicitudPago solActual = solicitudPagoDao.obtenerSolicitudPagoPorId(payload.getIdSolicitud());
        if (solActual == null) throw new SpBusinessException("No se encontró la solicitud con ID: " + payload.getIdSolicitud());

        solActual.setEstado("PAGADA");
        solActual.setAudUsuario((int) payload.getAudUsuario());
        // Actualizar el monto total definitivo si viene en el payload
        if (payload.getMontoTotalSolicitud() > 0) solActual.setMontoTotalSolicitud(payload.getMontoTotalSolicitud());

        RespuestaSp resSol = solicitudPagoDao.registrarSolicitudPago(solActual, "U");
        ejecutar(resSol, "Error cerrando solicitud como PAGADA");

        return respuestaCreada(payload.getIdSolicitud());
    }

    // ==================== ENDPOINTS DE CONSULTA ====================
    // Todos son POST (convención del proyecto). No llevan @Transactional porque son solo lectura.
    // Usan el método auxiliar procesarLista() que retorna 200 con lista o 204 si está vacía.

    /**
     * Obtiene solicitudes de pago por ID.
     * Si idSolicitud == 0, retorna todas las solicitudes.
     *
     * @param mb Filtro con idSolicitud (0 = todas)
     * @return Lista de solicitudes de pago
     */
    @PostMapping("/obtener-solicitudes")
    public ResponseEntity<ApiResponse<?>> obtenerSolicitudes(@RequestBody SolicitudPago mb) {
        return procesarLista(solicitudPagoDao.obtenerSolicitudPago(mb.getIdSolicitud()), "No se encontraron solicitudes.");
    }

    /**
     * Obtiene la relación de un proveedor dentro de una solicitud por su ID.
     *
     * @param mb Filtro con idSolicitudProveedor
     * @return Lista de relaciones proveedor-solicitud
     */
    @PostMapping("/obtener-solicitud-proveedor")
    public ResponseEntity<ApiResponse<?>> obtenerSolicitudProveedor(@RequestBody SolicitudProveedor mb) {
        return procesarLista(solicitudProveedorDao.obtenerSolicitudProveedor(mb.getIdSolicitudProveedor()), "No se encontró el proveedor.");
    }

    /**
     * Obtiene el detalle (factura) de una solicitud por su ID.
     *
     * @param mb Filtro con idDetalle
     * @return Lista de detalles de solicitud
     */
    @PostMapping("/obtener-detalle-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerDetalleSolicitud(@RequestBody DetalleSolicitud mb) {
        return procesarLista(detalleSolicitudDao.obtenerDetalleSolicitud(mb.getIdDetalle()), "No se encontró el detalle.");
    }

    /**
     * Obtiene todos los proveedores SAP asociados a una empresa.
     * <p>
     * Usa el SP p_list_tpex_SolicitudProveedor con ACCION "B" (búsqueda por empresa).
     *
     * @param mb Filtro con codEmpresa
     * @return Lista de proveedores de la empresa
     */
    @PostMapping("/obtener-proveedores-empresa")
    public ResponseEntity<ApiResponse<?>> obtenerProveedoresXEmpresa(@RequestBody SolicitudProveedor mb) {
        return procesarLista(solicitudProveedorDao.obtenerProveedoresXEmpresa(mb.getCodEmpresa()), "No se encontraron proveedores.");
    }

    /**
     * Obtiene las facturas de proveedor y órdenes de compra disponibles para una empresa.
     * <p>
     * Usa el SP p_list_tpex_DetalleSolicitud con ACCION "B".
     * Retorna datos de SAP (DocNum, facturas, órdenes de compra) para alimentar
     * los dropdowns del frontend al crear solicitudes.
     *
     * @param mb Filtro con codEmpresa
     * @return Lista de documentos disponibles
     */
    @PostMapping("/obtener-docnum-empresa")
    public ResponseEntity<ApiResponse<?>> obtenerDocumentosPorEmpresa(@RequestBody DetalleSolicitud mb) {
        return procesarLista(detalleSolicitudDao.obtenerFacProvYOrdCompraXEmpresa(mb.getCodEmpresa()), "No se encontraron documentos.");
    }

    /**
     * Genera un reporte de solicitudes de pago filtrado por rango de fechas.
     * <p>
     * Usa el SP p_list_tpex_SolicitudPago con ACCION "B". El DAO internamente
     * reconstruye un árbol (Solicitud→Proveedor→Detalle) desde filas planas
     * usando ReportePlanoDto y LinkedHashMap.
     *
     * @param filtro DTO con fechaInicio y fechaFin
     * @return Lista de SolicitudPagoDto (estructura de árbol reconstruida)
     */
    @PostMapping("/reporte-solicitudes-fechas")
    public ResponseEntity<ApiResponse<?>> reporteSolicitudesXFecha(@RequestBody FiltroFechasDto filtro) {
        return procesarLista(solicitudPagoDao.reporteSolicitudesXFecha(filtro.getFechaInicio(), filtro.getFechaFin()), "No se encontraron solicitudes.");
    }

    /**
     * Obtiene todas las cotizaciones bancarias asociadas a una solicitud de pago.
     * <p>
     * Usa el SP p_list_tpex_Cotizaciones con ACCION "S" (por solicitud).
     *
     * @param mb Filtro con idSolicitud
     * @return Lista de cotizaciones de la solicitud
     */
    @PostMapping("/obtener-cotizaciones-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerCotizacionesPorSolicitud(@RequestBody Cotizaciones mb) {
        return procesarLista(cotizacionesDao.obtenerCotizacionesPorSolicitud(mb.getIdSolicitud()), "No se encontraron cotizaciones.");
    }

    /**
     * Obtiene transacciones bancarias con múltiples filtros opcionales.
     * <p>
     * Usa el SP p_list_tpex_Transacciones con ACCION "S" (por solicitud).
     * Permite filtrar por: idSolicitud, cardCode, codBanco, estado, idTipoTransaccion, codEmpresa.
     *
     * @param mb Filtros múltiples (los que sean 0/null se ignoran en el SP)
     * @return Lista de transacciones que cumplen los filtros
     */
    @PostMapping("/obtener-transacciones-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccionesPorSolicitud(@RequestBody Transacciones mb) {
        return procesarLista(transaccionesDao.obtenerTransacciones(mb.getIdSolicitud(), mb.getCardCode(), mb.getCodBanco(), mb.getEstado(), mb.getIdTipoTransaccion(), mb.getCodEmpresa()), "No se encontraron transacciones.");
    }

    /**
     * Obtiene las transacciones asociadas a una cotización específica.
     * <p>
     * Usa el SP p_list_tpex_Transacciones con ACCION "C" (por cotización).
     *
     * @param mb Filtro con idCotizacion
     * @return Lista de transacciones de la cotización
     */
    @PostMapping("/obtener-transacciones-cotizacion")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccionesPorCotizacion(@RequestBody Transacciones mb) {
        return procesarLista(transaccionesDao.obtenerTransaccionesPorCotizacion(mb.getIdCotizacion()), "No se encontraron transacciones.");
    }

    /**
     * Obtiene una transacción bancaria específica por su ID y empresa.
     * <p>
     * A diferencia de los otros endpoints de consulta, este retorna un objeto único
     * (no una lista), ya que se busca por clave primaria.
     *
     * @param mb Filtro con idTransaccion y codEmpresa
     * @return La transacción encontrada, o 204 si no existe
     */
    @PostMapping("/obtener-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccion(@RequestBody Transacciones mb) {
        Transacciones result = transaccionesDao.obtenerTransaccion(mb.getIdTransaccion(), mb.getCodEmpresa());
        if (result == null) return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ApiResponse<>("No se encontró la transacción.", null, HttpStatus.NO_CONTENT.value()));
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, result, HttpStatus.OK.value()));
    }

    /**
     * Genera un reporte de transacciones filtrado por rango de fechas y otros criterios.
     * <p>
     * Usa el SP p_list_tpex_Transacciones con ACCION "B" (rango/búsqueda).
     *
     * @param mb Filtros: fechaInicio, fechaFin, cardCode, estado, idTipoTransaccion, codEmpresa
     * @return Lista de transacciones que cumplen los filtros
     */
    @PostMapping("/reporte-transacciones-fechas")
    public ResponseEntity<ApiResponse<?>> reporteTransaccionesXFecha(@RequestBody Transacciones mb) {
        return procesarLista(transaccionesDao.obtenerTransaccionesReporte(mb.getFechaInicio(), mb.getFechaFin(), mb.getCardCode(), mb.getEstado(), mb.getIdTipoTransaccion(), mb.getCodEmpresa()), "No se encontraron transacciones.");
    }

    /**
     * Obtiene los cargos asociados a una cotización específica.
     * <p>
     * Usa el SP p_list_tpex_Cargos con ACCION "C" (por cotización).
     *
     * @param mb Filtro con idCotizacion
     * @return Lista de cargos de la cotización
     */
    @PostMapping("/obtener-cargos-cotizacion")
    public ResponseEntity<ApiResponse<?>> obtenerCargosPorCotizacion(@RequestBody CargoPago mb) {
        return procesarLista(cargoPagoDao.obtenerCargoPorCotizacion(mb.getIdCotizacion()), "No se encontraron cargos.");
    }

    /**
     * Obtiene los cargos asociados a una transacción específica.
     * <p>
     * Usa el SP p_list_tpex_Cargos con ACCION "T" (por transacción).
     *
     * @param mb Filtro con idTransaccion
     * @return Lista de cargos de la transacción
     */
    @PostMapping("/obtener-cargos-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerCargosPorTransaccion(@RequestBody CargoPago mb) {
        return procesarLista(cargoPagoDao.obtenerCargoPorTransaccion(mb.getIdTransaccion()), "No se encontraron cargos.");
    }

    /**
     * Obtiene el historial de cambios de estado de una solicitud.
     * <p>
     * Usa el SP p_list_tpex_LogEstados con ACCION "S" (por solicitud).
     *
     * @param mb Filtro con idSolicitud
     * @return Lista de registros de log para la solicitud
     */
    @PostMapping("/obtener-log-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerLogPorSolicitud(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerLogPorSolicitud(mb.getIdSolicitud()), "No se encontraron logs.");
    }

    /**
     * Obtiene el historial de cambios de estado de una transacción.
     * <p>
     * Usa el SP p_list_tpex_LogEstados con ACCION "T" (por transacción).
     *
     * @param mb Filtro con idTransaccion
     * @return Lista de registros de log para la transacción
     */
    @PostMapping("/obtener-log-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerLogPorTransaccion(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerLogPorTransaccion(mb.getIdTransaccion()), "No se encontraron logs.");
    }

    /**
     * Obtiene el timeline completo de eventos de una solicitud (incluye logs de cotizaciones y transacciones asociadas).
     * <p>
     * Usa el SP p_list_tpex_LogEstados con ACCION "B" (timeline completo).
     * A diferencia de obtenerLogPorSolicitud, este endpoint retorna todos los eventos
     * relacionados: solicitud + cotizaciones + transacciones, en orden cronológico.
     *
     * @param mb Filtro con idSolicitud
     * @return Lista de eventos del timeline completo
     */
    @PostMapping("/obtener-timeline-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerTimelineSolicitud(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerTimelineCompleto(mb.getIdSolicitud()), "No se encontraron eventos.");
    }

    // ==================== CATÁLOGOS (CRUD simple) ====================
    // Cada catálogo sigue el mismo patrón:
    //   - Registrar: idXxx == 0 → INSERT ("I"), idXxx > 0 → UPDATE ("U")
    //   - Eliminar: siempre DELETE ("D")
    //   - Obtener: por ID (0 = todos), algunos tienen endpoint extra por banco
    //
    // Estos endpoints NO llevan @Transactional porque cada operación es atómica
    // (un solo SP) y el manejador global de excepciones ya cubre los errores.

    /** Registra o actualiza un canal de pago. Si idCanal == 0 → INSERT, si > 0 → UPDATE. */
    @PostMapping("/registrar-canal-pago")
    public ResponseEntity<ApiResponse<?>> registrarCanalPago(@RequestBody CanalesPago mb) {
        return respuestaEscritura(canalesPagoDao.registrarCanalesPago(mb, mb.getIdCanal() == 0 ? "I" : "U"));
    }

    /** Elimina un canal de pago por su ID. */
    @PostMapping("/eliminar-canal-pago")
    public ResponseEntity<ApiResponse<?>> eliminarCanalPago(@RequestBody CanalesPago mb) {
        return respuestaEscritura(canalesPagoDao.registrarCanalesPago(mb, "D"));
    }

    /** Obtiene canales de pago. Si idCanal == 0 → retorna todos. */
    @PostMapping("/obtener-canales-pago")
    public ResponseEntity<ApiResponse<?>> obtenerCanalesPago(@RequestBody CanalesPago mb) {
        return procesarLista(canalesPagoDao.obtenerCanalesPago(mb.getIdCanal()), "No se encontraron canales.");
    }

    /** Registra o actualiza una moneda. Si idMoneda == 0 → INSERT, si > 0 → UPDATE. */
    @PostMapping("/registrar-moneda")
    public ResponseEntity<ApiResponse<?>> registrarMoneda(@RequestBody Monedas mb) {
        return respuestaEscritura(monedasDao.registrarMonedas(mb, mb.getIdMoneda() == 0 ? "I" : "U"));
    }

    /** Elimina una moneda por su ID. */
    @PostMapping("/eliminar-moneda")
    public ResponseEntity<ApiResponse<?>> eliminarMoneda(@RequestBody Monedas mb) {
        return respuestaEscritura(monedasDao.registrarMonedas(mb, "D"));
    }

    /** Obtiene monedas. Si idMoneda == 0 → retorna todas. */
    @PostMapping("/obtener-monedas")
    public ResponseEntity<ApiResponse<?>> obtenerMonedas(@RequestBody Monedas mb) {
        return procesarLista(monedasDao.obtenerMonedas(mb.getIdMoneda()), "No se encontraron monedas.");
    }

    /** Registra o actualiza un tipo de cambio. Si idTipoCambio == 0 → INSERT, si > 0 → UPDATE. */
    @PostMapping("/registrar-tipo-cambio")
    public ResponseEntity<ApiResponse<?>> registrarTipoCambio(@RequestBody TiposCambio mb) {
        return respuestaEscritura(tiposCambioDao.registrarTiposCambio(mb, mb.getIdTipoCambio() == 0 ? "I" : "U"));
    }

    /** Elimina un tipo de cambio por su ID. */
    @PostMapping("/eliminar-tipo-cambio")
    public ResponseEntity<ApiResponse<?>> eliminarTipoCambio(@RequestBody TiposCambio mb) {
        return respuestaEscritura(tiposCambioDao.registrarTiposCambio(mb, "D"));
    }

    /** Obtiene tipos de cambio. Si idTipoCambio == 0 → retorna todos. */
    @PostMapping("/obtener-tipos-cambio")
    public ResponseEntity<ApiResponse<?>> obtenerTiposCambio(@RequestBody TiposCambio mb) {
        return procesarLista(tiposCambioDao.obtenerTiposCambio(mb.getIdTipoCambio()), "No se encontraron tipos de cambio.");
    }

    /**
     * Obtiene tipos de cambio filtrados por banco.
     * <p>
     * Usa el SP p_list_tpex_TiposCambio con ACCION "R" (por banco).
     *
     * @param mb Filtro con codBanco
     * @return Lista de tipos de cambio del banco
     */
    @PostMapping("/obtener-tipos-cambio-banco")
    public ResponseEntity<ApiResponse<?>> obtenerTiposCambioPorBanco(@RequestBody TiposCambio mb) {
        return procesarLista(tiposCambioDao.obtenerTiposCambioPorBanco(mb.getCodBanco()), "No se encontraron tipos de cambio.");
    }

    /** Registra o actualiza un tipo de cargo. Si idTipoCargo == 0 → INSERT, si > 0 → UPDATE. */
    @PostMapping("/registrar-tipo-cargo")
    public ResponseEntity<ApiResponse<?>> registrarTipoCargo(@RequestBody TiposCargo mb) {
        return respuestaEscritura(tiposCargoDao.registrarTiposCargo(mb, mb.getIdTipoCargo() == 0 ? "I" : "U"));
    }

    /** Elimina un tipo de cargo por su ID. */
    @PostMapping("/eliminar-tipo-cargo")
    public ResponseEntity<ApiResponse<?>> eliminarTipoCargo(@RequestBody TiposCargo mb) {
        return respuestaEscritura(tiposCargoDao.registrarTiposCargo(mb, "D"));
    }

    /** Obtiene tipos de cargo. Si idTipoCargo == 0 → retorna todos. */
    @PostMapping("/obtener-tipos-cargo")
    public ResponseEntity<ApiResponse<?>> obtenerTiposCargo(@RequestBody TiposCargo mb) {
        return procesarLista(tiposCargoDao.obtenerTiposCargo(mb.getIdTipoCargo()), "No se encontraron tipos de cargo.");
    }

    /** Registra o actualiza un tipo de transacción. Si idTipoTransaccion == 0 → INSERT, si > 0 → UPDATE. */
    @PostMapping("/registrar-tipo-transaccion")
    public ResponseEntity<ApiResponse<?>> registrarTipoTransaccion(@RequestBody TiposTransaccion mb) {
        return respuestaEscritura(tiposTransaccionDao.registrarTiposTransaccion(mb, mb.getIdTipoTransaccion() == 0 ? "I" : "U"));
    }

    /** Elimina un tipo de transacción por su ID. */
    @PostMapping("/eliminar-tipo-transaccion")
    public ResponseEntity<ApiResponse<?>> eliminarTipoTransaccion(@RequestBody TiposTransaccion mb) {
        return respuestaEscritura(tiposTransaccionDao.registrarTiposTransaccion(mb, "D"));
    }

    /** Obtiene tipos de transacción. Si idTipoTransaccion == 0 → retorna todos. */
    @PostMapping("/obtener-tipos-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerTiposTransaccion(@RequestBody TiposTransaccion mb) {
        return procesarLista(tiposTransaccionDao.obtenerTiposTransaccion(mb.getIdTipoTransaccion()), "No se encontraron tipos de transacción.");
    }

    /** Registra o actualiza una configuración de comisiones. Si idConfig == 0 → INSERT, si > 0 → UPDATE. */
    @PostMapping("/registrar-config-comisiones")
    public ResponseEntity<ApiResponse<?>> registrarConfigComisiones(@RequestBody ConfigComisionesBanco mb) {
        return respuestaEscritura(configComisionesBancoDao.registrarConfigComisionesBanco(mb, mb.getIdConfig() == 0 ? "I" : "U"));
    }

    /** Elimina una configuración de comisiones por su ID. */
    @PostMapping("/eliminar-config-comisiones")
    public ResponseEntity<ApiResponse<?>> eliminarConfigComisiones(@RequestBody ConfigComisionesBanco mb) {
        return respuestaEscritura(configComisionesBancoDao.registrarConfigComisionesBanco(mb, "D"));
    }

    /** Obtiene configuraciones de comisiones. Si idConfig == 0 → retorna todas. */
    @PostMapping("/obtener-config-comisiones")
    public ResponseEntity<ApiResponse<?>> obtenerConfigComisiones(@RequestBody ConfigComisionesBanco mb) {
        return procesarLista(configComisionesBancoDao.obtenerConfigComisionesBanco(mb.getIdConfig()), "No se encontró la configuración.");
    }

    /**
     * Obtiene las configuraciones de comisiones de un banco específico.
     * <p>
     * Usa el SP p_list_tpex_ConfigComisionesBanco con ACCION "B" (por banco).
     *
     * @param mb Filtro con codBanco
     * @return Lista de configuraciones de comisiones del banco
     */
    @PostMapping("/obtener-config-comisiones-banco")
    public ResponseEntity<ApiResponse<?>> obtenerConfigComisionesPorBanco(@RequestBody ConfigComisionesBanco mb) {
        return procesarLista(configComisionesBancoDao.obtenerConfigPorBanco(mb.getCodBanco()), "No se encontraron configuraciones.");
    }

    // ==================== VOUCHERS (Comprobantes de pago) ====================

    /**
     * Sube un archivo de voucher (comprobante) para una transacción bancaria.
     * <p>
     * <b>Flujo:</b>
     * <ol>
     *   <li>Valida que el archivo no esté vacío y tenga extensión permitida (PDF, JPG, JPEG, PNG)</li>
     *   <li>Genera una ruta relativa única: {@code pagos-extranjeros/vouchers/{idTransaccion}_{timestamp}.{ext}}</li>
     *   <li>Guarda el archivo en el filesystem vía {@link FileStorageService}</li>
     *   <li>Actualiza la ruta en la BD vía SP p_abm_tpex_Transacciones</li>
     *   <li>Si el SP falla, elimina el archivo del filesystem (compensación manual)</li>
     * </ol>
     * <p>
     * <b>Nota sobre compensación:</b> Como el guardado de archivo y la actualización de BD
     * no son atómicos (filesystem + SQL Server), si el SP falla después de guardar el archivo,
     * se elimina el archivo manualmente para mantener consistencia.
     *
     * @param idTransaccion ID de la transacción a la que se asocia el voucher
     * @param file          Archivo multipart (PDF, JPG, JPEG o PNG)
     * @param audUsuario    ID del usuario que realiza la operación
     * @return 201 Created con la ruta relativa del archivo guardado
     */
    @PostMapping(value = "/transacciones/{idTransaccion}/voucher", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> subirVoucher(@PathVariable long idTransaccion, @RequestParam("file") MultipartFile file, @RequestParam("audUsuario") int audUsuario) {

        // Validar que el archivo no esté vacío
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("El archivo no puede estar vacío.", null, HttpStatus.BAD_REQUEST.value()));
        }

        // Validar extensión del archivo
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "Nombre de archivo no disponible");
        String ext = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!EXTENSIONES_VOUCHER.contains(ext)) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("Extensión no permitida. Use PDF, JPG, JPEG o PNG.", null, HttpStatus.BAD_REQUEST.value()));
        }

        // Generar ruta relativa única (timestamp evita colisiones)
        String rutaRelativa = "pagos-extranjeros/vouchers/" + idTransaccion + "_" + System.currentTimeMillis() + "." + ext;

        try {
            // PASO 1: Guardar archivo en filesystem
            fileStorageService.guardarVoucher(file, rutaRelativa);

            try {
                // PASO 2: Actualizar la ruta en la BD
                transaccionesDao.actualizarVoucher(idTransaccion, rutaRelativa, audUsuario);
            } catch (SpBusinessException spEx) {
                // COMPENSACIÓN: Si el SP falló, eliminar el archivo que ya se guardó
                fileStorageService.eliminarVoucher(rutaRelativa);
                log.warn("Voucher eliminado por error en SP. Transacción ID={}: {}", idTransaccion, spEx.getMessage());
                throw spEx;
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(SUCCESS_MESSAGE, rutaRelativa, HttpStatus.CREATED.value()));

        } catch (SpBusinessException ex) {
            // Re-lanzar la excepción de negocio para que la maneje el GlobalExceptionHandler
            throw ex;
        } catch (IOException e) {
            log.error("Error guardando voucher para transacción ID={}: {}", idTransaccion, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>("Error al guardar el archivo.", null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Descarga el voucher (comprobante) asociado a una transacción bancaria.
     * <p>
     * <b>Flujo:</b>
     * <ol>
     *   <li>Busca la transacción y verifica que tenga voucher registrado</li>
     *   <li>Carga el archivo desde el filesystem vía {@link FileStorageService}</li>
     *   <li>Determina el Content-Type según la extensión del archivo</li>
     *   <li>Retorna el archivo para visualización inline (no descarga)</li>
     * </ol>
     * <p>
     * <b>Nota:</b> Este es el único endpoint GET del controlador. Retorna el archivo
     * directamente (no un ApiResponse) para que el navegador pueda mostrarlo inline.
     *
     * @param idTransaccion ID de la transacción cuyo voucher se desea obtener
     * @param codEmpresa    Código de empresa (opcional, default 0) para filtrar la búsqueda
     * @return El archivo del voucher con Content-Type apropiado, o 404 si no existe
     */
    @GetMapping("/transacciones/{idTransaccion}/voucher")
    public ResponseEntity<?> obtenerVoucher(@PathVariable long idTransaccion, @RequestParam(value = "codEmpresa", required = false, defaultValue = "0") long codEmpresa) {

        // Buscar la transacción y verificar que tenga voucher
        Transacciones trx = transaccionesDao.obtenerTransaccionPorId(idTransaccion, codEmpresa);
        if (trx == null || trx.getRutaVoucher() == null || Boolean.FALSE.equals(trx.getTieneVoucher())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>("La transacción no tiene voucher registrado.", null, HttpStatus.NOT_FOUND.value()));
        }

        try {
            // Cargar el archivo desde el filesystem
            Resource resource = fileStorageService.obtenerVoucher(trx.getRutaVoucher());

            // Determinar Content-Type según la extensión del archivo
            String rutaVoucher = trx.getRutaVoucher();
            String ext = rutaVoucher.contains(".") ? rutaVoucher.substring(rutaVoucher.lastIndexOf('.') + 1).toLowerCase() : "";
            String contentType;
            switch (ext) {
                case "pdf": contentType = "application/pdf"; break;
                case "jpg":
                case "jpeg": contentType = "image/jpeg"; break;
                case "png": contentType = "image/png"; break;
                default: contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            // Retornar archivo inline (se muestra en el navegador, no se descarga)
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).contentLength(resource.contentLength())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"").body(resource);

        } catch (IOException e) {
            log.error("Error leyendo voucher para transacción ID={}: {}", idTransaccion, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>("Error al leer el archivo.", null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    // ==================== ASIENTOS CONTABLES ====================

    /**
     * Registra o actualiza un asiento contable de una transacción.
     * Si idAsiento == 0 → INSERT ("I"), si > 0 → UPDATE ("U").
     *
     * @param payload Objeto Asientos con los datos del asiento
     * @return 201 Created con el ID del asiento
     */
    @PostMapping("/registrar-asiento")
    @Transactional
    public ResponseEntity<ApiResponse<?>> registrarAsiento(@RequestBody Asientos payload) {
        String accion = payload.getIdAsiento() == 0 ? "I" : "U";
        RespuestaSp res = asientosDao.registrarAsiento(payload, accion);
        ejecutar(res, "Error registrando asiento");
        long id = res.getIdGenerado() > 0 ? res.getIdGenerado() : payload.getIdAsiento();
        return respuestaCreada(id);
    }

    /**
     * Elimina un asiento contable por su ID.
     *
     * @param payload Objeto con idAsiento y audUsuario
     * @return 201 Created con el ID eliminado
     */
    @PostMapping("/eliminar-asiento")
    @Transactional
    public ResponseEntity<ApiResponse<?>> eliminarAsiento(@RequestBody Asientos payload) {
        RespuestaSp res = asientosDao.registrarAsiento(payload, "D");
        ejecutar(res, "Error eliminando asiento");
        return respuestaCreada(payload.getIdAsiento());
    }

    /**
     * Obtiene todos los asientos contables de una transacción.
     * Usa p_list_tpex_Asientos ACCION="T".
     *
     * @param mb Filtro con idTransaccion
     * @return Lista de asientos de la transacción
     */
    @PostMapping("/obtener-asientos-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerAsientosPorTransaccion(@RequestBody Asientos mb) {
        return procesarLista(asientosDao.obtenerAsientosPorTransaccion(mb.getIdTransaccion()), "No se encontraron asientos.");
    }

    /**
     * Valida el cuadre de los asientos de una transacción.
     * Retorna un resumen con totales y estadoCuadre ("CUADRADO"/"DESCUADRADO").
     * Usa p_list_tpex_Asientos ACCION="V".
     *
     * @param mb Filtro con idTransaccion
     * @return Resumen de cuadre o 204 si no hay asientos
     */
    @PostMapping("/validar-cuadre-asientos")
    public ResponseEntity<ApiResponse<?>> validarCuadreAsientos(@RequestBody Asientos mb) {
        Asientos result = asientosDao.validarCuadre(mb.getIdTransaccion());
        if (result == null) return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(new ApiResponse<>("No se encontraron asientos para cuadrar.", null, HttpStatus.NO_CONTENT.value()));
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, result, HttpStatus.OK.value()));
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Verifica que un SP se ejecutó sin errores. Si hay error, lanza excepción.
     * <p>
     * Este método es el mecanismo central de manejo de errores dentro de las
     * transacciones. Cuando se llama dentro de un método @Transactional,
     * la excepción lanzada provoca rollback automático de toda la operación.
     * <p>
     * La excepción SpBusinessException es capturada por el GlobalExceptionHandler
     * y convertida en una respuesta HTTP 400 con el mensaje de error del SP.
     *
     * @param res       Resultado del SP (contiene error, errormsg, idGenerado)
     * @param contexto  Descripción de la operación (para enriquecer el mensaje de error)
     * @throws SpBusinessException si res.getError() != 0
     */
    private void ejecutar(RespuestaSp res, String contexto) {
        if (res.getError() != 0) throw new SpBusinessException(contexto + ": " + res.getErrormsg());
    }

    /**
     * Construye una respuesta HTTP 201 Created estándar para operaciones de escritura exitosas.
     * <p>
     * Usado por los endpoints transaccionales (guardar, aprobar, confirmar, etc.)
     * que retornan el ID generado o actualizado.
     *
     * @param id ID generado o actualizado por la operación
     * @return ResponseEntity 201 con ApiResponse conteniendo el ID
     */
    private ResponseEntity<ApiResponse<?>> respuestaCreada(long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(SUCCESS_MESSAGE, id, HttpStatus.CREATED.value()));
    }

    /**
     * Construye la respuesta para operaciones de escritura de catálogos (CRUD simple).
     * <p>
     * A diferencia de respuestaCreada(), este método usa directamente el RespuestaSp
     * del SP, lo que permite retornar el mensaje específico del SP (puede ser un error
     * o un mensaje de éxito personalizado).
     * <p>
     * Si el SP retorna error == 0 → 201 Created con el ID generado.
     * Si el SP retorna error != 0 → 400 Bad Request con el mensaje de error.
     *
     * @param res Resultado del SP
     * @return ResponseEntity 201 o 400 según el resultado del SP
     */
    private ResponseEntity<ApiResponse<?>> respuestaEscritura(RespuestaSp res) {
        HttpStatus status = res.getError() == 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), status.value()));
    }

    /**
     * Procesa una lista de resultados para endpoints de consulta.
     * <p>
     * Si la lista tiene elementos → 200 OK con la lista.
     * Si la lista está vacía o es null → 204 No Content con mensaje informativo.
     * <p>
     * Este método centraliza la lógica de respuesta para todos los endpoints
     * de consulta, garantizando consistencia en el formato de respuesta.
     *
     * @param lista          Lista de resultados del SP
     * @param mensajeVacio   Mensaje a mostrar si la lista está vacía
     * @param <T>            Tipo de los elementos de la lista
     * @return ResponseEntity 200 con lista o 204 si está vacía
     */
    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, String mensajeVacio) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}