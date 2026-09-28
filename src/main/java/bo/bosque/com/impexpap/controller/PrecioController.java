package bo.bosque.com.impexpap.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.AccesoPantallaPrecios;
import bo.bosque.com.impexpap.commons.ArmadoPropuestaService;
import bo.bosque.com.impexpap.commons.GeneracionPropuestaService;
import bo.bosque.com.impexpap.commons.ReportesPreciosService;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IArticuloPropuesto;
import bo.bosque.com.impexpap.dao.IAutorizacion;
import bo.bosque.com.impexpap.dao.ICostoIncre;
import bo.bosque.com.impexpap.dao.IPorcentaje;
import bo.bosque.com.impexpap.dao.IPrecio;
import bo.bosque.com.impexpap.dao.IPrecioPropuesta;
import bo.bosque.com.impexpap.dao.IProducto;
import bo.bosque.com.impexpap.dao.IPropuesta;
import bo.bosque.com.impexpap.dto.ArmadoArticulosDto;
import bo.bosque.com.impexpap.dto.ArmadoFamiliaDto;
import bo.bosque.com.impexpap.dto.ArmadoLoteDto;
import bo.bosque.com.impexpap.dto.FiltroCodigosFamiliaDto;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.dto.GuardarFamiliaDto;
import bo.bosque.com.impexpap.dto.GuardarFletesDto;
import bo.bosque.com.impexpap.dto.PorcentajeDto;
import bo.bosque.com.impexpap.dto.ProductoDto;
import bo.bosque.com.impexpap.dto.QuitarArticuloDto;
import bo.bosque.com.impexpap.dto.ResolverPropuestaDto;
import bo.bosque.com.impexpap.model.Autorizacion;
import bo.bosque.com.impexpap.model.Porcentaje;
import bo.bosque.com.impexpap.model.Producto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modulo de Precios (tpr) — propuestas de precios, autorizacion y catalogo de familias.
 * <p>
 * Reemplaza a la pantalla {@code tprAutorizacion/Autorizacion.xhtml} de Bosque v2, que
 * era un monolito de 1417 lineas con ~22 dialogos manejado por un unico ManagedBean
 * ({@code WizardProductoNew}, 2087 lineas).
 * <p>
 * Toda la persistencia pasa por procedimientos almacenados a traves de {@code SpHelper};
 * no hay SQL escrito en Java. Como en el resto del backend, todos los endpoints son POST
 * —incluidas las lecturas— y responden con el envelope {@code {message, data, status}}.
 *
 * <h2>Que cambio respecto de la version anterior de este controlador</h2>
 * <ol>
 *   <li><b>El {@code audUsuario} ya no se acepta del cuerpo.</b> Antes tres endpoints
 *       hacian {@code propuestaDao.ultimaIdPropuesta(cuerpo.getAudUsuario())} para
 *       reatar el detalle a su cabecera. Como el {@code audUsuario} llegaba del cliente,
 *       cualquiera podia mandar el de otra persona y colgar su costo o su precio de la
 *       propuesta ajena; y dos pestanias del mismo usuario se pisaban entre si. Ahora el
 *       usuario sale de {@link DatosToken#codUsuarioDe(Authentication)} y la propuesta
 *       destino viaja explicita en el cuerpo.</li>
 *   <li><b>Permiso por boton en las tres operaciones sensibles.</b> Aprobar una propuesta
 *       cambia los precios de venta de toda la empresa. El gate de rol no distingue quien
 *       aprueba de quien solo mira, asi que se replica el mismo control que hacia el JSF
 *       con {@code loginX.autorizarBtn(...)}, ahora validado en el servidor.</li>
 *   <li><b>La aprobacion es transaccional.</b> Encadena dos escrituras dependientes y
 *       ningun procedimiento del modulo abre transaccion propia, asi que sin
 *       {@code @Transactional} un fallo a mitad de camino dejaba los precios aplicados
 *       sin registro de autorizacion.</li>
 *   <li><b>Envelope uniforme.</b> Antes cada endpoint devolvia una forma distinta
 *       (lista cruda, o un {@code Map} con {@code msg} y {@code ok}).</li>
 * </ol>
 *
 * <h2>Porcentajes por familia y lista de precios (tpr_porcentaje)</h2>
 * El porcentaje es el margen que se aplica sobre el costo para calcular el precio de cada
 * familia en cada lista de precios: es parte del calculo, no un catalogo, y por eso vive
 * aca y no en el controlador de catalogos. Reemplaza a los dialogos {@code dlgPorcen}
 * (porcentaje de una familia) y {@code dlgPorcGrupo} (edicion masiva por grupo de familia
 * SAP) de Autorizacion.xhtml.
 *
 * <h3>Dos cosas que el legacy hacia y aca no se replican</h3>
 * <ol>
 *   <li><b>La aplicacion masiva por grupo de familia SAP NO esta expuesta.</b> El JSF la
 *       resolvia con un procedimiento aparte, {@code p_abm_porcentajeXGrupo}
 *       ({@code @idGrpFamiliaSap, @idClasificacion, @porcen, @audUsuario, @codsExcluidos}),
 *       que no forma parte del contrato de {@link IPorcentaje}: ni {@code p_abm_porcentaje}
 *       ni {@code p_list_porcentaje} tienen una ACCION que aplique un porcentaje a todas las
 *       familias de un grupo con exclusiones. Habilitarla exige <b>una ACCION nueva en el
 *       procedimiento</b> (o incorporar {@code p_abm_porcentajeXGrupo} al modulo) y su
 *       metodo en el DAO; no se emula desde Java iterando familia por familia porque serian
 *       N escrituras sueltas, sin transaccion y sin la semantica set-based del original.
 *       Mientras tanto la pantalla arma la edicion masiva con
 *       {@link #lstPorcentajeParaGrupo(FiltroIdDto)} (la grilla de destinos) y
 *       {@link #lstFamiliaXGrupo(FiltroIdDto)} (las familias del grupo, para marcar cuales
 *       excluir), y guarda con {@link #registrarPorcentaje(Porcentaje, Authentication)} fila
 *       por fila.</li>
 *   <li><b>La validacion de porcentajes ascendentes por sucursal no se replica.</b> En el
 *       legacy {@code WizardProductoNew.validaPorcentaje()} recorre la grilla, arma el
 *       mensaje de error y <b>siempre devuelve 0</b>: el llamador solo mira ese retorno, asi
 *       que la validacion nunca bloqueo nada y los datos de tpr_porcentaje se cargaron
 *       durante anios sin ese control. Aca no se finge que existe: el alta y la modificacion
 *       reciben <b>una</b> fila por llamada, y una regla que compara las listas de precios de
 *       una misma sucursal entre si necesita la grilla completa. Si se decide exigirla, el
 *       lugar es un endpoint que reciba la grilla entera —o el propio procedimiento—, y tiene
 *       que rechazar la operacion de verdad, no solo mostrar un mensaje.</li>
 * </ol>
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/price")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class PrecioController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    /**
     * {@code tb_vista.codVista} de «Cambio de Precios»
     * ({@code tprAutorizacion/Autorizacion}, padre {@code codVista} 65 «modPrecios»).
     */
    private static final int VISTA_PRECIOS = 66;

    /**
     * Nombres de {@code tb_vistaBtn.nombreBtn} tal como ya existen para esta vista. Son
     * exactamente los tres que evaluaba {@code WizardProductoNew.esAutorizado(...)} en
     * Autorizacion.xhtml: no se inventa un esquema nuevo.
     *
     * <p><b>Antes de desplegar hay que verificar que las filas de {@code tb_usuarioBtn}
     * esten cargadas para los usuarios que hoy aprueban.</b> El fallback de
     * {@link AccesoModuloHelper#tieneBoton} deja pasar solo a {@code ROLE_ADM} cuando el
     * boton no esta asignado, asi que sin esas filas un autorizador con {@code ROLE_LIM}
     * pierde el acceso que hoy tiene.
     */
    private static final String BTN_APROBAR = "btnAprobar";
    private static final String BTN_EN_ESPERA = "btnPen";
    private static final String BTN_GENERAR = "btnGen";

    /** {@code tpr_autorizacion.esAprobada} = 1. Unico estado que aplica los precios. */
    private static final int ESTADO_APROBADA = 1;
    private static final int ESTADO_RECHAZADA = 2;
    private static final int ESTADO_EN_ESPERA = 3;
    private static final int TIPO_POR_ARTICULO = 2;

    private final IAutorizacion autDao;
    private final ICostoIncre costoIncreDao;
    private final IProducto productoDao;
    private final IArticuloPropuesto articuloPropuestoDao;
    private final IPrecio precioDao;
    private final IPropuesta propuestaDao;
    private final IPrecioPropuesta precioPropuestaDao;
    private final IPorcentaje porcentajeDao;
    private final AccesoModuloHelper acceso;
    private final AccesoPantallaPrecios pantallas;
    private final ArmadoPropuestaService armado;
    private final ReportesPreciosService reportes;
    private final GeneracionPropuestaService generacion;

    public PrecioController(IAutorizacion autDao,
                            ICostoIncre costoIncreDao,
                            IProducto productoDao,
                            IArticuloPropuesto articuloPropuestoDao,
                            IPrecio precioDao,
                            IPropuesta propuestaDao,
                            IPrecioPropuesta precioPropuestaDao,
                            IPorcentaje porcentajeDao,
                            AccesoModuloHelper acceso,
                            AccesoPantallaPrecios pantallas,
                            ArmadoPropuestaService armado,
                            ReportesPreciosService reportes,
                            GeneracionPropuestaService generacion) {
        this.autDao = autDao;
        this.costoIncreDao = costoIncreDao;
        this.productoDao = productoDao;
        this.articuloPropuestoDao = articuloPropuestoDao;
        this.precioDao = precioDao;
        this.propuestaDao = propuestaDao;
        this.precioPropuestaDao = precioPropuestaDao;
        this.porcentajeDao = porcentajeDao;
        this.acceso = acceso;
        this.pantallas = pantallas;
        this.armado = armado;
        this.reportes = reportes;
        this.generacion = generacion;
    }

    // ===================================================================== //
    //                              LECTURAS                                 //
    // ===================================================================== //

    /** Propuestas pendientes de autorizacion, con su estado y quien las genero. */
    @PostMapping("/autorizacion")
    public ResponseEntity<ApiResponse<?>> obtenerListaPropuesta() {
        return procesarLista(autDao.listAutorizacion(), "No existen propuestas registradas.");
    }

    /**
     * Estados posibles de una propuesta.
     *
     * <p>Sale de {@code utils.Tipos}, no de la base. El catalogo real vive en la vista
     * {@code v_tipos} grupo 38, que esta construida con literales dentro del propio
     * {@code CREATE VIEW}: agregar un estado alli exige un {@code ALTER VIEW}. Queda
     * pendiente unificar las dos fuentes.
     */
    @PostMapping("/estadoPropuesta")
    public ResponseEntity<ApiResponse<?>> listPropuestas() {
        return procesarLista(autDao.lstEstadoPropuestas(), "No hay estados configurados.");
    }

    /** Costo de flete de transporte por sucursal. */
    @PostMapping("/costoFlete")
    public ResponseEntity<ApiResponse<?>> listTransporte() {
        return procesarLista(costoIncreDao.listarCostoPorSucursal(),
                "No existen costos de flete registrados.");
    }

    /** Proveedores del catalogo SAP. */
    @PostMapping("/lstProveedor")
    public ResponseEntity<ApiResponse<?>> listProveedor() {
        return procesarLista(productoDao.listarProveedoresSap(),
                "No existen proveedores registrados.");
    }

    /**
     * Familias de producto con su descripcion resuelta (presentacion, tipo, color,
     * rango de gramaje, grupo SAP y proveedor).
     *
     * @param producto filtro; los campos en null no filtran
     */
    @PostMapping("/listFamilia")
    public ResponseEntity<ApiResponse<?>> listFamilia(@RequestBody Producto producto) {
        return procesarLista(productoDao.listarConDescripcion(producto),
                "No existen familias para el filtro indicado.");
    }

    /** Familias que pertenecen a un grupo de familia SAP, con su proveedor. */
    @PostMapping("/listFamiliaXGrupo")
    public ResponseEntity<ApiResponse<?>> lstFamiliaXGrupo(@RequestBody FiltroIdDto filtro) {
        return procesarLista(productoDao.listarFamiliasPorGrupoFamilia(filtro.getId()),
                "El grupo de familia no tiene familias asociadas.");
    }

    /**
     * Articulos del catalogo pertenecientes a una o varias familias.
     *
     * <p>El {@code codCad} viaja ahora en {@link FiltroCodigosFamiliaDto} y no dentro del
     * model {@code ArticuloPropuesto}: no es una columna de la tabla, y el model tiene que
     * traer exactamente las columnas porque {@code SpHelper} manda cada campo como
     * parametro del procedimiento.
     */
    @PostMapping("/listFamiliaXArticulo")
    public ResponseEntity<ApiResponse<?>> lstArticulosXFamilia(
            @RequestBody FiltroCodigosFamiliaDto filtro) {
        return procesarLista(articuloPropuestoDao.listarArticulosPorFamilias(filtro.getCodCad()),
                "No existen articulos para las familias indicadas.");
    }

    /** Una familia con su descripcion resuelta. */
    @PostMapping("/cargarProducto")
    public ResponseEntity<ApiResponse<?>> obtenerDatoFamilia(@RequestBody Producto producto) {
        exigirCodigoFamilia(producto.getCodigoFamilia());

        ProductoDto dto = productoDao.obtenerConDescripcion(producto.getCodigoFamilia());
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("La familia no existe.", null,
                            HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, dto, HttpStatus.OK.value()));
    }

    /**
     * Precios vigentes por tonelada de una familia, con porcentaje, IVA e IT, listos para
     * armar la grilla de repreciacion.
     */
    @PostMapping("/lstPrecioTonXFamilia")
    public ResponseEntity<ApiResponse<?>> obtenerPreciosActualesXFamilia(
            @RequestBody Producto filtro) {
        exigirCodigoFamilia(filtro.getCodigoFamilia());

        return procesarLista(precioDao.listarDetalleParaReprecio(filtro.getCodigoFamilia()),
                "La familia no tiene precios cargados.");
    }

    /**
     * La vista preliminar de una propuesta, con las mismas filas que su PDF: porcentaje,
     * precio actual y precio propuesto por tonelada de cada articulo (por familia), o el
     * precio que toma cada articulo (por articulo).
     */
    @PostMapping("/vistaPropuesta")
    public ResponseEntity<ApiResponse<?>> vistaPropuesta(@RequestBody FiltroIdDto filtro) {
        exigirPropuesta(filtro.getId());
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, reportes.vista(filtro.getId()),
                HttpStatus.OK.value()));
    }

    // /lstArticulosXPropuesta (la rama D suelta) se quito el 2026-09-25: la vista
    // preliminar lee /vistaPropuesta, que trae esos mismos precios por unidad.

    // Las escrituras sueltas de la cabecera, el flete, el precio propuesto y el costo
    // sugerido (/registrarPropuesta, /registrarCostoIncre, /registrarPrecioPropuesta y
    // /registrarCostoSug) se quitaron el 2026-09-25. La app ya no las usaba: todo pasa por
    // el asistente, que recalcula y exige quien opera. Y no pedian ningun permiso, asi que
    // cualquiera con sesion podia cambiar los precios de una propuesta En Espera antes de
    // que la aprobaran.

    // ===================================================================== //
    //                        FAMILIAS (tpr_producto)                        //
    // ===================================================================== //

    /**
     * Alta de una familia: la ficha de la pantalla Familias, que reemplaza a dlgDtFam y
     * dlgFam del JSF.
     *
     * <p>El codigo lo elige quien la carga: la PK de {@code tpr_producto} no es IDENTITY.
     * {@code p_abm_producto 'I'} rechaza un codigo repetido y crea la familia activa, sin
     * costo y con un precio en 0 por cada lista activa. El costo y la propuesta aprobada
     * los mueve despues el circuito de propuestas, nunca esta ficha.
     *
     * <p>Con la familia van sus porcentajes por lista de precios, como en el dialogo
     * "Registro de familias" del JSF ({@code WizardProductoNew.saveFamilia}), que los
     * insertaba uno por lista despues del alta. Aca van en la misma transaccion: si un
     * porcentaje falla, la familia no queda creada a medias.
     */
    @PostMapping("/familia/registrar")
    @Transactional
    public ResponseEntity<ApiResponse<?>> registrarFamilia(@RequestBody GuardarFamiliaDto familia,
                                                           Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.FAMILIAS);
        Producto p = familiaParaGuardar(familia, auth, true);
        // Se valida todo antes de escribir nada.
        List<Porcentaje> porcentajes = porcentajesParaGuardar(familia.getPorcentajes(),
                idsDe(porcentajeDao.listarSucursalesParaAbm()));
        RespuestaSp res = productoDao.registrar(p);
        guardarPorcentajes(p.getCodigoFamilia(), porcentajes, new HashMap<Long, Long>(),
                p.getAudUsuario());
        return respuestaEscritura(res);
    }

    /**
     * Modificacion de una familia. {@code p_abm_producto 'U'} reescribe todas las columnas
     * de la ficha -incluido el estado-, asi que se exige la fila completa. No toca el
     * costo ni la propuesta aprobada.
     *
     * <p>Los porcentajes que vengan se guardan en la misma transaccion: modifica los que la
     * familia ya tiene y da de alta los que le faltan. Cual es cual lo decide la grilla de
     * la base ({@code p_list_porcentaje 'A'}), no el {@code idPorcen} del cuerpo: asi la
     * ficha no puede tocar el porcentaje de otra familia.
     */
    @PostMapping("/familia/actualizar")
    @Transactional
    public ResponseEntity<ApiResponse<?>> actualizarFamilia(@RequestBody GuardarFamiliaDto familia,
                                                            Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.FAMILIAS);
        Producto p = familiaParaGuardar(familia, auth, false);
        Map<Long, Long> existentes = new HashMap<>();
        List<Porcentaje> porcentajes = null;
        if (familia.getPorcentajes() != null) {
            List<PorcentajeDto> grilla = porcentajeDao.listarPorcentajesPorFamilia(p.getCodigoFamilia());
            for (PorcentajeDto g : grilla) {
                if (g.getIdClasificacion() != null && g.getIdPorcen() != null && g.getIdPorcen() > 0L) {
                    existentes.put(g.getIdClasificacion(), g.getIdPorcen());
                }
            }
            porcentajes = porcentajesParaGuardar(familia.getPorcentajes(), idsDe(grilla));
        }
        RespuestaSp res = productoDao.actualizar(p);
        guardarPorcentajes(p.getCodigoFamilia(), porcentajes, existentes, p.getAudUsuario());
        return respuestaEscritura(res);
    }

    /**
     * Las listas de precios activas, por sucursal, con el porcentaje en 0: la grilla
     * "Porcentaje por familia" del alta ({@code p_list_porcentaje 'B'}). El procedimiento
     * no ordena; ordena la pantalla.
     */
    @PostMapping("/familia/listasParaPorcentaje")
    public ResponseEntity<ApiResponse<?>> listasParaPorcentaje() {
        return procesarLista(porcentajeDao.listarSucursalesParaAbm(),
                "No hay listas de precios activas configuradas.");
    }

    /**
     * Valida la grilla de la ficha: cada fila con su lista de precios, que tiene que ser
     * una de las permitidas y no repetirse, y un porcentaje de 0 o mas. Devuelve null si
     * la ficha no mando porcentajes.
     */
    private static List<Porcentaje> porcentajesParaGuardar(List<Porcentaje> filas, Set<Long> permitidas) {
        if (filas == null) return null;
        Set<Long> vistas = new HashSet<>();
        for (Porcentaje f : filas) {
            if (f == null || f.getIdClasificacion() == null || !permitidas.contains(f.getIdClasificacion())) {
                throw new SpBusinessException("Un porcentaje de la ficha no corresponde a una lista de precios activa.");
            }
            if (!vistas.add(f.getIdClasificacion())) {
                throw new SpBusinessException("La ficha trae dos porcentajes para la misma lista de precios.");
            }
            if (f.getPorcen() == null || f.getPorcen().signum() < 0) {
                throw new SpBusinessException("Los porcentajes de la familia tienen que ser 0 o más.");
            }
        }
        return filas;
    }

    private static Set<Long> idsDe(List<PorcentajeDto> grilla) {
        Set<Long> ids = new HashSet<>();
        if (grilla != null) {
            for (PorcentajeDto g : grilla) {
                if (g.getIdClasificacion() != null) ids.add(g.getIdClasificacion());
            }
        }
        return ids;
    }

    /** Una llamada por lista, que es lo que soporta {@code p_abm_porcentaje}. */
    private void guardarPorcentajes(int codigoFamilia, List<Porcentaje> filas, Map<Long, Long> existentes,
                                    Long audUsuario) {
        if (filas == null) return;
        for (Porcentaje f : filas) {
            Long idPorcen = existentes.get(f.getIdClasificacion());
            Porcentaje x = new Porcentaje();
            x.setIdPorcen(idPorcen);
            x.setCodigoFamilia(codigoFamilia);
            x.setIdClasificacion(f.getIdClasificacion());
            x.setPorcen(f.getPorcen());
            x.setAudUsuario(audUsuario);
            porcentajeDao.registrarPorcentaje(x, idPorcen == null ? "I" : "U");
        }
    }

    /**
     * Da de baja (estado 0) o reactiva (estado 1) una familia sin abrir la ficha:
     * {@code p_abm_producto 'E'}. Una familia inactiva no entra en propuestas nuevas ni se
     * lista entre las activas.
     */
    @PostMapping("/familia/cambiarEstado")
    public ResponseEntity<ApiResponse<?>> cambiarEstadoFamilia(@RequestBody Producto familia,
                                                               Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.FAMILIAS);
        exigirCodigoFamilia(familia.getCodigoFamilia());
        Integer estado = familia.getEstado();
        if (estado == null || (estado != 0 && estado != 1)) {
            throw new SpBusinessException("Debe indicar si la familia queda activa o inactiva.");
        }
        return respuestaEscritura(productoDao.cambiarEstado(familia.getCodigoFamilia(), estado,
                usuarioDelToken(auth)));
    }

    /**
     * Elimina una familia que nunca se uso -la creada por error-, con sus precios en 0 y
     * sus porcentajes: {@code p_abm_producto 'D'}. Si ya tiene movimiento (articulos,
     * propuestas, costos, precios cargados o filas en el respaldo), el procedimiento
     * rechaza con el motivo y no toca nada: esa familia se da de baja.
     */
    @PostMapping("/familia/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarFamilia(@RequestBody Producto familia,
                                                          Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.FAMILIAS);
        exigirCodigoFamilia(familia.getCodigoFamilia());
        return respuestaEscritura(productoDao.eliminar(familia.getCodigoFamilia(),
                usuarioDelToken(auth)));
    }

    /**
     * Cambia el grupo y el proveedor SAP de una familia sin tocar el resto de la ficha:
     * {@code p_abm_producto 'F'} y {@code 'G'}. Van juntos en una transaccion: si el
     * segundo falla, el primero se deshace. 0 o null = sin asignar.
     */
    @PostMapping("/familia/asignarSap")
    @Transactional
    public ResponseEntity<ApiResponse<?>> asignarSapFamilia(@RequestBody Producto familia,
                                                            Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.FAMILIAS);
        exigirCodigoFamilia(familia.getCodigoFamilia());
        int codigo = familia.getCodigoFamilia();
        long usuario = usuarioDelToken(auth);
        Long grupo = sinId(familia.getIdGrpFamiliaSap()) ? null : familia.getIdGrpFamiliaSap();
        Long proveedor = sinId(familia.getIdProveedorSap()) ? null : familia.getIdProveedorSap();

        // Un error de negocio de cualquiera de los dos llega como SpBusinessException y
        // deshace la transaccion.
        productoDao.asignarGrupoFamiliaSap(codigo, grupo, usuario);
        productoDao.asignarProveedorSap(codigo, proveedor, usuario);
        return respuestaEscritura(new RespuestaSp(0,
                "Grupo y proveedor SAP de la familia " + codigo + " actualizados.", codigo));
    }

    /**
     * Trae de SAP los proveedores y grupos de familia nuevos y actualiza los nombres de los
     * proveedores: {@code p_abm_producto 'H'}. El JSF lo corria cada vez que se abria la
     * pantalla de precios; la app lo corre al abrir la ficha de una familia y desde
     * Catalogos, asi que alcanza con cualquiera de las dos pantallas.
     *
     * <p>Sin {@code @Transactional}: lee de SAP por OPENROWSET (punto 17 de
     * tpr_Producto.sql).
     */
    @PostMapping("/familia/sincronizarSap")
    public ResponseEntity<ApiResponse<?>> sincronizarCatalogosSap(Authentication auth) {
        if (!pantallas.tiene(auth, AccesoPantallaPrecios.FAMILIAS)
                && !pantallas.tiene(auth, AccesoPantallaPrecios.CATALOGOS)) {
            throw new AccessDeniedException("No tiene habilitada la pantalla de Familias ni la de Catálogos.");
        }
        return respuestaEscritura(productoDao.sincronizarCatalogosSap(usuarioDelToken(auth)));
    }

    /**
     * Historial del costo de una familia: cada aprobacion que le cambio la propuesta y,
     * si cambio, el costo, la mas reciente primero ({@code p_list_bitCostoProducto}, el
     * del dialogo "Bitacora de costo / propuesta" del JSF).
     */
    @PostMapping("/familia/historialCosto")
    public ResponseEntity<ApiResponse<?>> historialCostoFamilia(@RequestBody Producto filtro) {
        exigirCodigoFamilia(filtro.getCodigoFamilia());
        return procesarLista(productoDao.listarHistorialCosto(filtro.getCodigoFamilia()),
                "La familia no tiene cambios de costo registrados.");
    }

    /**
     * Solo lo que la ficha escribe, con el usuario del token. Grupo y proveedor SAP son
     * opcionales: la app manda 0 cuando no hay, y 0 no es un id, asi que viaja NULL. Lo
     * mismo formato y gramaje vacios, que en toda la base estan en NULL.
     */
    private Producto familiaParaGuardar(Producto f, Authentication auth, boolean alta) {
        exigirCodigoFamilia(f.getCodigoFamilia());
        if (sinId(f.getIdPresentacion()) || sinId(f.getIdTipo()) || sinId(f.getIdRangoGram())
                || sinId(f.getIdColor())) {
            throw new SpBusinessException("Faltan datos de la familia: la presentación, el tipo, "
                    + "el rango de gramaje y el color son obligatorios.");
        }
        if (!alta && (f.getEstado() == null || (f.getEstado() != 0 && f.getEstado() != 1))) {
            throw new SpBusinessException("Debe indicar si la familia queda activa o inactiva.");
        }
        Producto p = new Producto();
        p.setCodigoFamilia(f.getCodigoFamilia());
        p.setIdGrpFamiliaSap(sinId(f.getIdGrpFamiliaSap()) ? null : f.getIdGrpFamiliaSap());
        p.setIdProveedorSap(sinId(f.getIdProveedorSap()) ? null : f.getIdProveedorSap());
        p.setIdPresentacion(f.getIdPresentacion());
        p.setIdTipo(f.getIdTipo());
        p.setIdRangoGram(f.getIdRangoGram());
        p.setIdColor(f.getIdColor());
        p.setFormato(textoONulo(f.getFormato()));
        p.setGramaje(textoONulo(f.getGramaje()));
        p.setEstado(alta ? null : f.getEstado());
        p.setAudUsuario(usuarioDelToken(auth));
        return p;
    }

    private static boolean sinId(Long id) {
        return id == null || id <= 0L;
    }

    private static String textoONulo(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    // ===================================================================== //
    //          PORCENTAJES POR FAMILIA Y LISTA DE PRECIOS (tpr_porcentaje)   //
    // ===================================================================== //

    /**
     * Grilla de porcentajes de una familia: una fila por sucursal y lista de precios, con
     * el porcentaje vigente y el {@code idPorcen} que despues se usa para modificar la fila.
     * Es el contenido del dialogo {@code dlgPorcen} del legacy.
     *
     * <p>Si la familia todavia no tiene porcentajes cargados el procedimiento devuelve la
     * misma grilla con {@code idPorcen} y {@code porcentaje} en 0: eso no es un error, es
     * un alta pendiente. La pantalla decide alta o modificacion por ese campo.
     *
     * @param filtro solo se usa {@code codigoFamilia}; es obligatorio
     */
    @PostMapping("/lstPorcentajeXFamilia")
    public ResponseEntity<ApiResponse<?>> lstPorcentajeXFamilia(@RequestBody Producto filtro) {
        exigirCodigoFamilia(filtro.getCodigoFamilia());

        return procesarLista(porcentajeDao.listarPorcentajesPorFamilia(filtro.getCodigoFamilia()),
                "No hay listas de precios activas para la familia indicada.");
    }

    /**
     * Grilla de destinos de la edicion masiva por grupo de familia SAP: las sucursales con
     * sus listas de precios activas, con el porcentaje en 0 para que el usuario cargue el
     * valor a aplicar. Es el grid del dialogo {@code dlgPorcGrupo}.
     *
     * <p><b>La grilla no depende del grupo</b> y aca no se simula lo contrario: la rama que
     * la devuelve no recibe el grupo y lista todas las clasificaciones activas, igual que
     * en el legacy. El grupo se exige igual porque sin el la edicion masiva no tiene
     * destino, y las familias afectadas —las mismas que se pueden excluir con un check—
     * salen de {@link #lstFamiliaXGrupo(FiltroIdDto)}.
     *
     * <p>El SP no ordena esta rama; el legacy ordenaba en memoria por {@code vpp} y luego
     * por sucursal. Ese orden lo tiene que aplicar la pantalla.
     *
     * @param filtro {@code id} = {@code idGrpFamiliaSap}; obligatorio
     */
    @PostMapping("/lstPorcentajeParaGrupo")
    public ResponseEntity<ApiResponse<?>> lstPorcentajeParaGrupo(@RequestBody FiltroIdDto filtro) {
        if (filtro.getId() <= 0L) {
            throw new SpBusinessException("Debe indicar el grupo de familia SAP.");
        }

        return procesarLista(porcentajeDao.listarSucursalesParaAbm(),
                "No hay listas de precios activas configuradas.");
    }

    /** Listas de precios que todavia no tienen porcentaje cargado para una familia. */
    @PostMapping("/lstPorcentajeFaltante")
    public ResponseEntity<ApiResponse<?>> lstPorcentajeFaltante(@RequestBody Producto filtro) {
        exigirCodigoFamilia(filtro.getCodigoFamilia());

        return procesarLista(porcentajeDao.listarPorcentajesFaltantes(filtro.getCodigoFamilia()),
                "La familia tiene todas sus listas de precios con porcentaje.");
    }

    /**
     * Filas crudas de {@code tpr_porcentaje} filtradas por familia y/o lista de precios.
     * A diferencia de {@link #lstPorcentajeXFamilia(Producto)} no arma la grilla ni
     * completa las listas faltantes: devuelve solo lo que existe en la tabla.
     *
     * @param filtro se usan {@code codigoFamilia} e {@code idClasificacion}; los campos en
     *               null no filtran
     */
    @PostMapping("/lstPorcentaje")
    public ResponseEntity<ApiResponse<?>> lstPorcentaje(@RequestBody Porcentaje filtro) {
        return procesarLista(
                porcentajeDao.listarPorcentajesPorFamiliaYClasificacion(
                        filtro.getCodigoFamilia(), filtro.getIdClasificacion()),
                "No existen porcentajes para el filtro indicado.");
    }

    /**
     * Alta o modificacion del porcentaje de una familia en una lista de precios.
     * {@code idPorcen} nulo o 0 inserta; mayor a 0 actualiza.
     *
     * <p>Una fila por llamada, que es lo que soporta el procedimiento: para guardar la
     * grilla completa la pantalla llama una vez por lista de precios, igual que hacia
     * {@code actualizaPorcen()} en el legacy.
     *
     * <p>En el alta hacen falta familia y lista de precios; en la modificacion el
     * procedimiento solo toca {@code porcen} y la auditoria, y no permite mover la fila a
     * otra familia ni a otra lista. El {@code audUsuario} sale del token: no se acepta del
     * cuerpo, aunque el model lo tenga como campo porque es una columna de la tabla.
     */
    @PostMapping("/registrarPorcentaje")
    public ResponseEntity<ApiResponse<?>> registrarPorcentaje(@RequestBody Porcentaje porcentaje,
                                                              Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.PORCENTAJES);
        boolean esAlta = porcentaje.getIdPorcen() == null || porcentaje.getIdPorcen() == 0L;

        if (porcentaje.getPorcen() == null) {
            throw new SpBusinessException("Debe indicar el porcentaje.");
        }
        if (esAlta) {
            exigirCodigoFamilia(porcentaje.getCodigoFamilia());
            if (porcentaje.getIdClasificacion() == null || porcentaje.getIdClasificacion() <= 0L) {
                throw new SpBusinessException("Debe indicar la lista de precios.");
            }
        }

        porcentaje.setAudUsuario(usuarioDelToken(auth));

        return respuestaEscritura(
                porcentajeDao.registrarPorcentaje(porcentaje, esAlta ? "I" : "U"));
    }

    // ===================================================================== //
    //                      ARMADO DE UNA PROPUESTA                          //
    // ===================================================================== //
    //
    // El asistente "Nueva propuesta": reemplaza a dlgNuevo, dlgProd, dlgVista y dlgArtD.
    // La logica vive en ArmadoPropuestaService; aca solo se resuelve quien opera y se
    // arma el envelope. Las escrituras son transaccionales en el servicio.

    /**
     * Fletes por sucursal para el paso de datos.
     *
     * @param filtro {@code id} = propuesta; 0 para un alta (devuelve las sucursales con
     *               el flete de la ultima propuesta por familia como sugerencia)
     */
    @PostMapping("/armado/fletes")
    public ResponseEntity<ApiResponse<?>> armadoFletes(@RequestBody FiltroIdDto filtro) {
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE,
                armado.fletes(filtro.getId()), HttpStatus.OK.value()));
    }

    /** Familias que ya estan en una propuesta, con su costo y sus listas propuestas. */
    @PostMapping("/armado/familias")
    public ResponseEntity<ApiResponse<?>> armadoFamilias(@RequestBody FiltroIdDto filtro) {
        return procesarLista(armado.familiasDePropuesta(filtro.getId()),
                "La propuesta todavía no tiene familias.");
    }

    /** Vista previa de una familia: precio vigente, guardado y calculado. No escribe. */
    @PostMapping("/armado/calcularFamilia")
    public ResponseEntity<ApiResponse<?>> armadoCalcularFamilia(@RequestBody ArmadoFamiliaDto dto) {
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE,
                armado.calcularFamilia(dto), HttpStatus.OK.value()));
    }

    /**
     * Carga una familia en la propuesta; si la propuesta no existe, la crea con sus
     * fletes. El precio lo vuelve a calcular el servidor: el cliente solo manda el costo.
     */
    @PostMapping("/armado/guardarFamilia")
    public ResponseEntity<ApiResponse<?>> armadoGuardarFamilia(@RequestBody ArmadoFamiliaDto dto,
                                                               Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Familia guardada en la propuesta.",
                        armado.guardarFamilia(dto, editorDe(auth)), HttpStatus.CREATED.value()));
    }

    /**
     * Vista previa de varias familias, cada una con su costo. No escribe; cada familia
     * vuelve con su grilla y, si no se podria guardar, con el motivo.
     */
    @PostMapping("/armado/calcularFamilias")
    public ResponseEntity<ApiResponse<?>> armadoCalcularFamilias(@RequestBody ArmadoLoteDto dto) {
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE,
                armado.calcularFamilias(dto), HttpStatus.OK.value()));
    }

    /**
     * Carga varias familias en la propuesta en una transaccion: si una no se puede guardar,
     * no se guarda ninguna. Si la propuesta no existe, la crea con sus fletes.
     */
    @PostMapping("/armado/guardarFamilias")
    public ResponseEntity<ApiResponse<?>> armadoGuardarFamilias(@RequestBody ArmadoLoteDto dto,
                                                                Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Familias guardadas en la propuesta.",
                        armado.guardarFamilias(dto, editorDe(auth)), HttpStatus.CREATED.value()));
    }

    /** Cambia los fletes de una propuesta pendiente y recalcula todas sus familias. */
    @PostMapping("/armado/guardarFletes")
    public ResponseEntity<ApiResponse<?>> armadoGuardarFletes(@RequestBody GuardarFletesDto dto,
                                                              Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Fletes guardados y precios recalculados.",
                        armado.guardarFletes(dto, editorDe(auth)), HttpStatus.CREATED.value()));
    }

    /** Articulos de una propuesta por articulo. */
    @PostMapping("/armado/articulos")
    public ResponseEntity<ApiResponse<?>> armadoArticulos(@RequestBody FiltroIdDto filtro) {
        return procesarLista(armado.articulosDePropuesta(filtro.getId()),
                "La propuesta todavía no tiene artículos.");
    }

    /** Agrega articulos a una propuesta por articulo, creandola si hace falta. */
    @PostMapping("/armado/agregarArticulos")
    public ResponseEntity<ApiResponse<?>> armadoAgregarArticulos(@RequestBody ArmadoArticulosDto dto,
                                                                 Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Artículos agregados a la propuesta.",
                        armado.agregarArticulos(dto, editorDe(auth)), HttpStatus.CREATED.value()));
    }

    /** Saca un articulo de una propuesta por articulo pendiente. */
    @PostMapping("/armado/quitarArticulo")
    public ResponseEntity<ApiResponse<?>> armadoQuitarArticulo(@RequestBody QuitarArticuloDto dto,
                                                               Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Artículo quitado de la propuesta.",
                        armado.quitarArticulo(dto, editorDe(auth)), HttpStatus.CREATED.value()));
    }

    /**
     * Trae de SAP los articulos nuevos y refresca stock y UTM
     * ({@code p_abm_ArticuloProp 'F'}). Puede tardar varios minutos.
     */
    @PostMapping("/armado/sincronizarArticulosSap")
    public ResponseEntity<ApiResponse<?>> armadoSincronizarArticulosSap(Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.PROPUESTAS);
        return respuestaEscritura(armado.sincronizarArticulosSap(usuarioDelToken(auth)));
    }

    /**
     * Quien arma, desde el token. El boton {@code btnPen} solo se consulta si la propuesta
     * resulta ser de otra persona: cuesta dos consultas y casi nunca hace falta.
     */
    private ArmadoPropuestaService.Editor editorDe(Authentication auth) {
        // Todas las escrituras del asistente pasan por aca: primero, la pantalla.
        pantallas.exigir(auth, AccesoPantallaPrecios.PROPUESTAS);
        DatosToken token = DatosToken.de(auth);
        return new ArmadoPropuestaService.Editor(token.getCodUsuario(), token.getCodEmpresa(),
                () -> acceso.tieneBoton(auth, VISTA_PRECIOS, BTN_EN_ESPERA));
    }

    // ===================================================================== //
    //                      CIRCUITO DE AUTORIZACION                         //
    // ===================================================================== //

    /**
     * Aprueba o rechaza una propuesta. Es la operacion mas sensible del modulo: al
     * aprobar una propuesta POR FAMILIA, los precios propuestos pasan a ser los precios
     * de venta vigentes de toda la empresa.
     *
     * <p>Una propuesta POR ARTICULO no cambia ningun precio: los articulos nuevos toman el
     * vigente de su familia. Al aprobarla solo se registra la autorizacion, y
     * {@code p_abm_autorizacion 'A'} guarda la bitacora ({@code tpr_bitArticulo}). Es lo
     * que hacia el JSF ({@code WizardProductoNew.actualizarAuto} solo aplica precios si
     * {@code tipo == 1}).
     *
     * <p>Las dos escrituras van en una sola transaccion. Ningun procedimiento del modulo
     * abre transaccion propia, asi que sin esto un fallo al aplicar los precios dejaba la
     * autorizacion registrada sin que los precios cambiaran, o al reves.
     *
     * @param dto  propuesta y estado destino
     * @param auth usuario autenticado; de aca sale quien aprueba, nunca del cuerpo
     */
    @PostMapping("/resolverPropuesta")
    @Transactional
    public ResponseEntity<ApiResponse<?>> resolverPropuesta(@RequestBody ResolverPropuestaDto dto,
                                                            Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.PROPUESTAS);
        acceso.exigirBoton(auth, VISTA_PRECIOS, BTN_APROBAR);
        exigirPropuesta(dto.getIdPropuesta());
        if (dto.getEsAprobada() == null) {
            throw new SpBusinessException("Debe indicar si la propuesta se aprueba o se rechaza.");
        }

        long audUsuario = usuarioDelToken(auth);

        RespuestaSp res = autDao.resolverPropuesta(dto.getIdPropuesta(), dto.getEsAprobada(),
                audUsuario);

        // Solo la aprobacion de una propuesta por familia vuelca los precios propuestos
        // sobre los vigentes (tpr_precio) y el costo sobre la familia (tpr_producto).
        if (dto.getEsAprobada() == ESTADO_APROBADA) {
            Integer tipo = propuestaDao.obtenerTipo(dto.getIdPropuesta());
            if (tipo != null && tipo == TIPO_POR_ARTICULO) {
                log.info("Propuesta {} por articulo aprobada por el usuario {}: solo bitacora, sin tocar precios.",
                        dto.getIdPropuesta(), audUsuario);
            } else {
                precioPropuestaDao.aplicarPreciosDePropuesta(dto.getIdPropuesta());
                log.info("Propuesta {} aprobada por el usuario {}: precios aplicados.",
                        dto.getIdPropuesta(), audUsuario);
            }
        }

        return respuestaEscritura(res);
    }

    /**
     * Manda la propuesta a autorizar: pasa a En Espera y queda a la vista de quien aprueba.
     *
     * <p>Solo desde Pendiente. Una propuesta Aprobada o No Aprobada ya salio del circuito:
     * el rechazo es definitivo y quien la propuso arma una nueva. El JSF dejaba reenviar
     * una rechazada ({@code esAprobada != 1 && != 3}); se cerro el 2026-09-28 a pedido del
     * usuario, porque una propuesta rechazada volvia a quedar En Espera.
     */
    @PostMapping("/marcarEnEspera")
    public ResponseEntity<ApiResponse<?>> marcarEnEspera(@RequestBody FiltroIdDto filtro,
                                                         Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.PROPUESTAS);
        acceso.exigirBoton(auth, VISTA_PRECIOS, BTN_EN_ESPERA);
        exigirPropuesta(filtro.getId());

        Integer estado = null;
        for (Autorizacion a : autDao.listarAutorizacion(null, filtro.getId(), null)) {
            if (a.getEsAprobada() != null) estado = a.getEsAprobada();
        }
        if (estado != null && estado == ESTADO_APROBADA) {
            throw new SpBusinessException("La propuesta " + filtro.getId()
                    + " ya está aprobada: no vuelve a autorizarse.");
        }
        if (estado != null && estado == ESTADO_RECHAZADA) {
            throw new SpBusinessException("La propuesta " + filtro.getId()
                    + " fue rechazada: no vuelve a autorizarse. Arme una propuesta nueva.");
        }
        if (estado != null && estado == ESTADO_EN_ESPERA) {
            throw new SpBusinessException("La propuesta " + filtro.getId()
                    + " ya está En Espera de autorización.");
        }

        return respuestaEscritura(autDao.marcarEnEspera(filtro.getId()));
    }

    /**
     * Genera una propuesta aprobada: devuelve CambioDePrecios.xlsx (los precios unitarios
     * por lista de SAP, para cargarlos en las empresas) y deja constancia de quien lo
     * genero y cuando. Como el JSF: solo con el boton {@code btnGen} y solo aprobada.
     *
     * <p>Reemplaza a {@code /registrarGeneracion}, que marcaba la propuesta sin darle el
     * archivo a nadie. El xlsx va crudo, sin ApiResponse, como los PDF; un error de
     * negocio sale como 400 con el JSON de siempre.
     */
    @PostMapping("/generarPropuesta")
    public ResponseEntity<byte[]> generarPropuesta(@RequestBody FiltroIdDto filtro, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.PROPUESTAS);
        acceso.exigirBoton(auth, VISTA_PRECIOS, BTN_GENERAR);
        exigirPropuesta(filtro.getId());

        return xlsx(generacion.generar(filtro.getId(), usuarioDelToken(auth)));
    }

    // ===================================================================== //
    //                               REPORTES                                //
    // ===================================================================== //
    //
    // PDF crudo (application/pdf), sin ApiResponse, como el resto de los reportes del
    // backend: el nombre del archivo lo pone el front. Un error de negocio (propuesta
    // inexistente, nada que imprimir) sale como 400 con el JSON de siempre.

    /**
     * La propuesta en PDF: por familia (tipo 1) o por articulo (tipo 2). Reemplaza a
     * rptPropuArt y RptArtPropuPorArticulo del JSF. Puede tardar: los articulos no
     * creados y las familias no actualizadas se consultan en otro servidor.
     */
    @PostMapping("/reporte/propuesta")
    public ResponseEntity<byte[]> reportePropuesta(@RequestBody FiltroIdDto filtro) {
        exigirPropuesta(filtro.getId());
        return pdf(reportes.propuesta(filtro.getId()));
    }

    /** Precios por tonelada vigentes de un grupo de familia SAP (RptPrecioActFam del JSF). */
    @PostMapping("/reporte/preciosGrupo")
    public ResponseEntity<byte[]> reportePreciosGrupo(@RequestBody FiltroIdDto filtro) {
        if (filtro.getId() <= 0L) {
            throw new SpBusinessException("Debe indicar el grupo de familia SAP.");
        }
        return pdf(reportes.preciosPorGrupo(filtro.getId()));
    }

    /** Precios por tonelada vigentes de todas las familias activas (RptTodoPrecioFam). */
    @PostMapping("/reporte/preciosTodas")
    public ResponseEntity<byte[]> reportePreciosTodas() {
        return pdf(reportes.preciosTodas());
    }

    /** Catalogo de familias activas (RptFamiliasActivas del JSF). */
    @PostMapping("/reporte/familiasActivas")
    public ResponseEntity<byte[]> reporteFamiliasActivas() {
        return pdf(reportes.familiasActivas());
    }

    // ===================================================================== //
    //                               APOYO                                   //
    // ===================================================================== //

    private static ResponseEntity<byte[]> pdf(byte[] pdf) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private static ResponseEntity<byte[]> xlsx(byte[] xlsx) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentLength(xlsx.length);
        return new ResponseEntity<>(xlsx, headers, HttpStatus.OK);
    }

    /** Usuario autenticado, como {@code long} para los campos {@code audUsuario}. */
    private long usuarioDelToken(Authentication auth) {
        return DatosToken.codUsuarioDe(auth);
    }

    /** Falla con 400 si la peticion no trae la propuesta sobre la que operar. */
    private void exigirPropuesta(Long idPropuesta) {
        if (idPropuesta == null || idPropuesta <= 0L) {
            throw new SpBusinessException("Debe indicar la propuesta sobre la que desea operar.");
        }
    }

    /**
     * Falla con 400 si la peticion no trae la familia sobre la que consultar.
     *
     * <p><b>El 0 es una familia.</b> {@code tpr_producto} tiene la familia 0 activa, con
     * precios en 21 listas y propuestas aprobadas en el legacy (la ultima, la 207). Los
     * procedimientos la filtran por igualdad: su comodin es NULL, no 0. Rechazarla dejaba
     * sin repreciar una familia real.
     */
    private void exigirCodigoFamilia(Integer codigoFamilia) {
        if (codigoFamilia == null || codigoFamilia < 0) {
            throw new SpBusinessException("Debe indicar el código de familia.");
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
