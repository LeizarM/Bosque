package bo.bosque.com.impexpap.controller;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IAccionCbr;
import bo.bosque.com.impexpap.dao.ICbrDetalle;
import bo.bosque.com.impexpap.dao.IGarantiaCbr;
import bo.bosque.com.impexpap.dto.BusquedaGarantiaRptDto;
import bo.bosque.com.impexpap.dto.EntregaGarantiaRptDto;
import bo.bosque.com.impexpap.dto.ExtensionGarantiaDto;
import bo.bosque.com.impexpap.dto.FiltroGarantiaDto;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.dto.GarantiaDto;
import bo.bosque.com.impexpap.dto.GarantiaRegistroDto;
import bo.bosque.com.impexpap.dto.ReciboGarantiaRptDto;
import bo.bosque.com.impexpap.model.AccionCbr;
import bo.bosque.com.impexpap.model.CbrDetalle;
import bo.bosque.com.impexpap.model.GarantiaCbr;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.Tipos;
import lombok.extern.slf4j.Slf4j;

/**
 * Modulo de Garantias de cobranza (tablas {@code tcbr_}).
 *
 * <p>Reemplaza la pantalla {@code tcbrGarantia/garantia.xhtml} de Bosque v2 (782 lineas, 13
 * dialogos, ManagedBean {@code WizardGarantiaCobranza}). El contrato completo esta en
 * {@code API_GARANTIAS.md} del espacio de migracion; el comportamiento de los procedimientos,
 * en {@code sql/tcbr_00_RUNBOOK.md}.
 *
 * <h2>Reglas que el JSF aplicaba en pantalla y aqui valida el servidor</h2>
 * <ul>
 *   <li><b>Permiso por boton</b> ({@code tb_vistaBtn} de la vista 45) en toda escritura, en
 *       los PDF y en el detalle de un cliente, con {@link AccesoModuloHelper#exigirBoton}. Son
 *       los mismos nombres que evaluaba {@code WizardGarantiaCobranza.esAutorizado(...)}.</li>
 *   <li><b>Una garantia CERRADA no se modifica.</b> El legacy ocultaba todos los botones con
 *       {@code esAutorizado(boton, datoEstado)} cuando el estado era CERRADO.</li>
 *   <li><b>Alta:</b> monto de garantia o de credito mayor a cero (validacion de
 *       {@code saveRegistro}).</li>
 * </ul>
 *
 * <h2>Lo que cambia respecto del legacy</h2>
 * <ul>
 *   <li>{@code montoGarantiaCalc} lo calcula el servidor (suma de los detalles) cada vez que
 *       cambian los detalles. En el legacy lo calculaba la pantalla y quedaba viejo cuando se
 *       agregaban detalles desde "Editar".</li>
 *   <li>El alta de la garantia y sus detalles va en una sola transaccion. El legacy grababa
 *       la garantia y despues cada detalle suelto.</li>
 *   <li>Las acciones REG y TRASP no se borran desde aqui: sin ellas la garantia deja de
 *       aparecer en los listados del legacy y la regla de traspaso se rompe.</li>
 *   <li>Los listados no repiten garantias por empresa SAP (ramas S, R y G de tcbr_04).</li>
 * </ul>
 *
 * <p>El usuario de auditoria sale siempre del token ({@link DatosToken}), nunca del cuerpo.
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/garantias")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class GarantiaController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    /** {@code tb_vista.codVista} de "Garantias" ({@code tcbrGarantia/garantia}, padre 44 "Cobranza"). */
    private static final int VISTA_GARANTIAS = 45;

    /** Nombres de {@code tb_vistaBtn.nombreBtn} de la vista 45, tal como existen hoy. */
    private static final String BTN_NUEVA = "btnNewGarCbr";
    private static final String BTN_EDITAR = "btnEditGarCbr";
    private static final String BTN_EDITAR_ADM = "btnEditAdmGarCbr";
    private static final String BTN_TRASPASO = "btnTraspGarCbr";
    private static final String BTN_REPORTE = "btnRptGaCbr";
    private static final String BTN_RECIBO = "btnRptUltGarCbr";
    private static final String BTN_VER_CLIENTE = "btnSCGarCbr";
    private static final String BTN_NUEVA_ACCION = "btnNewAcCbr";
    private static final String BTN_EDITAR_ACCION = "btnEditAcCbr";
    private static final String BTN_ELIMINAR_ACCION = "btnDelAcCbr";
    private static final String BTN_EXTENSION = "btnExtAcCbr";

    private static final List<String> ESTADOS = Arrays.asList("VIGENTE", "CADUCADO", "CERRADO");

    /** Unicos estados de accion que se registran a mano (Tipos.listBasicAccCbr del legacy). */
    private static final List<String> ESTADOS_ACCION_MANUAL = Arrays.asList("NOT", "CER");

    private final IGarantiaCbr garantiaDao;
    private final ICbrDetalle detalleDao;
    private final IAccionCbr accionDao;
    private final AccesoModuloHelper acceso;
    private final JasperReportExport jasper;

    public GarantiaController(IGarantiaCbr garantiaDao,
                              ICbrDetalle detalleDao,
                              IAccionCbr accionDao,
                              AccesoModuloHelper acceso,
                              JasperReportExport jasper) {
        this.garantiaDao = garantiaDao;
        this.detalleDao = detalleDao;
        this.accionDao = accionDao;
        this.acceso = acceso;
        this.jasper = jasper;
    }

    // ===================================================================== //
    //                              LECTURAS                                 //
    // ===================================================================== //

    /** Pantalla principal: una fila por cliente con garantias. */
    @PostMapping("/resumen-clientes")
    public ResponseEntity<ApiResponse<?>> resumenClientes(@RequestBody(required = false) FiltroGarantiaDto filtro) {
        String buscar = filtro != null ? filtro.getBuscar() : null;
        return procesarLista(garantiaDao.resumenPorCliente(buscar), "No hay garantías registradas.");
    }

    /** Garantias con estado y datos calculados, filtradas. */
    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@RequestBody(required = false) FiltroGarantiaDto filtro,
                                                 Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_VER_CLIENTE);
        validarFiltro(filtro);
        return procesarLista(garantiaDao.listar(filtro), "No hay garantías para el filtro indicado.");
    }

    /** Una garantia con sus datos calculados. */
    @PostMapping("/obtener")
    public ResponseEntity<ApiResponse<?>> obtener(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_VER_CLIENTE);
        GarantiaDto g = garantiaExistente(filtro.getId());
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, g, HttpStatus.OK.value()));
    }

    /** Buscador de clientes SAP para el alta (minimo 3 caracteres, maximo 50 resultados). */
    @PostMapping("/clientes-sap")
    public ResponseEntity<ApiResponse<?>> clientesSap(@RequestBody FiltroGarantiaDto filtro) {
        String buscar = filtro != null && filtro.getBuscar() != null ? filtro.getBuscar().trim() : "";
        if (buscar.length() < 3) {
            throw new SpBusinessException("Escriba al menos 3 caracteres del código o nombre del cliente.");
        }
        return procesarLista(garantiaDao.buscarClientesSap(buscar), "No se encontraron clientes en SAP.");
    }

    /** Detalles de una garantia. */
    @PostMapping("/detalles")
    public ResponseEntity<ApiResponse<?>> detalles(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_VER_CLIENTE);
        return procesarLista(detalleDao.listarPorGarantia(filtro.getId()), "La garantía no tiene detalles.");
    }

    /** Acciones (historia) de una garantia. */
    @PostMapping("/acciones")
    public ResponseEntity<ApiResponse<?>> acciones(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_VER_CLIENTE);
        return procesarLista(accionDao.listarPorGarantia(filtro.getId()), "La garantía no tiene acciones.");
    }

    /** Cantidad de garantias que esperan traspaso. */
    @PostMapping("/traspasos-pendientes")
    public ResponseEntity<ApiResponse<?>> traspasosPendientes() {
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, accionDao.traspasosPendientes(),
                HttpStatus.OK.value()));
    }

    /** Catalogo de tipos de garantia (v_tipos grupo 28). */
    @PostMapping("/tipos-garantia")
    public ResponseEntity<ApiResponse<?>> tiposGarantia() {
        return procesarLista(new Tipos().lstTipoGarantiaCbr(), "No hay tipos de garantía configurados.");
    }

    /** Catalogo de estados de accion (v_tipos grupo 29), para etiquetas. */
    @PostMapping("/estados-accion")
    public ResponseEntity<ApiResponse<?>> estadosAccion() {
        return procesarLista(new Tipos().lstEstadoAccionCbr(), "No hay estados de acción configurados.");
    }

    // ===================================================================== //
    //                             ESCRITURAS                                //
    // ===================================================================== //

    /**
     * Alta (codGarantia 0/null) o edicion de firmas, protesta y detalles nuevos
     * (codGarantia &gt; 0). Ver {@link GarantiaRegistroDto}.
     */
    @PostMapping("/registrar")
    @Transactional
    public ResponseEntity<ApiResponse<?>> registrar(@RequestBody GarantiaRegistroDto dto, Authentication auth) {
        int usuario = DatosToken.codUsuarioDe(auth);
        List<CbrDetalle> nuevos = dto.getDetalles() != null ? dto.getDetalles() : new ArrayList<>();
        validarDetallesNuevos(nuevos);

        long codGarantia;
        if (dto.esAlta()) {
            acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_NUEVA);
            validarMontosAlta(dto);

            dto.setMontoGarantiaCalc(sumaMontos(nuevos));
            dto.setAudUsuario((long) usuario);
            codGarantia = garantiaDao.alta(dto, dto.getObservacion()).getIdGenerado();
        } else {
            acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_EDITAR);
            GarantiaDto actual = garantiaModificable(dto.getCodGarantia());
            codGarantia = actual.getCodGarantia();
            garantiaDao.actualizarFirmas(codGarantia, dto.getRecFirmas(), dto.getNroProtesta(), usuario);
        }

        for (CbrDetalle d : nuevos) {
            d.setCodDetalle(null);
            d.setCodGarantia(codGarantia);
            d.setAudUsuario(usuario);
            detalleDao.registrar(d, "I");
        }
        if (!dto.esAlta() && !nuevos.isEmpty()) {
            // Se relee: la rama 'B' acaba de cambiar firmas y protesta, y el recalculo usa la
            // rama 'U', que reescribe todas las columnas con lo que se le pase.
            recalcularMontoCalc(garantiaExistente(codGarantia), usuario);
        }

        return creado(dto.esAlta() ? "Garantía registrada." : "Garantía actualizada.", codGarantia);
    }

    /**
     * Edicion administrativa: reemplaza todas las columnas editables menos el cliente. El
     * cuerpo tiene que venir completo (un null borra recFirmas o nroProtesta).
     */
    @PostMapping("/actualizar")
    @Transactional
    public ResponseEntity<ApiResponse<?>> actualizar(@RequestBody GarantiaCbr garantia, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_EDITAR_ADM);
        GarantiaDto actual = garantiaModificable(garantia.getCodGarantia());

        garantia.setCodGarantia(actual.getCodGarantia());
        garantia.setMontoGarantiaCalc(sumaMontos(detalleDao.listarPorGarantia(actual.getCodGarantia())));
        garantia.setAudUsuario((long) DatosToken.codUsuarioDe(auth));
        garantiaDao.actualizar(garantia);

        return creado("Garantía actualizada.", actual.getCodGarantia());
    }

    /** Extension: accion EXT + nueva fecha de expiracion, en una transaccion. */
    @PostMapping("/extension")
    @Transactional
    public ResponseEntity<ApiResponse<?>> extension(@RequestBody ExtensionGarantiaDto dto, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_EXTENSION);
        GarantiaDto actual = garantiaModificable(dto.getCodGarantia());
        int usuario = DatosToken.codUsuarioDe(auth);

        if (dto.getFechaExpiracion() == null) {
            throw new SpBusinessException("Debe indicar la nueva fecha de expiración.");
        }
        if (dia(dto.getFechaExpiracion()).compareTo(dia(actual.getFechaExpiracion())) <= 0) {
            throw new SpBusinessException("La nueva fecha de expiración debe ser posterior a la actual ("
                    + dia(actual.getFechaExpiracion()) + ").");
        }

        AccionCbr ext = new AccionCbr();
        ext.setCodGarantia(actual.getCodGarantia());
        ext.setFecha(dto.getFecha());
        ext.setEstado("EXT");
        ext.setObservacion(dto.getObservacion());
        ext.setAudUsuario(usuario);
        accionDao.registrar(ext, "I");   // el SP exige el traspaso previo

        actual.setFechaExpiracion(dto.getFechaExpiracion());
        actual.setAudUsuario((long) usuario);
        garantiaDao.actualizar(actual);

        return creado("Extensión registrada.", actual.getCodGarantia());
    }

    /** Alta de un detalle, o cambio de su monto (lo unico editable). Recalcula el monto calculado. */
    @PostMapping("/detalle/registrar")
    @Transactional
    public ResponseEntity<ApiResponse<?>> registrarDetalle(@RequestBody CbrDetalle detalle, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_EDITAR);
        int usuario = DatosToken.codUsuarioDe(auth);
        boolean esAlta = detalle.getCodDetalle() == null || detalle.getCodDetalle() == 0L;

        long codGarantia;
        if (esAlta) {
            codGarantia = detalle.getCodGarantia() != null ? detalle.getCodGarantia() : 0L;
            validarDetallesNuevos(java.util.Collections.singletonList(detalle));
        } else {
            CbrDetalle existente = detalleDao.obtener(detalle.getCodDetalle());
            if (existente == null) throw new SpBusinessException("El detalle indicado no existe.");
            codGarantia = existente.getCodGarantia();
            validarMonto(detalle.getMontoGarantiaParc());
        }
        GarantiaDto garantia = garantiaModificable(codGarantia);

        detalle.setCodGarantia(codGarantia);
        detalle.setAudUsuario(usuario);
        RespuestaSp res = detalleDao.registrar(detalle, esAlta ? "I" : "U");
        recalcularMontoCalc(garantia, usuario);

        return creado(esAlta ? "Detalle agregado." : "Detalle actualizado.", res.getIdGenerado());
    }

    /** Baja de un detalle. Recalcula el monto calculado. */
    @PostMapping("/detalle/eliminar")
    @Transactional
    public ResponseEntity<ApiResponse<?>> eliminarDetalle(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_EDITAR);
        int usuario = DatosToken.codUsuarioDe(auth);
        CbrDetalle existente = detalleDao.obtener(filtro.getId());
        if (existente == null) throw new SpBusinessException("El detalle indicado no existe.");
        GarantiaDto garantia = garantiaModificable(existente.getCodGarantia());

        detalleDao.eliminar(filtro.getId(), usuario);
        recalcularMontoCalc(garantia, usuario);

        return creado("Detalle eliminado.", filtro.getId());
    }

    /**
     * Alta de una accion (solo NOTA o CERRADO) o cambio de su observacion. Todo lo que no
     * sea NOTA exige el traspaso previo: lo valida {@code p_abm_AccionCbr}.
     */
    @PostMapping("/accion/registrar")
    public ResponseEntity<ApiResponse<?>> registrarAccion(@RequestBody AccionCbr accion, Authentication auth) {
        boolean esAlta = accion.getCodAccion() == null || accion.getCodAccion() == 0L;

        long codGarantia;
        if (esAlta) {
            acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_NUEVA_ACCION);
            if (accion.getEstado() == null || !ESTADOS_ACCION_MANUAL.contains(accion.getEstado().trim())) {
                throw new SpBusinessException("A mano solo se registran notas (NOT) o el cierre (CER). "
                        + "La extensión tiene su propia opción.");
            }
            codGarantia = accion.getCodGarantia() != null ? accion.getCodGarantia() : 0L;
        } else {
            acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_EDITAR_ACCION);
            AccionCbr existente = accionDao.obtener(accion.getCodAccion());
            if (existente == null) throw new SpBusinessException("La acción indicada no existe.");
            codGarantia = existente.getCodGarantia();
        }
        garantiaModificable(codGarantia);

        accion.setCodGarantia(codGarantia);
        accion.setAudUsuario(DatosToken.codUsuarioDe(auth));
        RespuestaSp res = accionDao.registrar(accion, esAlta ? "I" : "U");

        return creado(esAlta ? "Acción registrada." : "Acción actualizada.", res.getIdGenerado());
    }

    /** Baja de una accion. REG y TRASP no se borran. */
    @PostMapping("/accion/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarAccion(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_ELIMINAR_ACCION);
        AccionCbr existente = accionDao.obtener(filtro.getId());
        if (existente == null) throw new SpBusinessException("La acción indicada no existe.");
        if ("REG".equals(existente.getEstado()) || "TRASP".equals(existente.getEstado())) {
            throw new SpBusinessException("Las acciones de registro (REG) y traspaso (TRASP) no se eliminan.");
        }
        garantiaModificable(existente.getCodGarantia());

        accionDao.eliminar(filtro.getId(), DatosToken.codUsuarioDe(auth));
        return creado("Acción eliminada.", filtro.getId());
    }

    /** Traspasa a custodia todas las garantias pendientes. {@code data} = cantidad. */
    @PostMapping("/traspaso")
    public ResponseEntity<ApiResponse<?>> traspaso(Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_TRASPASO);
        long cantidad = accionDao.traspasar(DatosToken.codUsuarioDe(auth)).getIdGenerado();
        String msg = cantidad == 0
                ? "No había garantías pendientes de traspaso."
                : "Se traspasaron " + cantidad + " garantías.";
        return creado(msg, cantidad);
    }

    // ===================================================================== //
    //                              REPORTES                                 //
    // ===================================================================== //

    /** Recibo de una garantia con sus detalles (RptCobrRec + subRptCbrzDetalle). */
    @PostMapping("/reporte/recibo")
    public ResponseEntity<byte[]> reporteRecibo(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_RECIBO);
        List<ReciboGarantiaRptDto> filas = garantiaDao.recibo(filtro.getId());
        if (filas.isEmpty()) {
            throw new SpBusinessException("La garantía no existe o no tiene su registro de recepción.");
        }
        for (ReciboGarantiaRptDto f : filas) {
            f.setDetalles(detalleDao.listarParaRecibo(f.getCodGarantia()));
        }
        byte[] pdf = jasper.exportPDFDesdeColeccionConSubreportes(
                "RptCobrRec", filas, parametrosReporte(), "subRptCbrzDetalle");
        return pdf(pdf);
    }

    /** Nomina del ultimo traspaso (RptCbrEntregaGarantias). */
    @PostMapping("/reporte/traspaso")
    public ResponseEntity<byte[]> reporteTraspaso(Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_TRASPASO);
        List<EntregaGarantiaRptDto> filas = garantiaDao.ultimoTraspaso();
        if (filas.isEmpty()) {
            throw new SpBusinessException("No hay traspasos registrados.");
        }
        return pdf(jasper.exportPDFDesdeColeccion("RptCbrEntregaGarantias", filas, parametrosReporte()));
    }

    /** Listado filtrado (RptCbrGarantia). Sale de la rama 'G', sin garantias repetidas. */
    @PostMapping("/reporte/busqueda")
    public ResponseEntity<byte[]> reporteBusqueda(@RequestBody(required = false) FiltroGarantiaDto filtro,
                                                  Authentication auth) {
        acceso.exigirBoton(auth, VISTA_GARANTIAS, BTN_REPORTE);
        validarFiltro(filtro);
        List<GarantiaDto> garantias = garantiaDao.listar(filtro);
        if (garantias.isEmpty()) {
            throw new SpBusinessException("No hay garantías para el filtro indicado.");
        }

        List<BusquedaGarantiaRptDto> filas = new ArrayList<>(garantias.size());
        int n = 0;
        for (GarantiaDto g : garantias) {
            filas.add(new BusquedaGarantiaRptDto(
                    BigDecimal.valueOf(++n),
                    g.getDatoCliente() + " - " + g.getCodClienteSAP(),
                    doble(g.getMontoGarantia()),
                    doble(g.getMontoCredito()),
                    (g.getTiempoPago() != null ? g.getTiempoPago() : 0) + " Dias",
                    sqlDate(g.getFechaInicio()),
                    sqlDate(g.getFechaExpiracion()),
                    g.getTiposGarantia() != null ? g.getTiposGarantia() : " ",
                    g.getRealizoEmp(),
                    sqlDate(g.getFechaRegistro()),
                    g.getObservacionRegistro(),
                    g.getDatoEstado()));
        }
        return pdf(jasper.exportPDFDesdeColeccion("RptCbrGarantia", filas, parametrosReporte()));
    }

    // ===================================================================== //
    //                              APOYO                                    //
    // ===================================================================== //

    private GarantiaDto garantiaExistente(Long codGarantia) {
        GarantiaDto g = codGarantia != null ? garantiaDao.obtener(codGarantia) : null;
        if (g == null) throw new SpBusinessException("La garantía indicada no existe.");
        return g;
    }

    /** La garantia existe y no esta CERRADA (el legacy ocultaba todos los botones en ese caso). */
    private GarantiaDto garantiaModificable(Long codGarantia) {
        GarantiaDto g = garantiaExistente(codGarantia);
        if (g.esCerrada()) {
            throw new SpBusinessException("La garantía está cerrada y ya no se puede modificar.");
        }
        return g;
    }

    /**
     * Deja {@code montoGarantiaCalc} igual a la suma de los detalles. Usa la ACCION 'U' del
     * ABM, que reescribe todas las columnas: {@code actual} tiene que estar al dia con lo que
     * hay en la base (si otra escritura de la misma operacion la cambio, releerla antes).
     */
    private void recalcularMontoCalc(GarantiaDto actual, int usuario) {
        BigDecimal suma = sumaMontos(detalleDao.listarPorGarantia(actual.getCodGarantia()));
        if (actual.getMontoGarantiaCalc() != null && actual.getMontoGarantiaCalc().compareTo(suma) == 0) {
            return;
        }
        actual.setMontoGarantiaCalc(suma);
        actual.setAudUsuario((long) usuario);
        garantiaDao.actualizar(actual);
    }

    private static BigDecimal sumaMontos(List<CbrDetalle> detalles) {
        BigDecimal suma = BigDecimal.ZERO;
        for (CbrDetalle d : detalles) {
            if (d.getMontoGarantiaParc() != null) suma = suma.add(d.getMontoGarantiaParc());
        }
        return suma;
    }

    private static void validarMontosAlta(GarantiaCbr g) {
        BigDecimal mg = g.getMontoGarantia() != null ? g.getMontoGarantia() : BigDecimal.ZERO;
        BigDecimal mc = g.getMontoCredito() != null ? g.getMontoCredito() : BigDecimal.ZERO;
        if (mg.signum() < 0 || mc.signum() < 0) {
            throw new SpBusinessException("Los montos no pueden ser negativos.");
        }
        if (mg.signum() == 0 && mc.signum() == 0) {
            throw new SpBusinessException("Debe ingresar un monto de garantía o de crédito mayor a cero.");
        }
    }

    private static void validarDetallesNuevos(List<CbrDetalle> detalles) {
        for (CbrDetalle d : detalles) {
            if (d.getTipoGarantia() == null || d.getTipoGarantia().trim().isEmpty()) {
                throw new SpBusinessException("Cada detalle debe indicar el tipo de garantía.");
            }
            validarMonto(d.getMontoGarantiaParc());
        }
    }

    private static void validarMonto(BigDecimal monto) {
        if (monto != null && monto.signum() < 0) {
            throw new SpBusinessException("El monto del detalle no puede ser negativo.");
        }
    }

    private static void validarFiltro(FiltroGarantiaDto filtro) {
        if (filtro != null && filtro.getEstado() != null && !filtro.getEstado().trim().isEmpty()
                && !ESTADOS.contains(filtro.getEstado().trim())) {
            throw new SpBusinessException("Estado no válido. Use VIGENTE, CADUCADO o CERRADO.");
        }
    }

    /** yyyy-MM-dd de una fecha, para comparar por dia y no por instante. */
    private static String dia(java.util.Date d) {
        return d == null ? "" : new java.sql.Date(d.getTime()).toString();
    }

    private static java.sql.Date sqlDate(java.util.Date d) {
        return d == null ? null : new java.sql.Date(d.getTime());
    }

    private static Double doble(BigDecimal b) {
        return b == null ? null : b.doubleValue();
    }

    /**
     * Logos de los reportes. Los tres .jrxml declaran {@code logoEmpresa},
     * {@code logoSistema} y {@code logoAgua} como {@code InputStream}; hoy solo dibujan
     * {@code logoEmpresa}. Un stream nuevo por llamada: el llenado lo consume.
     */
    private Map<String, Object> parametrosReporte() {
        Map<String, Object> params = new HashMap<>();
        params.put("logoEmpresa", recurso("/logos/logoEmpresa.jpg"));
        params.put("logoAgua", recurso("/logos/logoEmpresaAgua.jpg"));
        params.put("logoSistema", recurso("/logos/logoIzquierdo.jpg"));
        return params;
    }

    private InputStream recurso(String ruta) {
        return getClass().getResourceAsStream(ruta);
    }

    private ResponseEntity<byte[]> pdf(byte[] pdf) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(pdf.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private ResponseEntity<ApiResponse<?>> creado(String mensaje, long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(mensaje, id, HttpStatus.CREATED.value()));
    }

    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, String mensajeVacio) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}
