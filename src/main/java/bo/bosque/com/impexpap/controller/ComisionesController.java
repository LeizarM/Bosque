package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.model.TipoCambioComision;
import bo.bosque.com.impexpap.dao.IComisionDinamica;
import bo.bosque.com.impexpap.dao.IComisionPorRango;
import bo.bosque.com.impexpap.dao.IGrupo;
import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.IEjecucionComision;
import bo.bosque.com.impexpap.dao.IGrupoXVendedor;
import bo.bosque.com.impexpap.dao.INotaPendiente;
import bo.bosque.com.impexpap.dao.IPagadoItem;
import bo.bosque.com.impexpap.dao.IPoliticaBond;
import bo.bosque.com.impexpap.dao.IPreliminarComision;
import bo.bosque.com.impexpap.dao.IVendedor;
import bo.bosque.com.impexpap.dto.EjecucionComisionDto;
import bo.bosque.com.impexpap.dto.FiltroComisionDinamicaDto;
import bo.bosque.com.impexpap.dto.FiltroDescuentoDto;
import bo.bosque.com.impexpap.dto.FiltroEmpresaDto;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.dto.FiltroNotaPreliminarDto;
import bo.bosque.com.impexpap.dto.FiltroPagadoItemDto;
import bo.bosque.com.impexpap.dto.FiltroPreliminarDto;
import bo.bosque.com.impexpap.dto.ReporteComisionDto;
import bo.bosque.com.impexpap.model.ComisionDinamica;
import bo.bosque.com.impexpap.model.ComisionPorRango;
import bo.bosque.com.impexpap.model.FamiliaPolitica;
import bo.bosque.com.impexpap.model.PagadoItem;
import bo.bosque.com.impexpap.model.PagadoItemCorte;
import bo.bosque.com.impexpap.model.PagadoItemResumen;
import bo.bosque.com.impexpap.model.VendedorClienteExcluido;
import bo.bosque.com.impexpap.model.VendedorExentoBond;
import bo.bosque.com.impexpap.model.Grupo;
import bo.bosque.com.impexpap.model.GrupoXVendedor;
import bo.bosque.com.impexpap.model.Vendedor;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modulo de Comisiones de vendedores (tcom).
 * <p>
 * Reemplaza a la pantalla Comisiones.xhtml de Bosque v2. Toda la persistencia
 * pasa por procedimientos almacenados; no hay SQL escrito en Java.
 * <p>
 * Como en el resto del backend, todos los endpoints son POST, incluidas las
 * lecturas, y responden con el envelope {@code {message, data, status}}.
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/comisiones")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class ComisionesController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    /**
     * {@code tb_vista.codVista} de «Comisiones» ({@code tcomComisiones/Comisiones}).
     * <p>
     * Los nombres de abajo son los MISMOS {@code tb_vistaBtn.nombreBtn} que evaluaba
     * {@code wComision.esAutorizado(...)} en Comisiones.xhtml y que hoy consulta
     * {@code ComisionesScreen} en Flutter. No se inventa un esquema nuevo: si el botón
     * no está en la tabla, nadie salvo {@code ROLE_ADM} pasa — y ese fallback es el del
     * legacy, replicado en {@link AccesoModuloHelper#tieneBoton}.
     * <p>
     * Los cinco {@code btnCom*} se crean con {@code sql/01_botones_comisiones.sql}.
     * <b>Ese script tiene que estar corrido antes de desplegar esto</b>, en las dos bases.
     */
    private static final int VISTA_COMISIONES = 82;

    private static final String BTN_VENDEDORES   = "btnComVendedores";
    private static final String BTN_GRUPOS       = "btnComGrupos";
    private static final String BTN_ASIGNACIONES = "btnGrpVen";
    private static final String BTN_DINAMICA     = "btnComDinamica";
    private static final String BTN_RANGOS       = "btnComRangos";
    private static final String BTN_PENDIENTES   = "btnComPendientes";
    private static final String BTN_POLITICA     = "btnComPolitica";
    private static final String TAB_EJECUTAR     = "TabEjecutar";
    private static final String TAB_PRELIMINAR   = "tabPreliminar";
    private static final String TAB_PRELIM_EXT   = "tabPreliminarExt";
    private static final String TAB_PRELIM_DIN   = "tabPreliminarComDinamica";
    private static final String TAB_PRELIM_NEW   = "tabPreliminarComDinamicaNew";

    /**
     * Internas y Esppapel se congelan y se pagan las dos como esInterno 1: las
     * ACCIONes A y G de {@code p_list_pagado} salen ambas de
     * {@code tcom_grupo.esInterno = 1} y se diferencian solo por origen.
     */
    private static final int ESINTERNO_PAGADAS = 1;

    /**
     * El origen con el que se congelan las notas de Esppapel, tal cual lo guarda
     * {@code tcom_pagado.origen}. Es el unico dato que separa Esppapel de las
     * internas: {@code esInterno} vale 1 en las dos.
     */
    private static final String ORIGEN_EPP = "ESPPAPEL";

    /**
     * Como se nombra en los reportes al otro lado de ese corte. Son TRES
     * origenes, no uno: por eso no se puede pedir "las internas" con un solo
     * {@code @origen}, que compara por igualdad.
     */
    private static final String EMPRESAS_INTERNAS = "IMPEXPAP / PAPIRUS / PRODUCTIVA PAPEL";

    private final IGrupo grupoDao;
    private final IVendedor vendedorDao;
    private final IGrupoXVendedor grupoXVendedorDao;
    private final IComisionDinamica comisionDinamicaDao;
    private final IPreliminarComision preliminarDao;
    private final IEjecucionComision ejecucionDao;
    private final INotaPendiente notaPendienteDao;
    private final JasperReportExport jasperReportExport;
    private final IComisionPorRango comisionPorRangoDao;
    private final AccesoModuloHelper acceso;
    private final IPoliticaBond politicaBondDao;
    private final IPagadoItem pagadoItemDao;

    public ComisionesController(IGrupo grupoDao,
                                IVendedor vendedorDao,
                                IGrupoXVendedor grupoXVendedorDao,
                                IComisionDinamica comisionDinamicaDao,
                                IPreliminarComision preliminarDao,
                                IEjecucionComision ejecucionDao,
                                INotaPendiente notaPendienteDao,
                                JasperReportExport jasperReportExport,
                                IComisionPorRango comisionPorRangoDao,
                                AccesoModuloHelper acceso,
                                IPoliticaBond politicaBondDao,
                                IPagadoItem pagadoItemDao) {
        this.grupoDao = grupoDao;
        this.vendedorDao = vendedorDao;
        this.grupoXVendedorDao = grupoXVendedorDao;
        this.comisionDinamicaDao = comisionDinamicaDao;
        this.preliminarDao = preliminarDao;
        this.ejecucionDao = ejecucionDao;
        this.notaPendienteDao = notaPendienteDao;
        this.jasperReportExport = jasperReportExport;
        this.comisionPorRangoDao = comisionPorRangoDao;
        this.acceso = acceso;
        this.politicaBondDao = politicaBondDao;
        this.pagadoItemDao = pagadoItemDao;
    }

    // ==================== GRUPOS ====================

    /** Alta o modificación de un grupo. idGrupo == 0 inserta, mayor a 0 actualiza. */
    @PostMapping("/registrar-grupo")
    public ResponseEntity<ApiResponse<?>> registrarGrupo(@RequestBody Grupo mb,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_GRUPOS);
        return respuestaEscritura(grupoDao.registrarGrupo(mb, mb.getIdGrupo() == 0 ? "I" : "U"));
    }

    /** Baja lógica de un grupo. El SP rechaza la baja si tiene vendedores vigentes. */
    @PostMapping("/eliminar-grupo")
    public ResponseEntity<ApiResponse<?>> eliminarGrupo(@RequestBody Grupo mb,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_GRUPOS);
        return respuestaEscritura(grupoDao.registrarGrupo(mb, "D"));
    }

    /** Grupos activos. idGrupo 0 devuelve todos. */
    @PostMapping("/obtener-grupos")
    public ResponseEntity<ApiResponse<?>> obtenerGrupos(@RequestBody FiltroIdDto filtro) {
        return procesarLista(grupoDao.obtenerGrupos(filtro.getId()), "No existen grupos registrados.");
    }

    /** Grupos que el vendedor todavía no tiene asignados y vigentes. */
    @PostMapping("/obtener-grupos-asignables")
    public ResponseEntity<ApiResponse<?>> obtenerGruposAsignables(@RequestBody FiltroIdDto filtro) {
        return procesarLista(grupoDao.obtenerGruposAsignables(filtro.getId()),
                "El vendedor ya tiene asignados todos los grupos disponibles.");
    }

    /** Grupos incluyendo los dados de baja. */
    @PostMapping("/obtener-grupos-todos")
    public ResponseEntity<ApiResponse<?>> obtenerGruposTodos() {
        return procesarLista(grupoDao.obtenerGruposTodos(), "No existen grupos registrados.");
    }

    // ==================== VENDEDORES ====================

    /**
     * Alta o modificación de un vendedor.
     * <p>
     * Los códigos por empresa SAP viajan en el campo {@code empresasXml} del modelo,
     * con formato {@code <e><i c="1" v="1234"/></e>}, donde c es el bd de la
     * empresa y v el código del vendedor en ella.
     */
    @PostMapping("/registrar-vendedor")
    public ResponseEntity<ApiResponse<?>> registrarVendedor(@RequestBody Vendedor mb,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_VENDEDORES);
        return respuestaEscritura(vendedorDao.registrarVendedor(mb, mb.getIdVendedor() == 0 ? "I" : "U"));
    }

    /** Baja lógica de un vendedor. */
    @PostMapping("/eliminar-vendedor")
    public ResponseEntity<ApiResponse<?>> eliminarVendedor(@RequestBody Vendedor mb,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_VENDEDORES);
        return respuestaEscritura(vendedorDao.registrarVendedor(mb, "D"));
    }

    /** Vendedores activos con sus códigos por empresa en columnas. */
    @PostMapping("/obtener-vendedores")
    public ResponseEntity<ApiResponse<?>> obtenerVendedores(@RequestBody FiltroIdDto filtro) {
        return procesarLista(vendedorDao.obtenerVendedores(filtro.getId()), "No existen vendedores registrados.");
    }

    /** Vendedores de una empresa SAP. bd 0 devuelve todas. */
    @PostMapping("/obtener-vendedores-empresa")
    public ResponseEntity<ApiResponse<?>> obtenerVendedoresPorEmpresa(@RequestBody FiltroEmpresaDto filtro) {
        return procesarLista(vendedorDao.obtenerVendedoresPorEmpresa(filtro.getBd()),
                "No existen vendedores para la empresa seleccionada.");
    }

    /** Vendedores incluyendo los dados de baja. */
    @PostMapping("/obtener-vendedores-todos")
    public ResponseEntity<ApiResponse<?>> obtenerVendedoresTodos() {
        return procesarLista(vendedorDao.obtenerVendedoresTodos(), "No existen vendedores registrados.");
    }

    // ==================== ASIGNACIÓN GRUPO / VENDEDOR ====================

    /**
     * Asigna un grupo a un vendedor o modifica la asignación.
     * <p>
     * El SP rechaza rangos de vigencia solapados para el mismo par vendedor-grupo,
     * porque en ese caso una nota podría tomar dos porcentajes distintos a la vez.
     */
    @PostMapping("/registrar-grupo-vendedor")
    public ResponseEntity<ApiResponse<?>> registrarGrupoXVendedor(@RequestBody GrupoXVendedor mb,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_ASIGNACIONES);
        return respuestaEscritura(
                grupoXVendedorDao.registrarGrupoXVendedor(mb, mb.getIdGrpVen() == 0 ? "I" : "U"));
    }

    /** Cierra la vigencia de una asignación. No borra: la historia la referencia. */
    @PostMapping("/eliminar-grupo-vendedor")
    public ResponseEntity<ApiResponse<?>> eliminarGrupoXVendedor(@RequestBody GrupoXVendedor mb,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_ASIGNACIONES);
        return respuestaEscritura(grupoXVendedorDao.registrarGrupoXVendedor(mb, "D"));
    }

    /** Todas las asignaciones de un vendedor, vigentes o no. */
    @PostMapping("/obtener-grupos-vendedor")
    public ResponseEntity<ApiResponse<?>> obtenerGruposPorVendedor(@RequestBody FiltroIdDto filtro) {
        return procesarLista(grupoXVendedorDao.obtenerPorVendedor(filtro.getId()),
                "El vendedor no tiene grupos asignados.");
    }

    /** Todas las asignaciones vigentes a la fecha. */
    @PostMapping("/obtener-asignaciones-vigentes")
    public ResponseEntity<ApiResponse<?>> obtenerAsignacionesVigentes() {
        return procesarLista(grupoXVendedorDao.obtenerVigentes(), "No existen asignaciones vigentes.");
    }

    // ==================== COMISIÓN DINÁMICA ====================

    /** Alta o modificación de una escala de meta. idDc == 0 inserta. */
    @PostMapping("/registrar-comision-dinamica")
    public ResponseEntity<ApiResponse<?>> registrarComisionDinamica(@RequestBody ComisionDinamica mb,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_DINAMICA);
        return respuestaEscritura(
                comisionDinamicaDao.registrarComisionDinamica(mb, mb.getIdDc() == 0 ? "I" : "U"));
    }

    /** Cierra la vigencia de una escala en lugar de borrarla. */
    @PostMapping("/eliminar-comision-dinamica")
    public ResponseEntity<ApiResponse<?>> eliminarComisionDinamica(@RequestBody ComisionDinamica mb,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_DINAMICA);
        return respuestaEscritura(comisionDinamicaDao.registrarComisionDinamica(mb, "D"));
    }

    /** Escalas de comisión dinámica. esInterno null devuelve internas y externas. */
    @PostMapping("/obtener-comisiones-dinamicas")
    public ResponseEntity<ApiResponse<?>> obtenerComisionesDinamicas(@RequestBody FiltroComisionDinamicaDto filtro) {
        return procesarLista(comisionDinamicaDao.obtenerTodas(filtro.getEsInterno()),
                "No existen escalas de comisión dinámica registradas.");
    }

    /** Escalas vigentes a la fecha indicada. Sin fecha, se usa la de hoy. */
    @PostMapping("/obtener-comisiones-dinamicas-vigentes")
    public ResponseEntity<ApiResponse<?>> obtenerComisionesDinamicasVigentes(
            @RequestBody FiltroComisionDinamicaDto filtro) {
        return procesarLista(comisionDinamicaDao.obtenerVigentes(filtro.getEsInterno(), filtro.getFecha()),
                "No existen escalas vigentes para la fecha indicada.");
    }

    // ==================== VISTAS PRELIMINARES ====================
    //
    // Llaman al SP p_list_paraPagar heredado de Bosque v2, con sus mismas letras
    // de ACCION. La migración no toca la matemática: si un número no coincide
    // con el sistema viejo, es un defecto de la migración, no una mejora.
    // Optimizar ese SP es un cambio posterior y aislado, para poder compararlo.

    /** Rama F. Preliminar de vendedores internos. */
    @PostMapping("/preliminar-interno")
    public ResponseEntity<ApiResponse<?>> preliminarInterno(@RequestBody FiltroPreliminarDto f,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, TAB_PRELIMINAR);
        validarFiltro(f, true);
        return procesarLista(preliminarDao.preliminarInterno(f.getMes(), f.getAnio(), f.getTc()),
                "No hay comisiones internas pendientes para el período seleccionado.");
    }

    /** Rama I. Preliminar de vendedores externos. */
    @PostMapping("/preliminar-externo")
    public ResponseEntity<ApiResponse<?>> preliminarExterno(@RequestBody FiltroPreliminarDto f,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, TAB_PRELIM_EXT);
        validarFiltro(f, true);
        return procesarLista(preliminarDao.preliminarExterno(f.getMes(), f.getAnio(), f.getTc()),
                "No hay comisiones externas pendientes para el período seleccionado.");
    }

    /** Rama J. Comisión dinámica, modalidad anterior. */
    @PostMapping("/preliminar-dinamica-anterior")
    public ResponseEntity<ApiResponse<?>> preliminarDinamicaAnterior(@RequestBody FiltroPreliminarDto f,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, TAB_PRELIM_DIN);
        validarFiltro(f, true);
        return procesarLista(preliminarDao.preliminarDinamicaAnterior(f.getMes(), f.getAnio(), f.getTc()),
                "No hay comisiones dinámicas para el período seleccionado.");
    }

    /** Rama K. Comisión dinámica, modalidad vigente. */
    @PostMapping("/preliminar-dinamica-vigente")
    public ResponseEntity<ApiResponse<?>> preliminarDinamicaVigente(@RequestBody FiltroPreliminarDto f,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, TAB_PRELIM_NEW);
        validarFiltro(f, true);
        return procesarLista(preliminarDao.preliminarDinamicaVigente(f.getMes(), f.getAnio(), f.getTc()),
                "No hay comisiones dinámicas para el período seleccionado.");
    }

    // ==================== CARGA Y EJECUCIÓN DEL PERÍODO ====================


    /**
     * Trae las notas de SAP para que el Preliminar muestre números de hoy.
     *
     * <p>La carga real la hace {@code p_abm_noPagado @ACCION='A'}, que hasta ahora
     * solo disparaba un job del Agente apuntado a {@code BOSQUE-2_0}: al 2026-08-22
     * producción había cargado a las 11:52 y {@code BOSQUE2PRUEBA} seguía con la del
     * 21 a las 22:26, diez notas atrás.
     *
     * <p><b>No se la llama directo.</b> Ese proc no tiene candado: su guardia contra
     * duplicados es un {@code DocNum not in (select docNum from tcom_noPagado ...)},
     * o sea leer y después insertar. Dos llamadas simultáneas ven las dos que la nota
     * falta y las dos la insertan; en PRUEBA hay 3 notas duplicadas de la carga del
     * 21/08, y duplicar acá se paga dos veces. Además reescribe el 82 % de la tabla
     * en cada corrida —89.945 de 109.824 filas el 22/08—, así que tampoco puede
     * correr cada vez que alguien entra a la pestaña.
     *
     * <p>{@code p_abm_tcom_SincronizarNotas} pone lo que faltaba: un candado que
     * <b>hace cola</b> en vez de rendirse. Cada apertura del Preliminar termina con
     * datos posteriores al instante en que se abrió —no hay ventana de frescura, el
     * gerente entra y ve lo de ahora—; lo único que se evita es traer DOS VECES lo
     * mismo cuando dos personas entran al mismo tiempo.
     *
     * <p>El permiso es el de la pestaña que lo necesita: alcanza con cualquiera de
     * las cuatro modalidades del preliminar, porque las cuatro leen la misma tabla.
     */
    @PostMapping("/sincronizar-notas")
    public ResponseEntity<ApiResponse<?>> sincronizarNotas(Authentication auth) {
        if (!acceso.tieneBoton(auth, VISTA_COMISIONES, TAB_PRELIMINAR)
                && !acceso.tieneBoton(auth, VISTA_COMISIONES, TAB_PRELIM_EXT)
                && !acceso.tieneBoton(auth, VISTA_COMISIONES, TAB_PRELIM_DIN)
                && !acceso.tieneBoton(auth, VISTA_COMISIONES, TAB_PRELIM_NEW)) {
            throw new AccessDeniedException("No tiene habilitado el preliminar.");
        }
        return procesarLista(
                ejecucionDao.sincronizarNotas(DatosToken.codUsuarioDe(auth)),
                "No se pudo consultar el estado de la sincronizacion.");
    }

    /** Indica si el período ya fue ejecutado, y con cuántos registros. */
    @PostMapping("/estado-periodo")
    public ResponseEntity<ApiResponse<?>> estadoPeriodo(@RequestBody EjecucionComisionDto d,
            Authentication auth) {
        // El Preliminar tambien lo consulta, para decir «este periodo ya fue
        // ejecutado» en vez de mostrar un cero mudo. Exigir TabEjecutar lo
        // dejaba en 403 a quien solo tiene el preliminar.
        exigirPreliminarOEjecutar(auth);
        validarPeriodo(d);
        return procesarLista(
                ejecucionDao.obtenerEstadoPeriodo(d.getMes(), d.getAnio(), d.getEsInterno()),
                "No se pudo determinar el estado del período.");
    }

    /**
     * Prepara las notas del período. Separa abiertas de cerradas y deja el
     * período listo para ejecutar. No mueve dinero, así que se puede repetir.
     */
    @PostMapping("/cargar-notas")
    public ResponseEntity<ApiResponse<?>> cargarNotas(@RequestBody EjecucionComisionDto d,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, TAB_EJECUTAR);
        validarPeriodo(d);
        return respuestaEscritura(
                ejecucionDao.cargarNotas(d.getMes(), d.getAnio(), d.getEsInterno(), d.getAudUsuario()));
    }

    /**
     * Realiza el corte: pasa las notas a tcom_pagado y las marca pagadas.
     * <p>
     * <b>No es reversible desde la aplicación.</b> El SP rechaza el pedido si el
     * período ya fue ejecutado o si no hay notas cargadas, de modo que el freno
     * no depende de que la pantalla se comporte bien.
     */
    @PostMapping("/ejecutar-pago")
    public ResponseEntity<ApiResponse<?>> ejecutarPago(@RequestBody EjecucionComisionDto d,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, TAB_EJECUTAR);
        validarPeriodo(d);
        if (d.getAudUsuario() <= 0) {
            throw new SpBusinessException("No se identificó el usuario que ejecuta el pago.");
        }
        return respuestaEscritura(
                ejecucionDao.ejecutarPago(d.getMes(), d.getAnio(), d.getEsInterno(), d.getAudUsuario()));
    }

    // ============ COMISIÓN POR RANGO DE DÍAS (solo administradores) ============
    //
    // Define qué porcentaje se paga según cuántos días tardó el cliente en
    // pagar. La usa la rama K del cálculo. Tocarla cambia lo que se paga en
    // todos los períodos que se calculen de acá en adelante, así que el
    // mantenimiento queda restringido a ROLE_ADM.
    //
    // La comisión de esta tabla va en BASE 1: 0.008 es 0,8%. No confundir con
    // tcom_grupo, que guarda puntos porcentuales.

    /**
     * Tipo de cambio vigente. Lo usa el preliminar como valor inicial en vez de
     * traerlo fijo desde el código de la app.
     */
    @PostMapping("/obtener-tipo-cambio")
    public ResponseEntity<ApiResponse<?>> obtenerTipoCambio(@RequestBody ReporteComisionDto d) {
        return procesarLista(comisionPorRangoDao.obtenerTipoCambio(d.getFechaDesde()),
                "No hay tipo de cambio registrado.");
    }

    /** Tramos vigentes. Lectura disponible para cualquier rol del módulo. */
    @PostMapping("/obtener-rangos-comision")
    public ResponseEntity<ApiResponse<?>> obtenerRangosComision() {
        return procesarLista(comisionPorRangoDao.obtenerRangos(),
                "No hay tramos de comisión por días cargados.");
    }

    /** Alta o modificación de un tramo. idCFR == 0 inserta. */
    @PreAuthorize("hasRole('ROLE_ADM')")
    @PostMapping("/registrar-rango-comision")
    public ResponseEntity<ApiResponse<?>> registrarRangoComision(@RequestBody ComisionPorRango mb,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_RANGOS);
        return respuestaEscritura(
                comisionPorRangoDao.registrarRango(mb, mb.getIdCFR() == 0 ? "I" : "U"));
    }

    /** Elimina un tramo. El SP rechaza el borrado si es el último de su tipo. */
    @PreAuthorize("hasRole('ROLE_ADM')")
    @PostMapping("/eliminar-rango-comision")
    public ResponseEntity<ApiResponse<?>> eliminarRangoComision(@RequestBody ComisionPorRango mb,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_RANGOS);
        return respuestaEscritura(comisionPorRangoDao.registrarRango(mb, "D"));
    }

    /**
     * Las notas que componen una fila del preliminar.
     * <p>
     * Es el «Ver Notas a Pagar» de cada fila de Comisiones.xhtml, que en la
     * migracion se habia quedado sin portar. El SP ya estaba: rama G1 de
     * {@code p_list_paraPagar}, la misma que llamaba {@code ParaPagarDao}.
     * <p>
     * Se autoriza con el boton de la pestana de la que salio la fila, porque
     * alli vivia el boton en el legacy. La modalidad la manda el cliente, que es
     * el unico que sabe en que pestana esta parado; si llega una que no se
     * reconoce se rechaza, en vez de elegir un permiso por descarte.
     */
    @PostMapping("/notas-preliminar")
    public ResponseEntity<ApiResponse<?>> notasPreliminar(@RequestBody FiltroNotaPreliminarDto f,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, botonDeModalidad(f.getModalidad()));

        if (f.getIdVendedor() <= 0) {
            throw new SpBusinessException("No se identificó el vendedor de la fila.");
        }
        if (f.getComision() == null) {
            throw new SpBusinessException("La fila no tiene un porcentaje de comisión.");
        }
        validarMesAnio(f.getMes(), f.getAnio());

        return procesarLista(
                preliminarDao.notasDeFila(f.getIdVendedor(), f.getMes(), f.getAnio(), f.comisionCad()),
                "Esta fila no tiene notas para mostrar.");
    }

    /** La modalidad del preliminar al boton del ACL que la protege. */
    private String botonDeModalidad(String modalidad) {
        if (modalidad == null) {
            throw new SpBusinessException("No se indicó la modalidad del preliminar.");
        }
        switch (modalidad) {
            case "interno":          return TAB_PRELIMINAR;
            case "externo":          return TAB_PRELIM_EXT;
            case "dinamicaAnterior": return TAB_PRELIM_DIN;
            case "dinamicaVigente":  return TAB_PRELIM_NEW;
            default:
                throw new SpBusinessException("Modalidad de preliminar desconocida: " + modalidad);
        }
    }

    // ============ POLITICA DEL DESCUENTO POR FAMILIA ============
    //
    // Configura la regla que desde el 21/08/2026 paga solo una parte de los
    // items de ciertas familias -hoy Papel Bond Blanco y Bobina Bond Blanco, al
    // 50%-. Se administra por tabla y no por codigo porque el porcentaje, las
    // familias y las excepciones cambian; el precedente de lo contrario es el
    // `case when vs.idVendedor = 64` que estuvo anos incrustado en un SP.
    //
    // Todo el bloque exige btnComPolitica: define cuanto se le paga a la fuerza
    // de ventas, asi que no puede quedar detras del mismo permiso que una
    // consulta.

    /**
     * Detalle de lo que se descuenta, item por item.
     * <p>
     * Va con btnComPolitica y no con el de preliminar porque expone la regla de
     * negocio completa -que familias, a que porcentaje, a quien-, no solo los
     * numeros del mes.
     *
     * @param accion P periodo abierto, H historico, R resumen
     */
    @PostMapping("/descuento-detalle/{accion}")
    public ResponseEntity<ApiResponse<?>> descuentoDetalle(
            @PathVariable String accion,
            @RequestBody FiltroDescuentoDto f,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);

        if (!"P".equals(accion) && !"H".equals(accion) && !"R".equals(accion)) {
            throw new SpBusinessException("Vista de detalle desconocida: " + accion);
        }
        return procesarLista(
                politicaBondDao.obtenerDescuentoDetalle(
                        f.getMes(), f.getAnio(), f.getOrigen(), f.getIdVendedor(), accion),
                "No hay descuentos para esos filtros.");
    }

    /** Familias de SAP a las que se les puede poner una politica. */
    @PostMapping("/politica-familias-sap")
    public ResponseEntity<ApiResponse<?>> politicaFamiliasSap(Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return procesarLista(politicaBondDao.obtenerFamiliasSap(false),
                "No hay familias registradas.");
    }

    /** Solo las que todavia no tienen politica: es lo que se ofrece al agregar. */
    @PostMapping("/politica-familias-disponibles")
    public ResponseEntity<ApiResponse<?>> politicaFamiliasDisponibles(Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return procesarLista(politicaBondDao.obtenerFamiliasSap(true),
                "Todas las familias ya tienen una politica activa.");
    }

    /** Politicas por familia, con su historial de vigencias. */
    @PostMapping("/politica-familias")
    public ResponseEntity<ApiResponse<?>> politicaFamilias(Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return procesarLista(politicaBondDao.obtenerFamiliaPolitica(),
                "No hay ninguna politica de familia registrada.");
    }

    /** Solo lo que rige hoy. Es lo que se esta aplicando en este momento. */
    @PostMapping("/politica-familias-vigentes")
    public ResponseEntity<ApiResponse<?>> politicaFamiliasVigentes(Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return procesarLista(politicaBondDao.obtenerFamiliaPoliticaVigente(null),
                "Hoy no hay ninguna politica vigente: no se esta descontando nada.");
    }

    /** Alta o modificacion. idFamPolitica == 0 inserta. */
    @PostMapping("/registrar-politica-familia")
    public ResponseEntity<ApiResponse<?>> registrarPoliticaFamilia(
            @RequestBody FamiliaPolitica mb, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return respuestaEscritura(politicaBondDao.registrarFamiliaPolitica(
                mb, mb.getIdFamPolitica() == 0 ? "I" : "U"));
    }

    /** Baja logica. El preliminar de un periodo viejo tiene que seguir siendo
     *  reconstruible con la politica que regia entonces, asi que no se borra. */
    @PostMapping("/eliminar-politica-familia")
    public ResponseEntity<ApiResponse<?>> eliminarPoliticaFamilia(
            @RequestBody FamiliaPolitica mb, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return respuestaEscritura(politicaBondDao.registrarFamiliaPolitica(mb, "D"));
    }

    /** Vendedores exentos del descuento. */
    @PostMapping("/politica-exentos")
    public ResponseEntity<ApiResponse<?>> politicaExentos(Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return procesarLista(politicaBondDao.obtenerVendedoresExentos(),
                "No hay ningun vendedor exento.");
    }

    @PostMapping("/registrar-politica-exento")
    public ResponseEntity<ApiResponse<?>> registrarPoliticaExento(
            @RequestBody VendedorExentoBond mb, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return respuestaEscritura(politicaBondDao.registrarVendedorExento(
                mb, mb.getIdVenExento() == 0 ? "I" : "U"));
    }

    @PostMapping("/eliminar-politica-exento")
    public ResponseEntity<ApiResponse<?>> eliminarPoliticaExento(
            @RequestBody VendedorExentoBond mb, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return respuestaEscritura(politicaBondDao.registrarVendedorExento(mb, "D"));
    }

    /** Clientes cuyas notas no cuentan para el total de un vendedor. */
    @PostMapping("/politica-clientes-excluidos")
    public ResponseEntity<ApiResponse<?>> politicaClientesExcluidos(Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return procesarLista(politicaBondDao.obtenerClientesExcluidos(),
                "No hay ningun cliente excluido.");
    }

    @PostMapping("/registrar-politica-cliente")
    public ResponseEntity<ApiResponse<?>> registrarPoliticaCliente(
            @RequestBody VendedorClienteExcluido mb, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return respuestaEscritura(politicaBondDao.registrarClienteExcluido(
                mb, mb.getIdVenCliExc() == 0 ? "I" : "U"));
    }

    @PostMapping("/eliminar-politica-cliente")
    public ResponseEntity<ApiResponse<?>> eliminarPoliticaCliente(
            @RequestBody VendedorClienteExcluido mb, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_POLITICA);
        return respuestaEscritura(politicaBondDao.registrarClienteExcluido(mb, "D"));
    }

    // ==================== NOTAS PENDIENTES ====================

    /**
     * Notas cerradas que todavía no se pagaron, con sus totales por vendedor.
     * <p>
     * En Bosque v2 esta lista existía en la pantalla pero nunca se llenaba: el
     * método que la cargaba estaba comentado y el diálogo salía siempre vacío.
     */
    @PostMapping("/obtener-notas-pendientes")
    public ResponseEntity<ApiResponse<?>> obtenerNotasPendientes(Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_PENDIENTES);
        return procesarLista(notaPendienteDao.obtenerPendientes(),
                "No hay notas pendientes de pago.");
    }

    // ==================== DETALLE POR ITEM DE LO PAGADO ====================

    /**
     * Las lineas de las notas de un periodo ya pagado, con el motivo por el que
     * cada una entro o no al descuento por familia.
     * <p>
     * Es lo mismo que imprime el reporte <b>si se manda el mismo origen</b>: los
     * PDF son uno por empresa y esta consulta tambien lo es. Sin
     * {@code origen} devuelve IMPEXPAP / PAPIRUS / PRODUCTIVA PAPEL y ESPPAPEL
     * juntas -las dos se congelan con {@code esInterno = 1}- y entonces no
     * cuadra con ninguno de los dos.
     * <p>
     * Vacio no es error: el mensaje del 204 es la lectura del corte, porque un
     * cero pelado no distingue "no habia nada que descontar" de "el historico no
     * se escribio".
     */
    /**
     * Lo pagado se consulta desde el Preliminar y desde Ejecutar.
     * <p>
     * Los reportes de comisiones pagadas y el detalle por item se piden desde
     * las dos pantallas: en Ejecutar porque es donde vivian en el ERP viejo, y
     * en el Preliminar porque quien revisa un periodo necesita contrastarlo
     * contra lo que ya se pago.
     * <p>
     * Exigir solo {@code TabEjecutar} dejaba a un usuario del Preliminar
     * viendo botones que el backend le rechazaba con 403. Pasaba de verdad:
     * aaguilar, con {@code tabPreliminarExt} y {@code tabPreliminarComDinamicaNew},
     * recibia «No se pudo generar el reporte» sobre un periodo ya ejecutado.
     * <p>
     * Misma regla que {@code /sincronizar-notas}: las cuatro modalidades leen
     * la misma tabla, asi que cualquiera habilita.
     */
    private void exigirPreliminarOEjecutar(Authentication auth) {
        if (acceso.tieneBoton(auth, VISTA_COMISIONES, TAB_EJECUTAR)
                || acceso.tieneBoton(auth, VISTA_COMISIONES, TAB_PRELIMINAR)
                || acceso.tieneBoton(auth, VISTA_COMISIONES, TAB_PRELIM_EXT)
                || acceso.tieneBoton(auth, VISTA_COMISIONES, TAB_PRELIM_DIN)
                || acceso.tieneBoton(auth, VISTA_COMISIONES, TAB_PRELIM_NEW)) {
            return;
        }
        throw new AccessDeniedException(
                "No tiene habilitado el preliminar ni la ejecucion de comisiones.");
    }

    @PostMapping("/items-pagados/{accion}")
    public ResponseEntity<ApiResponse<?>> pagadoItems(@PathVariable String accion,
            @RequestBody FiltroPagadoItemDto f,
            Authentication auth) {
        exigirPreliminarOEjecutar(auth);

        // 'R' delega en el resumen: es el mismo SP con otra forma de salida,
        // igual que descuentoDetalle enruta P/H/R por la ruta.
        if ("R".equals(accion)) {
            return pagadoItemsResumen(f, auth);
        }
        if (!"L".equals(accion)) {
            throw new SpBusinessException("Vista de items desconocida: " + accion);
        }
        validarMesAnio(f.getMes(), f.getAnio());
        int esInterno  = esInterno(f);
        String origen  = origen(f);
        List<PagadoItem> items = pagadoItemDao.obtenerItems(f.getMes(), f.getAnio(), esInterno,
                f.getIdPagado(), f.getDocNum(), origen, f.isSoloExcluidos());
        // La lectura del corte se pide solo cuando hay un vacio que explicar:
        // es otra consulta y no tiene sentido pagarla si ya hay que mostrar.
        return items.isEmpty()
                ? procesarLista(items, lecturaItems(f.getMes(), f.getAnio(), esInterno, origen))
                : procesarLista(items, null);
    }

    /**
     * Los mismos items agrupados por motivo: cuantos descontaron y cuantos no,
     * y por que. Es el numero que no existia en ningun lado.
     * <p>
     * Los filtros pesan igual que en el listado. El resumen es el que mas se
     * nota: sin {@code origen} suma las dos empresas y el total no coincide con
     * ninguno de los dos PDF.
     */
    // Sin @PostMapping propio: se llega por /items-pagados/R. Queda como
    // metodo publico porque el handler de arriba lo delega.

    public ResponseEntity<ApiResponse<?>> pagadoItemsResumen(@RequestBody FiltroPagadoItemDto f,
            Authentication auth) {
        // Se llega por /items-pagados/R: mismo permiso que la rama L, o la
        // vista de resumen quedaria cerrada para quien puede ver el detalle.
        exigirPreliminarOEjecutar(auth);
        validarMesAnio(f.getMes(), f.getAnio());
        int esInterno = esInterno(f);
        String origen = origen(f);
        List<PagadoItemResumen> resumen = pagadoItemDao.obtenerResumen(
                f.getMes(), f.getAnio(), esInterno, f.getIdPagado(), f.getDocNum(), origen);
        return resumen.isEmpty()
                ? procesarLista(resumen, lecturaItems(f.getMes(), f.getAnio(), esInterno, origen))
                : procesarLista(resumen, null);
    }

    /**
     * El corte por periodo: una fila por periodo congelado, siempre, aunque el
     * conteo sea cero.
     * <p>
     * Aca mes y anio en 0 valen "todos los periodos" -y esInterno nulo, "las
     * dos formas de pago"-, a diferencia de las dos consultas de arriba: el
     * corte es un historico y se mira entero para ver si algun periodo quedo
     * sin detalle.
     * <p>
     * No recibe origen ni docNum a proposito: {@code tcom_pagadoItemCorte} tiene
     * una fila por (periodo, esInterno) y no guarda el origen, asi que el corte
     * es del periodo entero -las dos empresas juntas- y no se puede partir por
     * empresa. Quien necesite el numero de una sola empresa tiene que contarlo
     * con {@code /items-pagados/R} filtrando por origen.
     */
    @PostMapping("/items-pagados-corte")
    public ResponseEntity<ApiResponse<?>> pagadoItemsCorte(@RequestBody FiltroPagadoItemDto f,
            Authentication auth) {
        exigirPreliminarOEjecutar(auth);
        return procesarLista(
                pagadoItemDao.obtenerCorte(f.getMes() == 0 ? null : f.getMes(),
                        f.getAnio() == 0 ? null : f.getAnio(), f.getEsInterno()),
                "Todavia no se congelo el detalle por item de ningun periodo.");
    }

    // ==================== REPORTES ====================

    /** Comisiones pagadas de vendedores internos, con el detalle por item. */
    @PostMapping("/reporte-pagadas-internas")
    public ResponseEntity<?> reportePagadasInternas(@RequestBody ReporteComisionDto d,
            Authentication auth) {
        exigirPreliminarOEjecutar(auth);
        return pdfPagadasConItems("RptComisionPagada",
                new String[]{"subRptDetComision", "subRptItemComision"}, d, false);
    }

    /** Comisiones pagadas de vendedores externos. */
    @PostMapping("/reporte-pagadas-externas")
    public ResponseEntity<?> reportePagadasExternas(@RequestBody ReporteComisionDto d,
            Authentication auth) {
        exigirPreliminarOEjecutar(auth);
        return pdfPorPeriodo("RptComisionPagadaExterno", new String[]{"subRptDetComisionExterno"}, d);
    }

    /** Comisiones pagadas por importación. */
    @PostMapping("/reporte-importaciones")
    public ResponseEntity<?> reporteImportaciones(@RequestBody ReporteComisionDto d,
            Authentication auth) {
        exigirPreliminarOEjecutar(auth);
        return pdfPorPeriodo("RptComisionesImportaciones",
                new String[]{"subRptComisionesImportacion"}, d);
    }

    /** Comisiones pagadas de Esppapel, con el detalle por item. */
    @PostMapping("/reporte-pagadas-epp")
    public ResponseEntity<?> reportePagadasEpp(@RequestBody ReporteComisionDto d,
            Authentication auth) {
        exigirPreliminarOEjecutar(auth);
        return pdfPagadasConItems("RptComisionPagadaEpp",
                new String[]{"subRptDetComisionEpp", "subRptItemComision"}, d, true);
    }

    /** Notas pendientes. No recibe parámetros: el SP filtra internamente. */
    @PostMapping("/reporte-notas-pendientes")
    public ResponseEntity<?> reporteNotasPendientes(Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, BTN_PENDIENTES);
        return pdf("RptComisionesNotasPendientes", SIN_SUBREPORTES, new HashMap<>());
    }

    /** Comisiones pagadas por vendedor en un rango de fechas. */
    @PostMapping("/reporte-por-vendedor")
    public ResponseEntity<?> reportePorVendedor(@RequestBody ReporteComisionDto d,
            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_COMISIONES, TAB_EJECUTAR);
        if (d.getFechaDesde() == null || d.getFechaHasta() == null) {
            throw new SpBusinessException("Indique la fecha desde y la fecha hasta.");
        }
        if (d.getFechaHasta().before(d.getFechaDesde())) {
            throw new SpBusinessException("La fecha hasta no puede ser anterior a la fecha desde.");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("fechaDesde", new java.sql.Date(d.getFechaDesde().getTime()));
        params.put("fechaHasta", new java.sql.Date(d.getFechaHasta().getTime()));
        return pdf("RptComisionesPagadasXVendedor", SIN_SUBREPORTES, params);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Valida el período y el tipo de cambio antes de llamar al SP.
     * <p>
     * El SP divide entre {@code @tc} sin protección: con cero o nulo da error de
     * división por cero, o devuelve importes en dólares vacíos. Es preferible
     * cortar acá con un mensaje que el usuario entienda.
     */
    private static final String[] SIN_SUBREPORTES = new String[0];

    /** Reportes que se filtran por mes y año. */
    private ResponseEntity<?> pdfPorPeriodo(String nombreReporte, String[] subreportes,
                                            ReporteComisionDto d) {
        return pdf(nombreReporte, subreportes, paramsPeriodo(d));
    }

    /**
     * Los dos reportes de pagadas que ademas imprimen el detalle por item.
     * <p>
     * Van cuatro parametros de mas al Jasper, y ninguno es decorativo:
     * <ul>
     *   <li>{@code esInterno}, porque {@code p_list_tcom_PagadoItem} filtra por
     *       periodo y forma de pago, no por reporte.</li>
     *   <li>{@code origen}, que es el corte por empresa hecho en la base. Va con
     *       valor SOLO en el reporte de Esppapel; ver abajo por que.</li>
     *   <li>{@code esEpp}, que rotula: dice de que empresa es el PDF y decide el
     *       texto de la banda sin datos.</li>
     *   <li>{@code lecturaItems}, para que un periodo sin items imprima por que
     *       no los tiene en vez de una hoja en blanco.</li>
     * </ul>
     * <p>
     * <b>Por que origen viaja en null para el reporte de internas.</b> El
     * parametro {@code @origen} del SP compara por IGUALDAD
     * ({@code @origen IS NULL OR origen = @origen}) y las internas no son un
     * origen sino tres -IMPEXPAP, PAPIRUS y PRODUCTIVA PAPEL-, asi que no hay un
     * solo llamado que signifique "todas menos ESPPAPEL". Esppapel si se corta
     * en la base; para el otro reporte el corte lo sigue haciendo el
     * {@code filterExpression} del subreporte, que es lo unico que puede
     * expresar la negacion. Si algun dia el SP recibe una lista de origenes o un
     * {@code @excluirOrigen}, este null se reemplaza y el filtro del jrxml se
     * cae solo.
     */
    private ResponseEntity<?> pdfPagadasConItems(String nombreReporte, String[] subreportes,
                                                 ReporteComisionDto d, boolean esEpp) {
        Map<String, Object> params = paramsPeriodo(d);
        params.put("esInterno", ESINTERNO_PAGADAS);
        params.put("esEpp", esEpp);
        params.put("origen", esEpp ? ORIGEN_EPP : null);
        // La frase del vacio va en try/catch, igual que tipoCambioDelDia():
        // consulta p_list_tcom_PagadoItemCorte, que es OTRO procedimiento que el
        // del subreporte. Una base con el script 22 corrido y el 23 no —las dos
        // bases ya divergieron antes— devolvia 500 y ningun PDF, cuando el
        // reporte habria salido perfecto: SpHelper convierte cualquier
        // DataAccessException en RuntimeException y sale por arriba.
        //
        // El reporte es el comprobante del pago. Un renglon que solo se imprime
        // cuando NO hay datos no puede dejar sin comprobante a quien lo pidio.
        String lectura;
        try {
            lectura = lecturaItemsReporte(d.getMes(), d.getAnio(), ESINTERNO_PAGADAS, esEpp);
        } catch (RuntimeException e) {
            lectura = "No se pudo determinar por que no hay detalle por item: "
                    + e.getMessage();
        }
        params.put("lecturaItems", lectura);
        return pdf(nombreReporte, subreportes, params);
    }

    private Map<String, Object> paramsPeriodo(ReporteComisionDto d) {
        if (d.getMes() < 1 || d.getMes() > 12) {
            throw new SpBusinessException("Seleccione un mes entre 1 y 12.");
        }
        if (d.getAnio() < 2000 || d.getAnio() > 2100) {
            throw new SpBusinessException("Seleccione un año válido.");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("mes", d.getMes());
        params.put("anio", d.getAnio());
        params.put("tipoCambioDia", tipoCambioDelDia());
        return params;
    }

    /**
     * La cotizacion del dia: la MISMA con la que el reporte arma su columna en
     * dolares.
     * <p>
     * <b>El argumento va en {@code null} a proposito.</b>
     * {@code p_list_tcom_TipoCambio} solo consulta SAP cuando {@code @fecha}
     * viene nula:
     * <pre>
     *     IF @fecha IS NULL
     *         ... FROM ORTT ... SET @origen = 'SAP';
     *     ... si no, cae al historico y SET @origen = 'HISTORICO';
     * </pre>
     * Pasandole la fecha de hoy saltea SAP y devuelve la ultima cotizacion
     * guardada — 6,96 en vez de 11,50 —, que es de otra epoca. Fue exactamente
     * el bug que se vio impreso en la cabecera.
     * <p>
     * Y no es un dato decorativo: {@code p_list_pagado} saca su {@code @tc} de
     * ese mismo ORTT ({@code ORDER BY RateDate DESC}) y con el divide MONTO A
     * PAGAR (USD). Comprobado contra el PDF: 2.762,64 / 240,23 = 11,50. Por eso
     * la cabecera dice que es la usada, en vez de aclarar que no lo es.
     * <p>
     * Ojo con la unica grieta: son dos consultas distintas al mismo origen. Si
     * SAP cargara una cotizacion nueva entre una y otra, la cabecera y la
     * columna podrian diferir. Es una ventana de milisegundos contra una tabla
     * que se actualiza una vez por dia; se acepta antes que reestructurar el SP
     * del reporte para que devuelva su {@code @tc}.
     * <p>
     * Un fallo al obtenerlo no tumba el PDF: el reporte es el comprobante del
     * pago, y quedarse sin el por una linea de cabecera seria peor.
     */
    private String tipoCambioDelDia() {
        try {
            List<TipoCambioComision> tc = comisionPorRangoDao.obtenerTipoCambio(null);
            if (tc.isEmpty() || tc.get(0).getTipoCambio() == null) {
                return "Tipo de cambio del dia: no disponible";
            }
            TipoCambioComision t = tc.get(0);
            String origen = t.getOrigen() == null || t.getOrigen().isEmpty()
                    ? "" : " (" + t.getOrigen() + ")";
            return "Tipo de cambio del dia: "
                    + new java.text.DecimalFormat("#,##0.00").format(t.getTipoCambio())
                    + origen + "  ·  es el usado para la columna en USD";
        } catch (RuntimeException e) {
            return "Tipo de cambio del dia: no disponible";
        }
    }

    /** Internas y Esppapel se congelan como esInterno 1; el filtro puede omitirlo. */
    private int esInterno(FiltroPagadoItemDto f) {
        return f.getEsInterno() == null ? ESINTERNO_PAGADAS : f.getEsInterno();
    }

    /**
     * El origen del filtro, con el blanco tratado como "sin filtro".
     * <p>
     * Una cadena vacia NO es lo mismo que null para el SP: filtraria por un
     * origen que no existe y devolveria cero filas, que se leeria como un
     * periodo sin detalle.
     */
    private String origen(FiltroPagadoItemDto f) {
        String o = f.getOrigen();
        if (o == null) {
            return null;
        }
        o = o.trim();
        return o.isEmpty() ? null : o;
    }

    /**
     * Por que esta consulta no tiene detalle por item, dicho en el mismo alcance
     * en el que se pregunto.
     * <p>
     * Con {@code origen} en null la frase es del periodo entero, que es tambien
     * el alcance de la consulta que quedo vacia. Con un origen la frase habla de
     * esa empresa: si no lo hiciera diria "el periodo no tiene detalle" cuando lo
     * que pasa es que el detalle es todo de la otra empresa.
     */
    private String lecturaItems(int mes, int anio, int esInterno, String origen) {
        return origen == null
                ? lecturaDelPeriodo(mes, anio, esInterno)
                : lecturaDeEmpresa(mes, anio, esInterno, origen,
                        contarItems(mes, anio, esInterno, origen));
    }

    /**
     * La misma frase para los dos PDF, que son uno por empresa.
     * <p>
     * Esppapel se cuenta con su origen. Las internas se cuentan por resta -todo
     * el periodo menos Esppapel- porque {@code @origen} compara por igualdad y
     * son tres origenes; dentro de {@code esInterno = 1} no hay ninguna empresa
     * mas, asi que la resta da exactamente las tres.
     */
    private String lecturaItemsReporte(int mes, int anio, int esInterno, boolean esEpp) {
        if (esEpp) {
            return lecturaDeEmpresa(mes, anio, esInterno, ORIGEN_EPP,
                    contarItems(mes, anio, esInterno, ORIGEN_EPP));
        }
        int delPeriodo = contarItems(mes, anio, esInterno, null);
        int deEpp      = contarItems(mes, anio, esInterno, ORIGEN_EPP);
        return lecturaDeEmpresa(mes, anio, esInterno, EMPRESAS_INTERNAS, delPeriodo - deEpp);
    }

    /**
     * Lo que se imprime cuando la vista de una empresa quedo vacia.
     * <p>
     * Son dos ceros distintos y la banda sin datos los imprimia como uno solo:
     * el periodo puede no tener nada congelado, o puede tener 19 items que son
     * todos de la otra empresa. Por eso la empresa va primero y el numero del
     * periodo va detras, dicho como lo que es -las dos empresas juntas- para que
     * no se lea como si fuera de esta.
     */
    private String lecturaDeEmpresa(int mes, int anio, int esInterno,
                                    String empresa, int itemsEmpresa) {
        String periodo = "  El periodo, con las dos empresas juntas: "
                + lecturaDelPeriodo(mes, anio, esInterno);
        if (itemsEmpresa > 0) {
            // Hay lineas de la empresa, pero ninguna llego hasta aca: en el PDF
            // es porque el reporte imprime solo lo que descuenta.
            return empresa + " tiene " + itemsEmpresa + " lineas congeladas en "
                    + mes + "/" + anio + ", pero ninguna entra en esta vista." + periodo;
        }
        return empresa + " no tiene ninguna linea congelada en " + mes + "/" + anio + "."
                + periodo;
    }

    /**
     * Cuantas lineas congelo el periodo para una empresa.
     * <p>
     * Se cuenta con el resumen y no con el listado porque el resumen ya viene
     * agrupado por motivo: son pocas filas aunque el periodo tenga miles de
     * items.
     *
     * @param origen la empresa; null cuenta el periodo entero
     */
    private int contarItems(int mes, int anio, int esInterno, String origen) {
        int total = 0;
        for (PagadoItemResumen r : pagadoItemDao.obtenerResumen(mes, anio, esInterno,
                null, null, origen)) {
            if (r.getItems() != null) {
                total += r.getItems();
            }
        }
        return total;
    }

    /**
     * Por que un periodo puede no tener detalle por item.
     * <p>
     * Cero items es un resultado valido -ninguna nota del periodo cayo dentro de
     * la vigencia de alguna politica- y a la vez es como se ve un historico que
     * no se escribio. El corte es lo unico que los separa, asi que ningun cero
     * de este modulo sale sin esta frase al lado.
     * <p>
     * Es del periodo y de las dos empresas juntas: {@code tcom_pagadoItemCorte}
     * no guarda el origen. Quien la imprima al lado de una empresa tiene que
     * decir de que alcance es.
     */
    private String lecturaDelPeriodo(int mes, int anio, int esInterno) {
        List<PagadoItemCorte> cortes = pagadoItemDao.obtenerCorte(mes, anio, esInterno);
        if (cortes.isEmpty()) {
            return "El periodo " + mes + "/" + anio + " no tiene corte de items: se pago"
                    + " antes de que existiera el historico por item, o el congelado no corrio.";
        }
        PagadoItemCorte c = cortes.get(0);
        return c.getLectura() + ".  " + c.getItems() + " items congelados, "
                + c.getItemsExcluidos() + " sin descuento;  " + c.getNotasPagadas()
                + " notas pagadas, " + c.getNotasSinItems() + " sin detalle.";
    }

    /**
     * Compila y devuelve el PDF.
     * <p>
     * Ni exportPDF ni exportPDFStatic servían: el primero está atado al
     * subreporte de ficha trabajador y busca el principal en la raíz del
     * classpath; el segundo exige archivos .jasper precompilados, y entonces los
     * cambios hechos sobre el .jrxml no se verían.
     */
    private ResponseEntity<?> pdf(String nombreReporte, String[] subreportes,
                                  Map<String, Object> params) {
        byte[] bytes =
                jasperReportExport.exportPDFConSubreportes(nombreReporte, subreportes, params);
        if (bytes == null || bytes.length == 0) {
            throw new SpBusinessException("El reporte no devolvió datos para los filtros indicados.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(bytes.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /** El periodo, que los tres filtros del modulo validan igual. */
    private void validarMesAnio(int mes, int anio) {
        if (mes < 1 || mes > 12) {
            throw new SpBusinessException("Seleccione un mes entre 1 y 12.");
        }
        if (anio < 2000 || anio > 2100) {
            throw new SpBusinessException("Seleccione un año válido.");
        }
    }

    private void validarPeriodo(EjecucionComisionDto d) {
        validarMesAnio(d.getMes(), d.getAnio());
    }

    private void validarFiltro(FiltroPreliminarDto f, boolean exigeTipoCambio) {
        validarMesAnio(f.getMes(), f.getAnio());
        if (exigeTipoCambio
                && (f.getTc() == null || f.getTc().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
            throw new SpBusinessException("Indique el tipo de cambio: se usa para convertir a dólares.");
        }
    }

    /** 201 si el SP no reportó error, 400 con el mensaje del SP si lo reportó. */
    private ResponseEntity<ApiResponse<?>> respuestaEscritura(RespuestaSp res) {
        HttpStatus status = res.getError() == 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), status.value()));
    }

    /** 200 con la lista, o 204 si vino vacía. Vacío no es error. */
    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, String mensajeVacio) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}
