package bo.bosque.com.impexpap.controller;

import java.util.List;

import bo.bosque.com.impexpap.commons.AccesoPantallaPrecios;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IClasificacionPrecio;
import bo.bosque.com.impexpap.dao.IColor;
import bo.bosque.com.impexpap.dao.ICostoIvaIt;
import bo.bosque.com.impexpap.dao.IGrupoFamTipoRangoGram;
import bo.bosque.com.impexpap.dao.IGrupoFamiliaSap;
import bo.bosque.com.impexpap.dao.IPresentacion;
import bo.bosque.com.impexpap.dao.IProveedorExtSap;
import bo.bosque.com.impexpap.dao.IRangoGramaje;
import bo.bosque.com.impexpap.dao.ITcAncla;
import bo.bosque.com.impexpap.dao.ITipo;
import bo.bosque.com.impexpap.dto.CostoIvaItDto;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.model.ClasificacionPrecio;
import bo.bosque.com.impexpap.model.Color;
import bo.bosque.com.impexpap.model.CostoIvaIt;
import bo.bosque.com.impexpap.model.GrupoFamTipoRangoGram;
import bo.bosque.com.impexpap.model.GrupoFamiliaSap;
import bo.bosque.com.impexpap.model.Presentacion;
import bo.bosque.com.impexpap.model.ProveedorExtSap;
import bo.bosque.com.impexpap.model.RangoGramaje;
import bo.bosque.com.impexpap.model.TcAncla;
import bo.bosque.com.impexpap.model.Tipo;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogos del modulo de Precios (tpr) — las diez tablas de apoyo que alimentan las
 * pantallas de propuestas y repreciacion: color, tipo, presentacion, rango de gramaje,
 * grupo de familia SAP, proveedor externo SAP, la tabla puente de parametros de gramaje,
 * las listas de precios, los impuestos IVA/IT y el ancla del tipo de cambio.
 * <p>
 * Los DAO y sus interfaces ya existian y compilaban, pero ningun controlador los
 * consumia: eran codigo muerto y el cliente Flutter no tenia forma de llenar un solo
 * combo. Este controlador es la unica puerta de entrada a esos diez catalogos.
 * <p>
 * Toda la persistencia pasa por procedimientos almacenados a traves de los DAO; no hay
 * SQL escrito en Java. Como en el resto del backend, todos los endpoints son POST
 * —incluidas las lecturas— y responden con el envelope {@code {message, data, status}}:
 * 201 en una escritura aceptada, 200 en una lectura con datos, 204 cuando no hay nada
 * que mostrar y 400 cuando el procedimiento devuelve un error de negocio.
 *
 * <h2>Criterios que sigue este controlador</h2>
 * <ol>
 *   <li><b>El {@code audUsuario} nunca se acepta del cuerpo.</b> Sale siempre de
 *       {@link DatosToken#codUsuarioDe(Authentication)} y se pisa sobre el modelo antes
 *       de llamar al DAO. Es la misma vulnerabilidad que ya se corrigio en
 *       {@code PrecioController}: si el cliente elige el usuario de auditoria, la
 *       auditoria no sirve para nada. Tampoco se usa como criterio de busqueda en los
 *       listados filtrados, para que no haya ninguna via por la que ese campo entre
 *       desde la peticion.</li>
 *   <li><b>Alta contra modificacion se decide en el servidor</b>, por la PK que trae el
 *       modelo: nula o 0 inserta, mayor a 0 actualiza. La unica excepcion es
 *       {@code /grupo-fam-tipo-rango}, que no tiene PK; ver alli.</li>
 *   <li><b>Las bajas exigen el identificador</b> antes de llegar al procedimiento, para
 *       que un cuerpo incompleto devuelva un mensaje legible y no un error del motor.</li>
 *   <li><b>{@code /tc-ancla} es de solo lectura</b> aunque su DAO tenga escrituras, y
 *       {@code /costo-iva-it} prefiere actualizar antes que insertar. Los dos casos
 *       estan explicados en el javadoc de sus bloques.</li>
 * </ol>
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/price/catalogo")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class CatalogoPreciosController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    private final IColor colorDao;
    private final ITipo tipoDao;
    private final IPresentacion presentacionDao;
    private final IRangoGramaje rangoGramajeDao;
    private final IGrupoFamiliaSap grupoFamiliaSapDao;
    private final IProveedorExtSap proveedorExtSapDao;
    private final IGrupoFamTipoRangoGram grupoFamTipoRangoGramDao;
    private final IClasificacionPrecio clasificacionPrecioDao;
    private final ICostoIvaIt costoIvaItDao;
    private final ITcAncla tcAnclaDao;
    private final AccesoPantallaPrecios pantallas;

    public CatalogoPreciosController(IColor colorDao,
                                     ITipo tipoDao,
                                     IPresentacion presentacionDao,
                                     IRangoGramaje rangoGramajeDao,
                                     IGrupoFamiliaSap grupoFamiliaSapDao,
                                     IProveedorExtSap proveedorExtSapDao,
                                     IGrupoFamTipoRangoGram grupoFamTipoRangoGramDao,
                                     IClasificacionPrecio clasificacionPrecioDao,
                                     ICostoIvaIt costoIvaItDao,
                                     ITcAncla tcAnclaDao,
                                     AccesoPantallaPrecios pantallas) {
        this.colorDao = colorDao;
        this.tipoDao = tipoDao;
        this.presentacionDao = presentacionDao;
        this.rangoGramajeDao = rangoGramajeDao;
        this.grupoFamiliaSapDao = grupoFamiliaSapDao;
        this.proveedorExtSapDao = proveedorExtSapDao;
        this.grupoFamTipoRangoGramDao = grupoFamTipoRangoGramDao;
        this.clasificacionPrecioDao = clasificacionPrecioDao;
        this.costoIvaItDao = costoIvaItDao;
        this.tcAnclaDao = tcAnclaDao;
        this.pantallas = pantallas;
    }

    // ===================================================================== //
    //                                COLOR                                  //
    // ===================================================================== //

    /** Colores del catalogo, activos e inactivos. {@code id} en 0 devuelve todos. */
    @PostMapping("/color/listar")
    public ResponseEntity<ApiResponse<?>> listarColores(@RequestBody FiltroIdDto filtro) {
        return procesarLista(colorDao.obtenerColores(filtro.getId()),
                "No existen colores registrados.");
    }

    /** Solo los colores activos, con id y nombre: es la lista que alimenta los combos. */
    @PostMapping("/color/activos")
    public ResponseEntity<ApiResponse<?>> listarColoresActivos() {
        return procesarLista(colorDao.obtenerColoresActivos(), "No hay colores activos.");
    }

    /** Alta o modificacion de un color. {@code idColor} nulo o 0 inserta; mayor a 0 actualiza. */
    @PostMapping("/color/registrar")
    public ResponseEntity<ApiResponse<?>> registrarColor(@RequestBody Color color,
                                                         Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        color.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(colorDao.registrarColor(color, accion(color.getIdColor())));
    }

    /** Baja de un color. El procedimiento la rechaza si alguna familia lo usa. */
    @PostMapping("/color/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarColor(@RequestBody Color color,
                                                        Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        exigirId(color.getIdColor(), "el color");
        color.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(colorDao.registrarColor(color, "D"));
    }

    // ===================================================================== //
    //                                 TIPO                                  //
    // ===================================================================== //

    /** Tipos de papel, activos e inactivos. {@code id} en 0 devuelve todos. */
    @PostMapping("/tipo/listar")
    public ResponseEntity<ApiResponse<?>> listarTipos(@RequestBody FiltroIdDto filtro) {
        return procesarLista(tipoDao.obtenerTipos(filtro.getId()),
                "No existen tipos registrados.");
    }

    /** Solo los tipos activos, con id y nombre, para los combos. */
    @PostMapping("/tipo/activos")
    public ResponseEntity<ApiResponse<?>> listarTiposActivos() {
        return procesarLista(tipoDao.obtenerTiposActivos(), "No hay tipos activos.");
    }

    /** Alta o modificacion de un tipo. {@code idTipo} nulo o 0 inserta; mayor a 0 actualiza. */
    @PostMapping("/tipo/registrar")
    public ResponseEntity<ApiResponse<?>> registrarTipo(@RequestBody Tipo tipo,
                                                        Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        tipo.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(tipoDao.registrarTipo(tipo, accion(tipo.getIdTipo())));
    }

    /** Baja de un tipo. El procedimiento la rechaza si esta en uso. */
    @PostMapping("/tipo/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarTipo(@RequestBody Tipo tipo,
                                                       Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        exigirId(tipo.getIdTipo(), "el tipo");
        tipo.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(tipoDao.registrarTipo(tipo, "D"));
    }

    // ===================================================================== //
    //                             PRESENTACION                              //
    // ===================================================================== //

    /** Catalogo completo de presentaciones, activas e inactivas. */
    @PostMapping("/presentacion/listar")
    public ResponseEntity<ApiResponse<?>> listarPresentaciones() {
        return procesarLista(presentacionDao.listarPresentaciones(),
                "No existen presentaciones registradas.");
    }

    /**
     * Presentaciones filtradas. Los campos del cuerpo que lleguen en null no filtran.
     *
     * <p>El {@code audUsuario} del cuerpo <b>se ignora a proposito</b>: el DAO admite
     * filtrar por el, pero ninguna pantalla lo necesita y dejar ese campo entrar desde la
     * peticion es exactamente el habito que se quiere evitar en este modulo.
     */
    @PostMapping("/presentacion/buscar")
    public ResponseEntity<ApiResponse<?>> buscarPresentaciones(
            @RequestBody Presentacion filtro) {
        return procesarLista(presentacionDao.listarPresentaciones(filtro.getIdPresentacion(),
                        filtro.getPresentacion(), filtro.getEstado(), null),
                "No existen presentaciones para el filtro indicado.");
    }

    /** Presentaciones activas, solo id y nombre: es la lista de los combos de producto. */
    @PostMapping("/presentacion/activas")
    public ResponseEntity<ApiResponse<?>> listarPresentacionesActivas() {
        return procesarLista(presentacionDao.listarActivas(), "No hay presentaciones activas.");
    }

    /** Una presentacion por su id. */
    @PostMapping("/presentacion/obtener")
    public ResponseEntity<ApiResponse<?>> obtenerPresentacion(@RequestBody FiltroIdDto filtro) {
        exigirId(filtro.getId(), "la presentación");
        return procesarUno(presentacionDao.obtenerPorId(filtro.getId()),
                "La presentación no existe.");
    }

    /** Alta o modificacion. {@code idPresentacion} nulo o 0 inserta; mayor a 0 actualiza. */
    @PostMapping("/presentacion/registrar")
    public ResponseEntity<ApiResponse<?>> registrarPresentacion(
            @RequestBody Presentacion presentacion, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        presentacion.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(presentacionDao.registrarPresentacion(presentacion,
                accion(presentacion.getIdPresentacion())));
    }

    /** Baja de una presentacion. El procedimiento la rechaza si hay familias que la usan. */
    @PostMapping("/presentacion/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarPresentacion(
            @RequestBody Presentacion presentacion, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        exigirId(presentacion.getIdPresentacion(), "la presentación");
        presentacion.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(presentacionDao.registrarPresentacion(presentacion, "D"));
    }

    // ===================================================================== //
    //                           RANGO DE GRAMAJE                            //
    // ===================================================================== //

    /**
     * Catalogo de rangos de gramaje, crudo, ordenado por limite inferior.
     *
     * <p>{@code min} y {@code max} son {@code decimal(16,2)}: viajan como numeros
     * exactos de punta a punta. Un rango 80,50 es 80,50 y no 80.
     */
    @PostMapping("/rango-gramaje/listar")
    public ResponseEntity<ApiResponse<?>> listarRangosGramaje() {
        return procesarLista(rangoGramajeDao.listarRangoGramaje(),
                "No existen rangos de gramaje registrados.");
    }

    /** Un rango de gramaje por su id. */
    @PostMapping("/rango-gramaje/obtener")
    public ResponseEntity<ApiResponse<?>> obtenerRangoGramaje(@RequestBody FiltroIdDto filtro) {
        exigirId(filtro.getId(), "el rango de gramaje");
        return procesarUno(rangoGramajeDao.obtenerPorId(filtro.getId()),
                "El rango de gramaje no existe.");
    }

    /** Los mismos rangos con la etiqueta «[ min - max ]» ya armada, para combos. */
    @PostMapping("/rango-gramaje/combo")
    public ResponseEntity<ApiResponse<?>> listarRangosGramajeParaCombo() {
        return procesarLista(rangoGramajeDao.listarParaCombo(),
                "No existen rangos de gramaje registrados.");
    }

    /**
     * Rangos configurados para un grupo de familia SAP y un tipo de papel.
     *
     * <p>El cuerpo es la tabla puente porque el par que se consulta es justamente su clave
     * natural. Los dos ids son obligatorios: esta rama del procedimiento no admite «sin
     * filtro», con un null devuelve vacio y el vacio no se distinguiria de «no hay
     * configuracion».
     */
    @PostMapping("/rango-gramaje/por-grupo-familia-tipo")
    public ResponseEntity<ApiResponse<?>> listarRangosPorGrupoFamiliaYTipo(
            @RequestBody GrupoFamTipoRangoGram filtro) {
        exigirClaveNatural(filtro);
        return procesarLista(rangoGramajeDao.listarPorGrupoFamiliaYTipo(
                        aLong(filtro.getIdGrpFamiliaSap()), aLong(filtro.getIdTipo())),
                "Ese grupo de familia y tipo no tienen rangos configurados.");
    }

    /** Alta o modificacion. {@code idRangoGram} nulo o 0 inserta; mayor a 0 actualiza. */
    @PostMapping("/rango-gramaje/registrar")
    public ResponseEntity<ApiResponse<?>> registrarRangoGramaje(
            @RequestBody RangoGramaje rangoGramaje, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        rangoGramaje.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(rangoGramajeDao.registrarRangoGramaje(rangoGramaje,
                accion(rangoGramaje.getIdRangoGram())));
    }

    /** Baja de un rango. El procedimiento la rechaza si el rango esta en uso. */
    @PostMapping("/rango-gramaje/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarRangoGramaje(
            @RequestBody RangoGramaje rangoGramaje, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        exigirId(rangoGramaje.getIdRangoGram(), "el rango de gramaje");
        rangoGramaje.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(rangoGramajeDao.registrarRangoGramaje(rangoGramaje, "D"));
    }

    // ===================================================================== //
    //                        GRUPO DE FAMILIA SAP                           //
    // ===================================================================== //

    /** Todos los grupos de familia SAP con su equivalencia de codigo por empresa. */
    @PostMapping("/grupo-familia-sap/listar")
    public ResponseEntity<ApiResponse<?>> listarGruposFamiliaSap() {
        return procesarLista(grupoFamiliaSapDao.listar(),
                "No existen grupos de familia registrados.");
    }

    /** Un grupo de familia SAP por su id. */
    @PostMapping("/grupo-familia-sap/obtener")
    public ResponseEntity<ApiResponse<?>> obtenerGrupoFamiliaSap(@RequestBody FiltroIdDto filtro) {
        exigirId(filtro.getId(), "el grupo de familia");
        return procesarLista(grupoFamiliaSapDao.obtenerPorId(filtro.getId()),
                "El grupo de familia no existe.");
    }

    /** Busqueda por los campos no nulos del cuerpo; los que lleguen en null no filtran. */
    @PostMapping("/grupo-familia-sap/buscar")
    public ResponseEntity<ApiResponse<?>> buscarGruposFamiliaSap(
            @RequestBody GrupoFamiliaSap filtro) {
        return procesarLista(grupoFamiliaSapDao.buscar(filtro),
                "No existen grupos de familia para el filtro indicado.");
    }

    /**
     * Alta o modificacion de un grupo de familia SAP.
     *
     * <p>Al modificar, un codigo por empresa que llegue en null significa «no tocar»: el
     * procedimiento lo resuelve con {@code ISNULL(@param, columna)}. Para limpiar una de
     * esas columnas hay que mandar cadena vacia, no null.
     */
    @PostMapping("/grupo-familia-sap/registrar")
    public ResponseEntity<ApiResponse<?>> registrarGrupoFamiliaSap(
            @RequestBody GrupoFamiliaSap grupo, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        grupo.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(esAlta(grupo.getIdGrpFamiliaSap())
                ? grupoFamiliaSapDao.registrar(grupo)
                : grupoFamiliaSapDao.actualizar(grupo));
    }

    /** Baja de un grupo de familia SAP. */
    @PostMapping("/grupo-familia-sap/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarGrupoFamiliaSap(@RequestBody FiltroIdDto filtro,
                                                                  Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        exigirId(filtro.getId(), "el grupo de familia");
        return respuestaEscritura(grupoFamiliaSapDao.eliminar(filtro.getId()));
    }

    // ===================================================================== //
    //                      PROVEEDOR EXTERNO SAP                            //
    // ===================================================================== //

    /** Todos los proveedores externos SAP. */
    @PostMapping("/proveedor-sap/listar")
    public ResponseEntity<ApiResponse<?>> listarProveedores() {
        return procesarLista(proveedorExtSapDao.listarProveedores(),
                "No existen proveedores registrados.");
    }

    /**
     * Proveedores filtrados; los campos en null no filtran. El {@code codProvExtSap} es
     * texto, no numero: admite letras y ceros a la izquierda.
     *
     * <p>Igual que en presentaciones, el {@code audUsuario} del cuerpo se ignora.
     */
    @PostMapping("/proveedor-sap/buscar")
    public ResponseEntity<ApiResponse<?>> buscarProveedores(@RequestBody ProveedorExtSap filtro) {
        return procesarLista(proveedorExtSapDao.listarProveedores(filtro.getIdProveedorSap(),
                        filtro.getCodProvExtSap(), filtro.getProveedorExtSap(), null),
                "No existen proveedores para el filtro indicado.");
    }

    /** Un proveedor externo SAP por su id. */
    @PostMapping("/proveedor-sap/obtener")
    public ResponseEntity<ApiResponse<?>> obtenerProveedor(@RequestBody FiltroIdDto filtro) {
        exigirId(filtro.getId(), "el proveedor");
        return procesarUno(proveedorExtSapDao.obtenerPorId(filtro.getId()),
                "El proveedor no existe.");
    }

    /** Alta o modificacion. {@code idProveedorSap} nulo o 0 inserta; mayor a 0 actualiza. */
    @PostMapping("/proveedor-sap/registrar")
    public ResponseEntity<ApiResponse<?>> registrarProveedor(@RequestBody ProveedorExtSap proveedor,
                                                             Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        proveedor.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(proveedorExtSapDao.registrarProveedorExtSap(proveedor,
                accion(proveedor.getIdProveedorSap())));
    }

    /** Baja de un proveedor. El procedimiento la rechaza si alguna familia lo referencia. */
    @PostMapping("/proveedor-sap/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarProveedor(@RequestBody ProveedorExtSap proveedor,
                                                            Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.CATALOGOS);
        exigirId(proveedor.getIdProveedorSap(), "el proveedor");
        proveedor.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(proveedorExtSapDao.registrarProveedorExtSap(proveedor, "D"));
    }

    // ===================================================================== //
    //          PARAMETROS DE GRAMAJE (grupo de familia x tipo)              //
    // ===================================================================== //
    //
    // Esta tabla es un HEAP: no tiene PRIMARY KEY ni columna IDENTITY. La fila se
    // identifica por su clave natural (idGrpFamiliaSap, idTipo), asi que ni la baja ni la
    // modificacion pueden pedir un id —no existe—, y el alta nunca devuelve uno: el
    // idGenerado vuelve en 0 aunque la insercion haya salido bien.

    /** Todas las asignaciones cargadas, crudas. */
    @PostMapping("/grupo-fam-tipo-rango/listar")
    public ResponseEntity<ApiResponse<?>> listarGrupoFamTipoRango() {
        return procesarLista(grupoFamTipoRangoGramDao.listarGrupoFamTipoRangoGram(),
                "No existen parámetros de gramaje registrados.");
    }

    /** Las asignaciones de un grupo de familia, una por tipo de papel. */
    @PostMapping("/grupo-fam-tipo-rango/por-grupo-familia")
    public ResponseEntity<ApiResponse<?>> listarGrupoFamTipoRangoPorGrupo(
            @RequestBody FiltroIdDto filtro) {
        exigirId(filtro.getId(), "el grupo de familia");
        return procesarLista(grupoFamTipoRangoGramDao.obtenerPorGrupoFamilia(filtro.getId()),
                "El grupo de familia no tiene rangos asignados.");
    }

    /** La fila que corresponde a la clave natural {@code (idGrpFamiliaSap, idTipo)}. */
    @PostMapping("/grupo-fam-tipo-rango/obtener")
    public ResponseEntity<ApiResponse<?>> obtenerGrupoFamTipoRango(
            @RequestBody GrupoFamTipoRangoGram filtro) {
        exigirClaveNatural(filtro);
        return procesarUno(grupoFamTipoRangoGramDao.obtenerPorClaveNatural(
                        aLong(filtro.getIdGrpFamiliaSap()), aLong(filtro.getIdTipo())),
                "Ese grupo de familia y tipo todavía no están configurados.");
    }

    /** La tabla pivoteada: un renglon por grupo de familia con sus columnas Liviano / Mediano / Pesado. */
    @PostMapping("/grupo-fam-tipo-rango/parametros-gramaje")
    public ResponseEntity<ApiResponse<?>> listarParametrosGramaje() {
        return procesarLista(grupoFamTipoRangoGramDao.listarParametrosGramaje(),
                "No existen parámetros de gramaje registrados.");
    }

    /**
     * Alta o modificacion de una asignacion, identificada por la clave natural
     * {@code (idGrpFamiliaSap, idTipo)}.
     *
     * <p>Al no haber PK, el cliente no puede mandar un id que diga si esto es un alta o
     * una modificacion, y tampoco se le puede creer si lo dijera. La decision se toma en
     * el servidor: se consulta la clave natural y, si la fila ya existe, va 'U'; si no,
     * 'I'. Asi un segundo alta del mismo par no duplica la fila en una tabla que no tiene
     * unique que lo impida.
     */
    @PostMapping("/grupo-fam-tipo-rango/registrar")
    public ResponseEntity<ApiResponse<?>> registrarGrupoFamTipoRango(
            @RequestBody GrupoFamTipoRangoGram asignacion, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.PARAMETROS);
        exigirClaveNatural(asignacion);
        if (asignacion.getIdRangoGram() == null) {
            throw new SpBusinessException("Debe indicar el rango de gramaje que desea asignar.");
        }
        asignacion.setAudUsuario(usuarioDelToken(auth));

        boolean existe = grupoFamTipoRangoGramDao.obtenerPorClaveNatural(
                aLong(asignacion.getIdGrpFamiliaSap()), aLong(asignacion.getIdTipo())) != null;

        return respuestaEscritura(grupoFamTipoRangoGramDao.registrarGrupoFamTipoRangoGram(
                asignacion, existe ? "U" : "I"));
    }

    /** Baja de una asignacion, identificada tambien por su clave natural. */
    @PostMapping("/grupo-fam-tipo-rango/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarGrupoFamTipoRango(
            @RequestBody GrupoFamTipoRangoGram asignacion, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.PARAMETROS);
        exigirClaveNatural(asignacion);
        asignacion.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(
                grupoFamTipoRangoGramDao.registrarGrupoFamTipoRangoGram(asignacion, "D"));
    }

    // ===================================================================== //
    //                    CLASIFICACION DE PRECIO (listas)                   //
    // ===================================================================== //

    /**
     * Listado plano de listas de precios. Los filtros del cuerpo en null o 0 no filtran.
     */
    @PostMapping("/clasificacion-precio/listar")
    public ResponseEntity<ApiResponse<?>> listarClasificacionPrecio(
            @RequestBody ClasificacionPrecio filtro) {
        return procesarLista(clasificacionPrecioDao.obtenerClasificacionPrecio(
                        filtro.getIdClasificacion(), filtro.getCodSucursal()),
                "No existen listas de precios registradas.");
    }

    /** Listas de precios con el nombre de su sucursal, ordenadas por estado y vpp. */
    @PostMapping("/clasificacion-precio/con-sucursal")
    public ResponseEntity<ApiResponse<?>> listarClasificacionPrecioConSucursal() {
        return procesarLista(clasificacionPrecioDao.obtenerClasificacionPrecioConSucursal(),
                "No existen listas de precios registradas.");
    }

    /** Los vpp ya usados. Sirve para proponer el siguiente y para validar en pantalla. */
    @PostMapping("/clasificacion-precio/vpps")
    public ResponseEntity<ApiResponse<?>> listarVpps() {
        return procesarLista(clasificacionPrecioDao.obtenerVpps(),
                "No hay ningún vpp registrado.");
    }

    /**
     * Indica si el vpp ya esta usado por otra lista de precios.
     *
     * <p>Al editar, el {@code idClasificacion} del cuerpo se excluye de la verificacion:
     * una lista no choca consigo misma. Devuelve siempre 200 con un booleano; que el vpp
     * exista no es un error, es la respuesta.
     */
    @PostMapping("/clasificacion-precio/existe-vpp")
    public ResponseEntity<ApiResponse<?>> existeVpp(@RequestBody ClasificacionPrecio filtro) {
        if (filtro.getVpp() == null) {
            throw new SpBusinessException("Debe indicar el vpp que desea verificar.");
        }
        boolean existe = clasificacionPrecioDao.existeVpp(filtro.getVpp(),
                filtro.getIdClasificacion());
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, existe, HttpStatus.OK.value()));
    }

    /** Alta o modificacion. {@code idClasificacion} nulo o 0 inserta; mayor a 0 actualiza. */
    @PostMapping("/clasificacion-precio/registrar")
    public ResponseEntity<ApiResponse<?>> registrarClasificacionPrecio(
            @RequestBody ClasificacionPrecio clasificacion, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.LISTAS);
        clasificacion.setAudUsuario(usuarioDelToken(auth));
        return respuestaEscritura(clasificacionPrecioDao.registrarClasificacionPrecio(
                clasificacion, accion(clasificacion.getIdClasificacion())));
    }

    /**
     * Activa o desactiva una lista de precios sin tocar el resto de los campos.
     *
     * <p>Es una operacion aparte y no un {@code registrar} con el estado cambiado, porque
     * la rama del procedimiento que usa solo escribe estado y auditoria: no hay forma de
     * pisar sin querer el nombre ni el vpp.
     */
    @PostMapping("/clasificacion-precio/cambiar-estado")
    public ResponseEntity<ApiResponse<?>> cambiarEstadoClasificacionPrecio(
            @RequestBody ClasificacionPrecio clasificacion, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.LISTAS);
        exigirId(clasificacion.getIdClasificacion(), "la lista de precios");
        if (clasificacion.getEstado() == null) {
            throw new SpBusinessException("Debe indicar si la lista de precios queda activa o no.");
        }
        return respuestaEscritura(clasificacionPrecioDao.cambiarEstado(
                clasificacion.getIdClasificacion(), clasificacion.getEstado(),
                usuarioDelToken(auth)));
    }

    /**
     * Baja fisica de una lista de precios. El procedimiento verifica antes que no tenga
     * precios ni porcentajes asociados.
     */
    @PostMapping("/clasificacion-precio/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarClasificacionPrecio(
            @RequestBody FiltroIdDto filtro, Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.LISTAS);
        exigirId(filtro.getId(), "la lista de precios");
        return respuestaEscritura(
                clasificacionPrecioDao.eliminarClasificacionPrecio(filtro.getId()));
    }

    // ===================================================================== //
    //                          IVA / IT (singleton)                         //
    // ===================================================================== //

    /**
     * Las filas de IVA/IT, de la mas nueva a la mas vieja.
     *
     * <p>En una base sana devuelve exactamente una. Mas de una fila es la senal de que el
     * invariante de fila unica se rompio, y por eso el listado existe: para poder verlo.
     */
    @PostMapping("/costo-iva-it/listar")
    public ResponseEntity<ApiResponse<?>> listarCostoIvaIt() {
        return procesarLista(costoIvaItDao.listarCostoIvaIt(),
                "No hay IVA ni IT configurados.");
    }

    /** La fila vigente, resuelta de forma determinista, con {@code totalIvaIt} ya sumado. */
    @PostMapping("/costo-iva-it/vigente")
    public ResponseEntity<ApiResponse<?>> obtenerCostoIvaItVigente() {
        return procesarUno(costoIvaItDao.obtenerVigente(), "No hay IVA ni IT configurados.");
    }

    /** Filas asociadas a una propuesta. */
    @PostMapping("/costo-iva-it/por-propuesta")
    public ResponseEntity<ApiResponse<?>> listarCostoIvaItPorPropuesta(
            @RequestBody FiltroIdDto filtro) {
        exigirId(filtro.getId(), "la propuesta");
        return procesarLista(costoIvaItDao.listarPorPropuesta(filtro.getId()),
                "La propuesta no tiene IVA ni IT propios.");
    }

    /**
     * Modifica el IVA y el IT que usa todo el calculo de precios.
     *
     * <p><b>Esta tabla es un singleton y el endpoint lo hace cumplir.</b> Los
     * procedimientos del modulo leen los dos impuestos con
     * {@code (SELECT TOP 1 iva FROM tpr_costoIvaIt)} <b>sin ORDER BY</b>: si alguna vez
     * hubiera una segunda fila, cual de las dos gana la decide el plan de ejecucion, y
     * los precios de todo el sistema cambiarian en silencio, sin ningun error y sin que
     * nadie lo note hasta ver una factura mal. Un alta accidental es todo lo que hace
     * falta para provocarlo.
     *
     * <p>Por eso este endpoint <b>prefiere actualizar antes que insertar</b>: si ya hay
     * una fila, modifica esa —la vigente, resuelta por el mayor {@code idCII}, aunque el
     * cuerpo no traiga el {@code idCII} o traiga otro— y solo inserta cuando la tabla
     * esta realmente vacia. El procedimiento ademas rechaza por su cuenta la segunda
     * fila; esto es la segunda barrera, del lado de la aplicacion, para que la primera
     * nunca se ponga a prueba.
     */
    @PostMapping("/costo-iva-it/registrar")
    public ResponseEntity<ApiResponse<?>> registrarCostoIvaIt(@RequestBody CostoIvaIt costoIvaIt,
                                                              Authentication auth) {
        pantallas.exigir(auth, AccesoPantallaPrecios.PARAMETROS);
        costoIvaIt.setAudUsuario(usuarioDelToken(auth));

        CostoIvaItDto vigente = costoIvaItDao.obtenerVigente();
        if (vigente == null) {
            log.warn("tpr_costoIvaIt está vacía: se inserta la primera y única fila de IVA/IT.");
            return respuestaEscritura(costoIvaItDao.registrarCostoIvaIt(costoIvaIt, "I"));
        }

        if (costoIvaIt.getIdCII() != null && !costoIvaIt.getIdCII().equals(vigente.getIdCII())) {
            log.warn("Se pidió modificar el IVA/IT {} pero la fila vigente es la {}: "
                            + "se actualiza la vigente, que es la que leen los cálculos.",
                    costoIvaIt.getIdCII(), vigente.getIdCII());
        }
        costoIvaIt.setIdCII(vigente.getIdCII());

        return respuestaEscritura(costoIvaItDao.registrarCostoIvaIt(costoIvaIt, "U"));
    }

    // ===================================================================== //
    //                    ANCLA DEL TIPO DE CAMBIO (lectura)                 //
    // ===================================================================== //
    //
    // SOLO LECTURA, a proposito. tpr_tcAncla es la configuracion del reprecio nocturno
    // automatico, que hoy maneja un job de PowerShell y que esta FUERA DEL ALCANCE de
    // esta migracion. El DAO tiene las escrituras que el job necesita (sembrar el ancla,
    // moverla despues de un reprecio, registrar el tipo de cambio visto, ajustar el
    // umbral) y aqui NO se exponen: una de esas operaciones lanzada desde la aplicacion
    // descalibraria el job —mover el ancla a mano hace que el reprecio de esa noche no
    // dispare, o que dispare sobre una base equivocada—. Estos dos endpoints existen para
    // que la pantalla pueda MOSTRAR como esta configurado, nada mas.

    /** Anclas configuradas, una por empresa. */
    @PostMapping("/tc-ancla/listar")
    public ResponseEntity<ApiResponse<?>> listarTcAncla() {
        return procesarLista(tcAnclaDao.listar(), "No hay anclas de tipo de cambio configuradas.");
    }

    /** El ancla de una empresa puntual, identificada por su base de datos SAP. */
    @PostMapping("/tc-ancla/obtener")
    public ResponseEntity<ApiResponse<?>> obtenerTcAncla(@RequestBody TcAncla filtro) {
        if (filtro.getCompanyDB() == null || filtro.getCompanyDB().trim().isEmpty()) {
            throw new SpBusinessException("Debe indicar la empresa cuya ancla desea consultar.");
        }
        return procesarUno(tcAnclaDao.obtenerPorCompanyDB(filtro.getCompanyDB()),
                "Esa empresa todavía no tiene ancla sembrada.");
    }

    // ===================================================================== //
    //                               APOYO                                   //
    // ===================================================================== //

    /** Usuario autenticado, como {@code long} para los campos {@code audUsuario}. */
    private long usuarioDelToken(Authentication auth) {
        return DatosToken.codUsuarioDe(auth);
    }

    /** Una PK nula o en 0 es un alta; cualquier otra cosa, una modificacion. */
    private boolean esAlta(Number id) {
        return id == null || id.longValue() == 0L;
    }

    /** La ACCION que corresponde segun venga o no la PK: 'I' alta, 'U' modificacion. */
    private String accion(Number id) {
        return esAlta(id) ? "I" : "U";
    }

    /** Falla con 400 si la peticion no trae el identificador sobre el que operar. */
    private void exigirId(Number id, String queCosa) {
        if (id == null || id.longValue() <= 0L) {
            throw new SpBusinessException("Debe indicar " + queCosa + " sobre el que desea operar.");
        }
    }

    /**
     * Falla con 400 si falta alguna de las dos partes de la clave natural de
     * {@code tpr_grupoFamTipoRangoGram}. Es lo unico que identifica a esas filas: la tabla
     * no tiene PK.
     */
    private void exigirClaveNatural(GrupoFamTipoRangoGram asignacion) {
        if (asignacion.getIdGrpFamiliaSap() == null || asignacion.getIdGrpFamiliaSap() <= 0) {
            throw new SpBusinessException("Debe indicar el grupo de familia.");
        }
        if (asignacion.getIdTipo() == null || asignacion.getIdTipo() <= 0) {
            throw new SpBusinessException("Debe indicar el tipo de papel.");
        }
    }

    /** Los ids de la tabla puente son int; el resto de la aplicacion los transporta como Long. */
    private Long aLong(Integer valor) {
        return valor == null ? null : valor.longValue();
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

    /** Igual que {@link #procesarLista}, para las consultas que devuelven una sola fila. */
    private <T> ResponseEntity<ApiResponse<?>> procesarUno(T dato, String mensajeVacio) {
        if (dato == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, dato, HttpStatus.OK.value()));
    }
}
