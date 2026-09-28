package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IArticuloPropuesto;
import bo.bosque.com.impexpap.dao.IPorcentaje;
import bo.bosque.com.impexpap.dao.IAutorizacion;
import bo.bosque.com.impexpap.dao.ICostoIncre;
import bo.bosque.com.impexpap.dao.ICostoSug;
import bo.bosque.com.impexpap.dao.IPrecio;
import bo.bosque.com.impexpap.dao.IPrecioPropuesta;
import bo.bosque.com.impexpap.dao.IProducto;
import bo.bosque.com.impexpap.dao.IPropuesta;
import bo.bosque.com.impexpap.dto.ArmadoArticulosDto;
import bo.bosque.com.impexpap.dto.ArmadoFamiliaDto;
import bo.bosque.com.impexpap.dto.ArmadoLoteDto;
import bo.bosque.com.impexpap.dto.AutorizacionDto;
import bo.bosque.com.impexpap.dto.CalculoFamiliaDto;
import bo.bosque.com.impexpap.dto.CostoIncreCiudadDto;
import bo.bosque.com.impexpap.dto.CostoIncreSucursalDto;
import bo.bosque.com.impexpap.dto.FamiliaArmadaDto;
import bo.bosque.com.impexpap.dto.FamiliaLoteDto;
import bo.bosque.com.impexpap.dto.FletesArmadoDto;
import bo.bosque.com.impexpap.dto.GuardarFletesDto;
import bo.bosque.com.impexpap.dto.LineaArmadoDto;
import bo.bosque.com.impexpap.dto.PrecioCDto;
import bo.bosque.com.impexpap.dto.PrecioDDto;
import bo.bosque.com.impexpap.dto.PorcentajeArmadoDto;
import bo.bosque.com.impexpap.dto.PorcentajeDto;
import bo.bosque.com.impexpap.dto.PrecioGDto;
import bo.bosque.com.impexpap.dto.ProductoDto;
import bo.bosque.com.impexpap.dto.PropuestaDto;
import bo.bosque.com.impexpap.dto.QuitarArticuloDto;
import bo.bosque.com.impexpap.dto.ResultadoArmadoDto;
import bo.bosque.com.impexpap.model.ArticuloPropuesto;
import bo.bosque.com.impexpap.model.CostoIncre;
import bo.bosque.com.impexpap.model.CostoSug;
import bo.bosque.com.impexpap.model.Porcentaje;
import bo.bosque.com.impexpap.model.PrecioPropuesta;
import bo.bosque.com.impexpap.model.Propuesta;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * <b>El armado de una propuesta de precios:</b> la parte de Autorizacion.xhtml que
 * todavia no tenia reemplazo.
 *
 * <h3>Que reemplaza</h3>
 * En el legacy, crear una propuesta era una cadena de dialogos sobre el mismo ManagedBean
 * ({@code WizardProductoNew}): {@code dlgNuevo} (titulo, observacion, fletes),
 * {@code dlgProd} (elegir familias), {@code dlgVista} (costo propuesto, "Calcular",
 * "GUARDAR") y, para las propuestas por articulo, {@code dlgArtD} ("Agregar" de a uno).
 * Cada "GUARDAR" hacia entre cuatro y quince escrituras sueltas, sin transaccion, y la
 * propuesta recien creada se reencontraba con {@code ultimaPropuesta(audUsuario)}: dos
 * pestanias del mismo usuario se pisaban entre si.
 *
 * <h3>Lo que se conserva</h3>
 * <ul>
 *   <li><b>La formula</b> de {@code calcular()}:
 *       {@code costo x (1 + porcentaje) x (1 + IVA + IT) + flete de la sucursal}. Se
 *       verifico contra la propuesta 1123 de BOSQUE2PRUEBA: las 312 lineas coinciden
 *       hasta la cuarta cifra decimal (el legacy calculaba en {@code float}).</li>
 *   <li><b>La validacion de {@code validar()}</b>: dentro de una sucursal, ninguna lista
 *       puede quedar por debajo de la anterior.</li>
 *   <li><b>Cuando nace la propuesta</b>: con la primera familia o el primer articulo que
 *       se guarda. No quedan propuestas vacias por un formulario abandonado.</li>
 *   <li><b>Que se escribe</b>: solo las listas cuyo precio cambia, un costo sugerido por
 *       familia (upsert de {@code p_abm_costoSug 'I'}) y un flete por sucursal.</li>
 * </ul>
 *
 * <h3>Lo que cambia</h3>
 * <ul>
 *   <li><b>El precio lo calcula el servidor, siempre.</b> La pantalla pide la vista previa
 *       a {@link #calcularFamilia} y al guardar el servidor vuelve a calcular: nunca se
 *       persiste un numero que haya calculado el cliente.</li>
 *   <li><b>Una transaccion por operacion.</b> Si falla la ultima linea no queda nada.</li>
 *   <li><b>La validacion bloquea de verdad.</b> En el legacy el mensaje salia pero, para
 *       la primera familia, la condicion del {@code if} ya se habia evaluado.</li>
 *   <li><b>Recalcular es posible.</b> Volver a guardar una familia actualiza sus lineas
 *       -incluidas las que ya estaban propuestas- con el costo, los porcentajes y los
 *       fletes de hoy; y cambiar un flete recalcula todas las familias
 *       ({@link #guardarFletes}). El legacy dejaba el precio viejo en esos casos.</li>
 *   <li><b>Solo se modifica una propuesta Pendiente</b>, del tipo que corresponde, y solo
 *       quien la armo o quien tiene el boton {@code btnPen} -el mismo que habilitaba
 *       "Editar" en el XHTML-.</li>
 * </ul>
 *
 * <h3>Sobre la transaccion</h3>
 * {@code p_abm_precioPropuesta} abre y cierra su propia transaccion; adentro de la de
 * Spring eso solo anida el contador y funciona. Si ese procedimiento falla, su
 * {@code ROLLBACK} deshace tambien lo anterior -que es lo que se quiere- y la peticion
 * termina en error: no queda nada escrito a medias.
 */
@Slf4j
@Component
public class ArmadoPropuestaService {

    /** {@code tpr_propuesta.tipo}: reprecio de familias por tonelada. */
    public static final int TIPO_POR_FAMILIA = 1;
    /** {@code tpr_propuesta.tipo}: alta de articulos puntuales (mercaderia nueva). */
    public static final int TIPO_POR_ARTICULO = 2;

    /** {@code tpr_autorizacion.esAprobada}: la unica en la que se arma. */
    static final int ESTADO_PENDIENTE = 0;

    static final int MAX_TITULO = 200;
    static final int MAX_OBS = 250;
    static final int MAX_ARTICULOS = 1000;

    /**
     * Familias por pedido en lote. La pantalla parte el lote en tandas mas chicas para
     * mostrar el avance; esto es el tope para que una sola peticion no tenga abierta una
     * transaccion de minutos.
     */
    static final int MAX_FAMILIAS_LOTE = 100;

    /** Cuatro decimales: los precios por tonelada se guardan en float(53). */
    static final int ESCALA_PRECIO = 4;

    /** Media unidad de la cuarta cifra: menos que eso es el mismo precio. */
    private static final BigDecimal TOLERANCIA = new BigDecimal("0.00005");

    /** El margen tiene que dejar el precio por encima de cero y no ser un disparate de tipeo. */
    private static final BigDecimal PORCENTAJE_MINIMO = new BigDecimal("-100");
    private static final BigDecimal PORCENTAJE_MAXIMO = new BigDecimal("1000");

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /** Cuantas propuestas por familia se revisan buscando fletes para sugerir. */
    private static final int PROPUESTAS_A_REVISAR = 5;

    private final IPropuesta propuestaDao;
    private final IAutorizacion autorizacionDao;
    private final IPrecio precioDao;
    private final IPrecioPropuesta precioPropuestaDao;
    private final ICostoIncre costoIncreDao;
    private final ICostoSug costoSugDao;
    private final IProducto productoDao;
    private final IArticuloPropuesto articuloPropuestoDao;
    private final IPorcentaje porcentajeDao;

    public ArmadoPropuestaService(IPropuesta propuestaDao,
                                  IAutorizacion autorizacionDao,
                                  IPrecio precioDao,
                                  IPrecioPropuesta precioPropuestaDao,
                                  ICostoIncre costoIncreDao,
                                  ICostoSug costoSugDao,
                                  IProducto productoDao,
                                  IArticuloPropuesto articuloPropuestoDao,
                                  IPorcentaje porcentajeDao) {
        this.propuestaDao = propuestaDao;
        this.autorizacionDao = autorizacionDao;
        this.precioDao = precioDao;
        this.precioPropuestaDao = precioPropuestaDao;
        this.costoIncreDao = costoIncreDao;
        this.costoSugDao = costoSugDao;
        this.productoDao = productoDao;
        this.articuloPropuestoDao = articuloPropuestoDao;
        this.porcentajeDao = porcentajeDao;
    }

    /**
     * Quien esta armando. Lo resuelve el controlador desde el token; el servicio no toca
     * {@code Authentication} para poder probarse sin la cadena de seguridad.
     */
    public static final class Editor {

        private final long codUsuario;
        private final long codEmpresa;

        /**
         * Si tiene el boton {@code btnPen}. Es un proveedor y no un booleano porque
         * resolverlo cuesta dos consultas y solo hace falta cuando la propuesta es ajena.
         */
        private final BooleanSupplier puedeEditarAjenas;

        public Editor(long codUsuario, long codEmpresa, BooleanSupplier puedeEditarAjenas) {
            this.codUsuario = codUsuario;
            this.codEmpresa = codEmpresa;
            this.puedeEditarAjenas = puedeEditarAjenas;
        }

        public long getCodUsuario() { return codUsuario; }

        public long getCodEmpresa() { return codEmpresa; }

        boolean puedeEditarAjenas() {
            return puedeEditarAjenas != null && puedeEditarAjenas.getAsBoolean();
        }
    }

    // ===================================================================== //
    //                              LECTURAS                                 //
    // ===================================================================== //

    /**
     * Los fletes para el paso de datos. Con {@code idPropuesta > 0}, los guardados de esa
     * propuesta; con 0, las sucursales de {@code p_list_costoIncre 'C'} con el valor que
     * tuvieron en la ultima propuesta por familia. Ver {@link FletesArmadoDto}.
     */
    public FletesArmadoDto fletes(long idPropuesta) {
        FletesArmadoDto respuesta = new FletesArmadoDto();

        if (idPropuesta > 0) {
            respuesta.setIdPropuestaReferencia(idPropuesta);
            respuesta.setDeLaPropuesta(true);
            respuesta.setFletes(nuncaNulo(costoIncreDao.listarPorPropuesta(idPropuesta)));
            return respuesta;
        }

        Map<Long, BigDecimal> previos = new HashMap<>();
        Long referencia = null;
        for (Long candidata : ultimasPropuestasPorFamilia()) {
            List<CostoIncreSucursalDto> suyos = nuncaNulo(costoIncreDao.listarPorPropuesta(candidata));
            if (suyos.isEmpty()) continue;
            for (CostoIncreSucursalDto f : suyos) {
                if (f.getCodSucursal() != null && f.getValor() != null) {
                    previos.put(f.getCodSucursal(), f.getValor());
                }
            }
            referencia = candidata;
            break;
        }

        for (CostoIncreCiudadDto sucursal : nuncaNulo(costoIncreDao.listarSucursalesParaCarga())) {
            if (sucursal.getCodSucursal() == null) continue;
            respuesta.getFletes().add(new CostoIncreSucursalDto(
                    null,
                    sucursal.getCodSucursal(),
                    sucursal.getNombre(),
                    previos.getOrDefault(sucursal.getCodSucursal(), BigDecimal.ZERO)));
        }
        respuesta.setIdPropuestaReferencia(previos.isEmpty() ? null : referencia);
        respuesta.setDeLaPropuesta(false);
        return respuesta;
    }

    /**
     * Las familias que ya estan en la propuesta, con su costo y cuantas listas tienen
     * precio propuesto. Ver {@link FamiliaArmadaDto}.
     */
    public List<FamiliaArmadaDto> familiasDePropuesta(long idPropuesta) {
        exigirId(idPropuesta);

        Map<Integer, Integer> lineasPorFamilia = new HashMap<>();
        for (PrecioPropuesta pp : nuncaNulo(precioPropuestaDao.listarPorPropuesta(idPropuesta))) {
            Integer codigo = codigoFamiliaDe(pp.getCodigoFamilia());
            if (codigo != null) lineasPorFamilia.merge(codigo, 1, Integer::sum);
        }

        List<FamiliaArmadaDto> familias = new ArrayList<>();
        Set<Integer> vistas = new HashSet<>();
        for (CostoSug costo : nuncaNulo(costoSugDao.obtenerPorPropuesta(idPropuesta))) {
            Integer codigo = costo.getCodigoFamilia();
            if (codigo == null || !vistas.add(codigo)) continue;
            familias.add(new FamiliaArmadaDto(codigo, costo.getCostoSug(),
                    lineasPorFamilia.getOrDefault(codigo, 0)));
        }
        // Precios sin costo sugerido: datos viejos o cargados a mano. Se muestran igual,
        // porque al aprobar se aplican aunque aca no se vean.
        for (Map.Entry<Integer, Integer> e : lineasPorFamilia.entrySet()) {
            if (vistas.add(e.getKey())) {
                familias.add(new FamiliaArmadaDto(e.getKey(), null, e.getValue()));
            }
        }
        familias.sort(Comparator.comparing(FamiliaArmadaDto::getCodigoFamilia));
        return familias;
    }

    /** Los articulos de una propuesta por articulo, tal como estan en la tabla. */
    public List<ArticuloPropuesto> articulosDePropuesta(long idPropuesta) {
        exigirId(idPropuesta);
        ArticuloPropuesto filtro = new ArticuloPropuesto();
        filtro.setIdPropuesta(idPropuesta);
        return nuncaNulo(articuloPropuestoDao.listar(filtro));
    }

    /**
     * La vista previa de una familia: no escribe nada. Con {@code costo} nulo usa el ya
     * guardado en la propuesta, y si tampoco hay devuelve la grilla sin calcular.
     */
    public CalculoFamiliaDto calcularFamilia(ArmadoFamiliaDto dto) {
        int codigoFamilia = exigirCodigoFamilia(dto.getCodigoFamilia());
        boolean existe = esId(dto.getIdPropuesta());
        if (dto.getCosto() != null && dto.getCosto().signum() <= 0) {
            throw new SpBusinessException("El costo propuesto tiene que ser mayor a cero.");
        }

        Map<Long, BigDecimal> porcentajes = porcentajesPedidos(dto.getPorcentajes());
        Contexto ctx = leerContexto(existe ? dto.getIdPropuesta() : null, codigoFamilia,
                existe ? null : dto.getFletes());
        return calcular(ctx, dto.getCosto(), porcentajes);
    }

    /**
     * La vista previa de varias familias, cada una con su costo. No escribe nada.
     *
     * <p>No se corta en la primera familia con problemas: cada una vuelve con su grilla y,
     * si no se podria guardar, con el motivo en {@code impedimento}. La pantalla muestra
     * el lote entero y guarda solo las que estan bien.
     */
    public List<CalculoFamiliaDto> calcularFamilias(ArmadoLoteDto dto) {
        List<FamiliaLoteDto> pedidas = familiasDelLote(dto.getFamilias());
        boolean existe = esId(dto.getIdPropuesta());
        Long idPropuesta = existe ? dto.getIdPropuesta() : null;
        Map<Long, BigDecimal> fletes = existe ? fletesDePropuesta(idPropuesta) : null;

        List<CalculoFamiliaDto> calculos = new ArrayList<>();
        for (FamiliaLoteDto familia : pedidas) {
            calculos.add(calcularDelLote(idPropuesta, familia,
                    existe ? null : dto.getFletes(), fletes, !existe));
        }
        return calculos;
    }

    // ===================================================================== //
    //                             ESCRITURAS                                //
    // ===================================================================== //

    /**
     * Carga (o recarga) una familia en una propuesta por familia. Si la propuesta no
     * existe la crea, con sus fletes, en la misma transaccion.
     */
    @Transactional
    public CalculoFamiliaDto guardarFamilia(ArmadoFamiliaDto dto, Editor editor) {
        int codigoFamilia = exigirCodigoFamilia(dto.getCodigoFamilia());
        BigDecimal costo = dto.getCosto();
        if (costo == null || costo.signum() <= 0) {
            throw new SpBusinessException("Indique el costo propuesto de la familia, mayor a cero.");
        }

        Map<Long, BigDecimal> porcentajes = porcentajesPedidos(dto.getPorcentajes());
        boolean esAlta = !esId(dto.getIdPropuesta());
        List<CostoIncreSucursalDto> fletesDelAlta = null;
        if (esAlta) {
            validarCabecera(dto.getTitulo(), dto.getObs());
            fletesDelAlta = fletesValidos(dto.getFletes());
            exigirEmpresa(editor);
        } else {
            exigirEditable(dto.getIdPropuesta(), TIPO_POR_FAMILIA, editor);
        }

        Contexto ctx = leerContexto(esAlta ? null : dto.getIdPropuesta(), codigoFamilia, fletesDelAlta);
        if (ctx.familia.getEstado() == null || ctx.familia.getEstado() != 1) {
            throw new SpBusinessException("La familia " + codigoFamilia
                    + " está inactiva: no se le pueden proponer precios.");
        }
        if (ctx.vigentes.isEmpty()) {
            throw new SpBusinessException(ctx.todos.isEmpty()
                    ? "La familia " + codigoFamilia + " no tiene precios cargados: no hay nada que repreciar."
                    : "La familia " + codigoFamilia + " solo tiene precios en listas inactivas. "
                            + "Active las listas en «Listas de precio» para poder repreciarla.");
        }

        CalculoFamiliaDto calculo = calcular(ctx, costo, porcentajes);
        exigirSinErrores(calculo, "");

        if (esAlta) {
            exigirFleteParaCadaSucursal(calculo, ctx.fletes);
        }
        if (calculo.getLineasNuevas() + calculo.getLineasActualizadas() == 0 && !calculo.isEnPropuesta()) {
            throw new SpBusinessException("Con ese costo ningún precio de la familia " + codigoFamilia
                    + " cambia: no hay nada que proponer.");
        }

        long idPropuesta = esAlta
                ? crearPropuesta(TIPO_POR_FAMILIA, dto.getTitulo(), dto.getObs(), editor)
                : dto.getIdPropuesta();
        if (esAlta) {
            insertarFletes(idPropuesta, fletesDelAlta, editor);
        }

        // Primero los porcentajes: si la base rechaza uno, la excepcion revierte todo y
        // no quedan precios calculados con un porcentaje que no se guardo.
        int porcentajesGuardados = registrarPorcentajes(codigoFamilia, calculo, editor.getCodUsuario());
        escribirLineas(idPropuesta, codigoFamilia, calculo, editor.getCodUsuario());
        grabarCostoSugerido(idPropuesta, codigoFamilia, costo, editor.getCodUsuario());

        calculo.setIdPropuesta(idPropuesta);
        calculo.setEnPropuesta(true);
        calculo.setCostoGuardado(costo);
        calculo.setGuardado(true);
        log.info("Propuesta {}: familia {} cargada por el usuario {} ({} nuevas, {} actualizadas, "
                        + "{} porcentajes registrados).",
                idPropuesta, codigoFamilia, editor.getCodUsuario(),
                calculo.getLineasNuevas(), calculo.getLineasActualizadas(), porcentajesGuardados);
        return calculo;
    }

    /**
     * Carga varias familias en la propuesta, cada una con su costo, en UNA transaccion. Si
     * la propuesta no existe, nace con este lote y sus fletes.
     *
     * <p>Todas se calculan y se validan antes de escribir la primera fila: si una sola no
     * se puede guardar -inactiva, fuera de orden, sin cambios-, no se guarda ninguna y el
     * mensaje nombra la familia. La pantalla manda solo las que la vista previa dio por
     * buenas; esto es la red por si algo cambio en el medio.
     *
     * <p>Los porcentajes son los de {@code tpr_porcentaje}: en lote no se cambian.
     */
    @Transactional
    public ResultadoArmadoDto guardarFamilias(ArmadoLoteDto dto, Editor editor) {
        List<FamiliaLoteDto> pedidas = familiasDelLote(dto.getFamilias());
        boolean esAlta = !esId(dto.getIdPropuesta());
        List<CostoIncreSucursalDto> fletesDelAlta = null;
        Map<Long, BigDecimal> fletesLeidos = null;
        if (esAlta) {
            validarCabecera(dto.getTitulo(), dto.getObs());
            fletesDelAlta = fletesValidos(dto.getFletes());
            exigirEmpresa(editor);
        } else {
            exigirEditable(dto.getIdPropuesta(), TIPO_POR_FAMILIA, editor);
            fletesLeidos = fletesDePropuesta(dto.getIdPropuesta());
        }

        List<CalculoFamiliaDto> calculos = new ArrayList<>();
        List<String> problemas = new ArrayList<>();
        for (FamiliaLoteDto familia : pedidas) {
            CalculoFamiliaDto calculo = calcularDelLote(esAlta ? null : dto.getIdPropuesta(),
                    familia, fletesDelAlta, fletesLeidos, esAlta);
            if (calculo.getImpedimento() != null) {
                problemas.add("Familia " + familia.getCodigoFamilia() + ". " + calculo.getImpedimento());
            } else {
                calculos.add(calculo);
            }
        }
        if (!problemas.isEmpty()) {
            String extra = problemas.size() > 1
                    ? " (y " + (problemas.size() - 1) + " "
                            + (problemas.size() == 2 ? "familia más" : "familias más") + " con problemas)"
                    : "";
            throw new SpBusinessException(problemas.get(0) + extra + " No se guardó nada.");
        }

        long idPropuesta = esAlta
                ? crearPropuesta(TIPO_POR_FAMILIA, dto.getTitulo(), dto.getObs(), editor)
                : dto.getIdPropuesta();
        if (esAlta) {
            insertarFletes(idPropuesta, fletesDelAlta, editor);
        }

        int lineas = 0;
        for (CalculoFamiliaDto calculo : calculos) {
            lineas += escribirLineas(idPropuesta, calculo.getCodigoFamilia(), calculo, editor.getCodUsuario());
            grabarCostoSugerido(idPropuesta, calculo.getCodigoFamilia(), calculo.getCosto(),
                    editor.getCodUsuario());
        }

        log.info("Propuesta {}: {} familias cargadas en lote por el usuario {} ({} lineas escritas).",
                idPropuesta, calculos.size(), editor.getCodUsuario(), lineas);
        return new ResultadoArmadoDto(idPropuesta, 0, 0, calculos.size(), lineas);
    }

    /**
     * Cambia los fletes de una propuesta por familia pendiente y recalcula todas sus
     * familias con su costo guardado. Si una sola queda fuera de orden no se guarda nada.
     */
    @Transactional
    public ResultadoArmadoDto guardarFletes(GuardarFletesDto dto, Editor editor) {
        long idPropuesta = exigirId(dto.getIdPropuesta());
        exigirEditable(idPropuesta, TIPO_POR_FAMILIA, editor);
        List<CostoIncreSucursalDto> nuevos = fletesValidos(dto.getFletes());

        Map<Long, CostoIncreSucursalDto> actuales = new HashMap<>();
        for (CostoIncreSucursalDto f : nuncaNulo(costoIncreDao.listarPorPropuesta(idPropuesta))) {
            if (f.getCodSucursal() != null) actuales.put(f.getCodSucursal(), f);
        }

        boolean cambioAlguno = false;
        for (CostoIncreSucursalDto flete : nuevos) {
            CostoIncreSucursalDto actual = actuales.get(flete.getCodSucursal());
            CostoIncre registro = new CostoIncre();
            registro.setCodSucursal(flete.getCodSucursal());
            registro.setIdPropuesta(idPropuesta);
            registro.setValor(flete.getValor());
            registro.setAudUsuario(editor.getCodUsuario());

            if (actual != null && actual.getIdIncre() != null) {
                if (mismoMonto(actual.getValor(), flete.getValor())) continue;
                registro.setIdIncre(actual.getIdIncre());
                costoIncreDao.actualizar(registro);
            } else {
                costoIncreDao.insertar(registro);
            }
            cambioAlguno = true;
        }

        if (!cambioAlguno) {
            return new ResultadoArmadoDto(idPropuesta, 0, 0, 0, 0);
        }

        int familias = 0;
        int lineas = 0;
        for (FamiliaArmadaDto familia : familiasDePropuesta(idPropuesta)) {
            BigDecimal costo = familia.getCostoSug();
            if (costo == null || costo.signum() <= 0) continue;

            Contexto ctx = leerContexto(idPropuesta, familia.getCodigoFamilia(), null);
            CalculoFamiliaDto calculo = calcular(ctx, costo, Collections.<Long, BigDecimal>emptyMap());
            exigirSinErrores(calculo, "Familia " + familia.getCodigoFamilia() + ": ");
            lineas += escribirLineas(idPropuesta, familia.getCodigoFamilia(), calculo,
                    editor.getCodUsuario());
            familias++;
        }

        log.info("Propuesta {}: fletes cambiados por el usuario {}; {} familias y {} lineas recalculadas.",
                idPropuesta, editor.getCodUsuario(), familias, lineas);
        return new ResultadoArmadoDto(idPropuesta, 0, 0, familias, lineas);
    }

    /**
     * Agrega articulos a una propuesta por articulo, creandola si hace falta. Los que ya
     * estaban se cuentan como omitidos y no se duplican.
     */
    @Transactional
    public ResultadoArmadoDto agregarArticulos(ArmadoArticulosDto dto, Editor editor) {
        List<String> codigos = codigosLimpios(dto.getCodArticulos());
        if (codigos.isEmpty()) {
            throw new SpBusinessException("Elija al menos un artículo para agregar.");
        }
        if (codigos.size() > MAX_ARTICULOS) {
            throw new SpBusinessException("Se pueden agregar hasta " + MAX_ARTICULOS
                    + " artículos por vez; se eligieron " + codigos.size() + ".");
        }

        boolean esAlta = !esId(dto.getIdPropuesta());
        Set<String> yaEstan = new HashSet<>();
        long idPropuesta;
        if (esAlta) {
            validarCabecera(dto.getTitulo(), dto.getObs());
            exigirEmpresa(editor);
            idPropuesta = crearPropuesta(TIPO_POR_ARTICULO, dto.getTitulo(), dto.getObs(), editor);
        } else {
            idPropuesta = dto.getIdPropuesta();
            exigirEditable(idPropuesta, TIPO_POR_ARTICULO, editor);
            for (ArticuloPropuesto a : articulosDePropuesta(idPropuesta)) {
                if (a.getCodArticulo() != null) yaEstan.add(claveArticulo(a.getCodArticulo()));
            }
        }

        int agregados = 0;
        int omitidos = 0;
        for (String codigo : codigos) {
            if (yaEstan.contains(claveArticulo(codigo))) {
                omitidos++;
                continue;
            }
            // 'G' copia familia, descripcion, stock y UTM desde tpr_articulo y rechaza un
            // codigo que no exista alli: ese rechazo revierte todo el lote.
            articuloPropuestoDao.agregarArticuloAPropuesta(idPropuesta, codigo, editor.getCodUsuario());
            agregados++;
        }

        log.info("Propuesta {}: {} articulos agregados ({} ya estaban) por el usuario {}.",
                idPropuesta, agregados, omitidos, editor.getCodUsuario());
        return new ResultadoArmadoDto(idPropuesta, agregados, omitidos, 0, 0);
    }

    /** Saca un articulo de una propuesta por articulo pendiente. */
    @Transactional
    public ResultadoArmadoDto quitarArticulo(QuitarArticuloDto dto, Editor editor) {
        long idPropuesta = exigirId(dto.getIdPropuesta());
        if (!esId(dto.getIdArticulo())) {
            throw new SpBusinessException("Indique el artículo que desea quitar.");
        }
        exigirEditable(idPropuesta, TIPO_POR_ARTICULO, editor);

        ArticuloPropuesto articulo = articuloPropuestoDao.obtenerPorId(dto.getIdArticulo());
        if (articulo == null || !Objects.equals(articulo.getIdPropuesta(), idPropuesta)) {
            throw new SpBusinessException("Ese artículo no pertenece a la propuesta N.º " + idPropuesta + ".");
        }
        articuloPropuestoDao.eliminar(dto.getIdArticulo());
        return new ResultadoArmadoDto(idPropuesta, 1, 0, 0, 0);
    }

    /**
     * Trae de SAP los articulos nuevos y refresca stock y UTM de los que ya estan
     * ({@code p_abm_ArticuloProp 'F'}). El legacy lo corria cada vez que alguien abria
     * "Nueva Propuesta"; aca es un boton, porque puede tardar varios minutos y una
     * propuesta por familia no lo necesita.
     */
    public RespuestaSp sincronizarArticulosSap(long audUsuario) {
        return articuloPropuestoDao.sincronizarArticulosSap(audUsuario);
    }

    // ===================================================================== //
    //                              CALCULO                                  //
    // ===================================================================== //

    /** Todo lo que hace falta para calcular una familia, leido una sola vez. */
    private static final class Contexto {
        Long idPropuesta;
        ProductoDto familia;
        /** Precios de las listas ACTIVAS ({@code p_list_precio 'D'}): lo que se reprecia. */
        List<PrecioDDto> vigentes = new ArrayList<>();
        /** Todos los precios de la familia, tambien de listas inactivas ({@code 'C'}). */
        List<PrecioCDto> todos = new ArrayList<>();
        final Map<Long, PrecioGDto> propuestas = new HashMap<>();
        final Map<Long, BigDecimal> fletes = new HashMap<>();
        CostoSug costoGuardado;
    }

    private Contexto leerContexto(Long idPropuesta, int codigoFamilia,
                                  List<CostoIncreSucursalDto> fletesDelAlta) {
        return leerContexto(idPropuesta, codigoFamilia, fletesDelAlta, null);
    }

    /**
     * Con {@code fletesLeidos} no se vuelven a leer los fletes de la propuesta: en lote son
     * los mismos para todas las familias y se leen una vez.
     */
    private Contexto leerContexto(Long idPropuesta, int codigoFamilia,
                                  List<CostoIncreSucursalDto> fletesDelAlta,
                                  Map<Long, BigDecimal> fletesLeidos) {
        Contexto ctx = new Contexto();
        ctx.idPropuesta = idPropuesta;

        ctx.familia = productoDao.obtenerConDescripcion(codigoFamilia);
        if (ctx.familia == null) {
            throw new SpBusinessException("La familia " + codigoFamilia + " no existe.");
        }
        ctx.vigentes = nuncaNulo(precioDao.listarDetalleParaReprecio(codigoFamilia));
        ctx.todos = nuncaNulo(precioDao.listarPorFamilia(codigoFamilia));

        if (idPropuesta != null) {
            for (PrecioGDto g : nuncaNulo(precioDao.listarDePropuesta(codigoFamilia, idPropuesta))) {
                if (g.getIdPrecio() != null) ctx.propuestas.put(g.getIdPrecio(), g);
            }
            ctx.fletes.putAll(fletesLeidos != null ? fletesLeidos : fletesDePropuesta(idPropuesta));
            ctx.costoGuardado = costoSugDao.obtenerPorPropuestaYFamilia(idPropuesta, codigoFamilia);
        } else if (fletesDelAlta != null) {
            for (CostoIncreSucursalDto f : fletesDelAlta) {
                if (f.getCodSucursal() != null) ctx.fletes.put(f.getCodSucursal(), noNulo(f.getValor()));
            }
        }
        return ctx;
    }

    private CalculoFamiliaDto calcular(Contexto ctx, BigDecimal costoPedido,
                                       Map<Long, BigDecimal> porcentajesPedidos) {
        BigDecimal guardado = ctx.costoGuardado != null ? ctx.costoGuardado.getCostoSug() : null;
        BigDecimal costo = costoPedido != null ? costoPedido : guardado;

        CalculoFamiliaDto r = new CalculoFamiliaDto();
        r.setIdPropuesta(ctx.idPropuesta);
        r.setCodigoFamilia(ctx.familia.getCodigoFamilia());
        r.setGrupoFamilia(ctx.familia.getGrpFamilia());
        r.setProveedor(ctx.familia.getProveedorSap());
        r.setPresentacion(ctx.familia.getPresentacion());
        r.setTipo(ctx.familia.getTipo());
        r.setRangoGramaje(ctx.familia.getRangoGramaje());
        r.setColor(ctx.familia.getColor());
        r.setCostoActual(ctx.familia.getCostoTM());
        r.setCostoGuardado(guardado);
        r.setEnPropuesta(ctx.costoGuardado != null || !ctx.propuestas.isEmpty());
        r.setCosto(costo);

        for (PrecioDDto vigente : ctx.vigentes) {
            LineaArmadoDto linea = new LineaArmadoDto();
            linea.setIdPrecio(vigente.getIdPrecio());
            linea.setIdClasificacion(vigente.getIdClasificacion());
            linea.setCodSucursal(vigente.getCodSucursal());
            linea.setNombreSucursal(vigente.getNombre());
            linea.setNombrePrecio(vigente.getNombrePrecio());
            linea.setVpp(vigente.getVpp());
            linea.setListNum(vigente.getListNum());
            BigDecimal porcentajeVigente = noNulo(vigente.getPorcentaje());
            BigDecimal pedido = vigente.getIdClasificacion() == null
                    ? null : porcentajesPedidos.get(vigente.getIdClasificacion());
            linea.setPorcentajeVigente(porcentajeVigente);
            if (pedido != null && !mismoMonto(pedido, porcentajeVigente)) {
                linea.setPorcentaje(pedido);
                linea.setPorcentajeCambiado(true);
                r.setPorcentajesCambiados(r.getPorcentajesCambiados() + 1);
            } else {
                linea.setPorcentaje(porcentajeVigente);
            }
            linea.setIva(noNulo(vigente.getIva()));
            linea.setIt(noNulo(vigente.getIt()));
            linea.setFlete(ctx.fletes.getOrDefault(vigente.getCodSucursal(), BigDecimal.ZERO));
            linea.setPrecioActual(noNulo(vigente.getPrecio()));

            PrecioGDto yaPropuesta = ctx.propuestas.get(vigente.getIdPrecio());
            if (yaPropuesta != null) {
                linea.setIdPrecioPropuesto(yaPropuesta.getIdPrecioPropuesto());
                linea.setPrecioPropuestoGuardado(yaPropuesta.getPrecioPropuesto());
            }

            if (costo == null) {
                linea.setEstado(LineaArmadoDto.SIN_CALCULO);
            } else {
                BigDecimal precio = precioDe(costo, linea.getPorcentaje(), linea.getIva(),
                        linea.getIt(), linea.getFlete());
                linea.setPrecioCalculado(precio);
                linea.setEstado(estadoDe(linea, yaPropuesta));
            }
            r.getLineas().add(linea);
        }

        agregarListasInactivas(ctx, r);

        r.getLineas().sort(Comparator
                .comparing(LineaArmadoDto::getVpp, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LineaArmadoDto::getCodSucursal, Comparator.nullsLast(Comparator.naturalOrder())));
        r.setErrores(marcarFueraDeOrden(r.getLineas()));

        for (LineaArmadoDto linea : r.getLineas()) {
            if (LineaArmadoDto.NUEVA.equals(linea.getEstado())) r.setLineasNuevas(r.getLineasNuevas() + 1);
            else if (LineaArmadoDto.ACTUALIZA.equals(linea.getEstado())) r.setLineasActualizadas(r.getLineasActualizadas() + 1);
            else if (LineaArmadoDto.SIN_CAMBIO.equals(linea.getEstado())) r.setLineasSinCambio(r.getLineasSinCambio() + 1);
            else if (LineaArmadoDto.LISTA_INACTIVA.equals(linea.getEstado())) r.setLineasInactivas(r.getLineasInactivas() + 1);
        }
        return r;
    }

    /**
     * Lo que la grilla de listas activas no muestra y hay que decir.
     *
     * <p>{@code p_list_precio 'D'} -la grilla del reprecio- trae solo las listas activas,
     * igual que en el legacy: una lista desactivada no se reprecia. Pero la familia puede
     * tener precio en listas inactivas, y sin nombrarlas la grilla parece incompleta. Y si
     * la propuesta ya tenia precio en una lista que despues se desactivo, esa fila sigue
     * en {@code tpr_precioPropuesta} y se aplica al aprobar: se muestra como
     * {@link LineaArmadoDto#LISTA_INACTIVA}, con su precio guardado, sin recalcular.
     */
    private static void agregarListasInactivas(Contexto ctx, CalculoFamiliaDto r) {
        Set<Long> activos = new HashSet<>();
        for (PrecioDDto v : ctx.vigentes) {
            if (v.getIdPrecio() != null) activos.add(v.getIdPrecio());
        }

        Map<Long, PrecioCDto> inactivos = new LinkedHashMap<>();
        for (PrecioCDto p : ctx.todos) {
            if (p.getIdPrecio() != null && !activos.contains(p.getIdPrecio())) {
                inactivos.put(p.getIdPrecio(), p);
            }
        }
        for (PrecioCDto p : inactivos.values()) {
            // 'C' ya arma el nombre como el legacy: "Precio 1".
            r.getListasInactivas().add(texto(p.getNombre(), "Sucursal " + p.getCodSucursal())
                    + " · " + texto(p.getNombrePrecio(), "lista " + p.getIdClasificacion()));
        }

        for (PrecioGDto g : ctx.propuestas.values()) {
            if (g.getIdPrecio() == null || activos.contains(g.getIdPrecio())) continue;
            PrecioCDto vigente = inactivos.get(g.getIdPrecio());

            LineaArmadoDto linea = new LineaArmadoDto();
            linea.setIdPrecio(g.getIdPrecio());
            linea.setIdClasificacion(vigente != null ? vigente.getIdClasificacion() : null);
            linea.setCodSucursal(g.getCodSucursal());
            linea.setNombreSucursal(g.getNombre());
            linea.setNombrePrecio(g.getNombrePrecio());
            linea.setVpp(g.getVpp());
            linea.setListNum(g.getListNum());
            linea.setPorcentaje(noNulo(g.getPorcentaje()));
            linea.setIva(noNulo(g.getIva()));
            linea.setIt(noNulo(g.getIt()));
            linea.setFlete(ctx.fletes.getOrDefault(g.getCodSucursal(), BigDecimal.ZERO));
            linea.setPrecioActual(vigente != null ? noNulo(vigente.getPrecio()) : noNulo(g.getPrecioActual()));
            linea.setIdPrecioPropuesto(g.getIdPrecioPropuesto());
            linea.setPrecioPropuestoGuardado(g.getPrecioPropuesto());
            linea.setEstado(LineaArmadoDto.LISTA_INACTIVA);
            r.getLineas().add(linea);
        }
    }

    /**
     * Que se hace con la linea al guardar.
     *
     * <p>Una lista que todavia no esta en la propuesta se inserta solo si su precio cambia:
     * es lo que hacia el legacy ({@code montoProp != montoActual}). Una que ya esta se
     * actualiza cuando el calculo nuevo difiere de lo guardado, aunque el resultado sea
     * igual al precio vigente: dejarla con el propuesto viejo es justamente el precio
     * equivocado que el legacy terminaba aprobando.
     */
    static String estadoDe(LineaArmadoDto linea, PrecioGDto yaPropuesta) {
        if (yaPropuesta == null) {
            return mismoMonto(linea.getPrecioCalculado(), linea.getPrecioActual())
                    ? LineaArmadoDto.SIN_CAMBIO : LineaArmadoDto.NUEVA;
        }
        boolean igual = mismoMonto(linea.getPrecioCalculado(), yaPropuesta.getPrecioPropuesto())
                && mismoMonto(linea.getPrecioActual(), yaPropuesta.getPrecioActual())
                && mismoMonto(linea.getPorcentaje(), yaPropuesta.getPorcentaje());
        return igual ? LineaArmadoDto.SIN_CAMBIO : LineaArmadoDto.ACTUALIZA;
    }

    /**
     * {@code costo x (1 + porcentaje/100) x (1 + (iva + it)/100) + flete}, a cuatro
     * decimales. Es la cuenta de {@code WizardProductoNew.calcular()}.
     */
    static BigDecimal precioDe(BigDecimal costo, BigDecimal porcentaje, BigDecimal iva,
                               BigDecimal it, BigDecimal flete) {
        MathContext mc = MathContext.DECIMAL64;
        BigDecimal margen = BigDecimal.ONE.add(noNulo(porcentaje).divide(CIEN, mc), mc);
        BigDecimal impuestos = BigDecimal.ONE.add(noNulo(iva).add(noNulo(it), mc).divide(CIEN, mc), mc);
        return costo.multiply(margen, mc)
                .multiply(impuestos, mc)
                .add(noNulo(flete), mc)
                .setScale(ESCALA_PRECIO, RoundingMode.HALF_UP);
    }

    /**
     * La regla de {@code validar()}: dentro de una sucursal, recorridas por numero de
     * lista, ninguna puede quedar por debajo de la anterior. Marca cada linea que la rompe
     * y devuelve un mensaje por cada una.
     */
    static List<String> marcarFueraDeOrden(List<LineaArmadoDto> lineas) {
        List<LineaArmadoDto> orden = new ArrayList<>(lineas);
        orden.sort(Comparator
                .comparing(LineaArmadoDto::getCodSucursal, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LineaArmadoDto::getVpp, Comparator.nullsLast(Comparator.naturalOrder())));

        List<String> errores = new ArrayList<>();
        LineaArmadoDto anterior = null;
        for (LineaArmadoDto linea : orden) {
            linea.setFueraDeOrden(false);
            if (linea.getPrecioCalculado() == null) continue;

            boolean mismaSucursal = anterior != null
                    && Objects.equals(anterior.getCodSucursal(), linea.getCodSucursal());
            if (mismaSucursal && linea.getPrecioCalculado()
                    .compareTo(anterior.getPrecioCalculado().subtract(TOLERANCIA)) < 0) {
                linea.setFueraDeOrden(true);
                errores.add(texto(linea.getNombreSucursal(), "la sucursal " + linea.getCodSucursal())
                        + ": la lista " + etiquetaLista(linea) + " quedaría en "
                        + monto(linea.getPrecioCalculado()) + ", por debajo de la lista "
                        + etiquetaLista(anterior) + ", que queda en "
                        + monto(anterior.getPrecioCalculado())
                        + ". Revise los porcentajes de la familia.");
            }
            anterior = linea;
        }
        return errores;
    }

    // ===================================================================== //
    //                               APOYO                                   //
    // ===================================================================== //

    /**
     * Una familia del lote, calculada y con el motivo por el que no se podria guardar. Un
     * error de negocio de una familia (que no exista, por ejemplo) no corta el lote: queda
     * como su impedimento.
     */
    private CalculoFamiliaDto calcularDelLote(Long idPropuesta, FamiliaLoteDto familia,
                                              List<CostoIncreSucursalDto> fletesDelAlta,
                                              Map<Long, BigDecimal> fletesLeidos, boolean esAlta) {
        try {
            Contexto ctx = leerContexto(idPropuesta, familia.getCodigoFamilia(), fletesDelAlta, fletesLeidos);
            CalculoFamiliaDto calculo = calcular(ctx, familia.getCosto(), Collections.<Long, BigDecimal>emptyMap());
            calculo.setImpedimento(impedimentoParaGuardar(ctx, calculo, esAlta));
            return calculo;
        } catch (SpBusinessException e) {
            CalculoFamiliaDto calculo = new CalculoFamiliaDto();
            calculo.setIdPropuesta(idPropuesta);
            calculo.setCodigoFamilia(familia.getCodigoFamilia());
            calculo.setCosto(familia.getCosto());
            calculo.setImpedimento(e.getMessage());
            return calculo;
        }
    }

    /**
     * Por que una familia calculada no se podria guardar, o null. Son las mismas reglas
     * que {@link #guardarFamilia}, dichas sin cortar: en lote cada familia informa la suya.
     */
    private static String impedimentoParaGuardar(Contexto ctx, CalculoFamiliaDto calculo, boolean esAlta) {
        if (ctx.familia.getEstado() == null || ctx.familia.getEstado() != 1) {
            return "Está inactiva: no se le pueden proponer precios.";
        }
        if (ctx.vigentes.isEmpty()) {
            return ctx.todos.isEmpty()
                    ? "No tiene precios cargados: no hay nada que repreciar."
                    : "Solo tiene precios en listas inactivas. Active las listas en «Listas de precio» "
                            + "para poder repreciarla.";
        }
        List<String> errores = calculo.getErrores();
        if (errores != null && !errores.isEmpty()) {
            return errores.get(0) + (errores.size() > 1 ? " (y " + (errores.size() - 1) + " más)" : "");
        }
        if (esAlta) {
            for (LineaArmadoDto linea : calculo.getLineas()) {
                if (!ctx.fletes.containsKey(linea.getCodSucursal())) {
                    return "Falta el flete de "
                            + texto(linea.getNombreSucursal(), "la sucursal " + linea.getCodSucursal())
                            + ", que tiene listas de precios de esta familia.";
                }
            }
        }
        if (calculo.getLineasNuevas() + calculo.getLineasActualizadas() == 0 && !calculo.isEnPropuesta()) {
            return "Con ese costo ningún precio cambia: no hay nada que proponer.";
        }
        return null;
    }

    /**
     * Las familias del lote: cada una una sola vez, con codigo y un costo mayor a cero. Se
     * valida el lote entero antes de leer nada.
     */
    static List<FamiliaLoteDto> familiasDelLote(List<FamiliaLoteDto> familias) {
        if (familias == null || familias.isEmpty()) {
            throw new SpBusinessException("Escriba el costo de al menos una familia.");
        }
        if (familias.size() > MAX_FAMILIAS_LOTE) {
            throw new SpBusinessException("Se pueden procesar hasta " + MAX_FAMILIAS_LOTE
                    + " familias por vez; llegaron " + familias.size() + ".");
        }
        Set<Integer> vistas = new HashSet<>();
        for (FamiliaLoteDto f : familias) {
            if (f == null) {
                throw new SpBusinessException("Hay una familia vacía en el lote.");
            }
            int codigo = exigirCodigoFamilia(f.getCodigoFamilia());
            if (f.getCosto() == null || f.getCosto().signum() <= 0) {
                throw new SpBusinessException("El costo propuesto de la familia " + codigo
                        + " tiene que ser mayor a cero.");
            }
            if (!vistas.add(codigo)) {
                throw new SpBusinessException("La familia " + codigo + " está dos veces en el lote.");
            }
        }
        return familias;
    }

    /** Los fletes guardados de una propuesta, por sucursal. */
    private Map<Long, BigDecimal> fletesDePropuesta(long idPropuesta) {
        Map<Long, BigDecimal> fletes = new HashMap<>();
        for (CostoIncreSucursalDto f : nuncaNulo(costoIncreDao.listarPorPropuesta(idPropuesta))) {
            if (f.getCodSucursal() != null) fletes.put(f.getCodSucursal(), noNulo(f.getValor()));
        }
        return fletes;
    }

    /** Los fletes con los que nace una propuesta. */
    private void insertarFletes(long idPropuesta, List<CostoIncreSucursalDto> fletes, Editor editor) {
        for (CostoIncreSucursalDto flete : fletes) {
            CostoIncre nuevo = new CostoIncre();
            nuevo.setCodSucursal(flete.getCodSucursal());
            nuevo.setIdPropuesta(idPropuesta);
            nuevo.setValor(flete.getValor());
            nuevo.setAudUsuario(editor.getCodUsuario());
            costoIncreDao.insertar(nuevo);
        }
    }

    /**
     * Los porcentajes que llegan del editor, por lista. Sin lista o sin valor no se
     * aceptan: dejarlos pasar seria calcular con un cero que nadie escribio.
     */
    static Map<Long, BigDecimal> porcentajesPedidos(List<PorcentajeArmadoDto> pedidos) {
        Map<Long, BigDecimal> r = new HashMap<>();
        if (pedidos == null) return r;
        for (PorcentajeArmadoDto p : pedidos) {
            if (p == null) continue;
            if (!esId(p.getIdClasificacion())) {
                throw new SpBusinessException("Falta la lista de precio de un porcentaje.");
            }
            if (p.getPorcentaje() == null) {
                throw new SpBusinessException("Escriba el porcentaje de todas las listas.");
            }
            if (p.getPorcentaje().compareTo(PORCENTAJE_MINIMO) <= 0
                    || p.getPorcentaje().compareTo(PORCENTAJE_MAXIMO) > 0) {
                throw new SpBusinessException("El porcentaje " + monto(p.getPorcentaje())
                        + " no es un margen posible: tiene que ser mayor a -100 y hasta 1.000.");
            }
            r.put(p.getIdClasificacion(), p.getPorcentaje());
        }
        return r;
    }

    /**
     * Registra en tpr_porcentaje los porcentajes cambiados en el editor: modifica la fila
     * de la familia en esa lista, o la da de alta si la familia no tenia porcentaje ahi.
     * Es la misma escritura que hace la pantalla «Porcentajes».
     */
    private int registrarPorcentajes(int codigoFamilia, CalculoFamiliaDto calculo, long audUsuario) {
        List<LineaArmadoDto> cambiadas = new ArrayList<>();
        for (LineaArmadoDto linea : calculo.getLineas()) {
            if (linea.isPorcentajeCambiado()) cambiadas.add(linea);
        }
        if (cambiadas.isEmpty()) return 0;

        Map<Long, Long> idPorcenPorLista = new HashMap<>();
        for (PorcentajeDto p : nuncaNulo(porcentajeDao.listarPorcentajesPorFamilia(codigoFamilia))) {
            if (p.getIdClasificacion() != null && esId(p.getIdPorcen())) {
                idPorcenPorLista.put(p.getIdClasificacion(), p.getIdPorcen());
            }
        }

        for (LineaArmadoDto linea : cambiadas) {
            Porcentaje fila = new Porcentaje();
            fila.setCodigoFamilia(codigoFamilia);
            fila.setIdClasificacion(linea.getIdClasificacion());
            fila.setPorcen(linea.getPorcentaje());
            fila.setAudUsuario(audUsuario);
            Long idPorcen = idPorcenPorLista.get(linea.getIdClasificacion());
            RespuestaSp r;
            if (idPorcen != null) {
                fila.setIdPorcen(idPorcen);
                r = porcentajeDao.registrarPorcentaje(fila, "U");
            } else {
                r = porcentajeDao.registrarPorcentaje(fila, "I");
            }
            if (r == null || r.getError() != 0) {
                throw new SpBusinessException("No se pudo registrar el porcentaje de "
                        + texto(linea.getNombreSucursal(), "la sucursal") + " · " + etiquetaLista(linea)
                        + (r == null || r.getErrormsg() == null ? "." : ": " + r.getErrormsg())
                        + " No se guardó nada.");
            }
        }
        return cambiadas.size();
    }

    private int escribirLineas(long idPropuesta, int codigoFamilia, CalculoFamiliaDto calculo,
                               long audUsuario) {
        int escritas = 0;
        for (LineaArmadoDto linea : calculo.getLineas()) {
            boolean nueva = LineaArmadoDto.NUEVA.equals(linea.getEstado());
            boolean actualiza = LineaArmadoDto.ACTUALIZA.equals(linea.getEstado());
            if (!nueva && !actualiza) continue;

            PrecioPropuesta fila = new PrecioPropuesta();
            fila.setIdPrecioPropuesto(actualiza ? linea.getIdPrecioPropuesto() : null);
            fila.setIdPropuesta(idPropuesta);
            fila.setIdPrecio(linea.getIdPrecio());
            fila.setCodigoFamilia(String.valueOf(codigoFamilia));
            fila.setPrecioActual(linea.getPrecioActual());
            fila.setPrecioPropuesto(linea.getPrecioCalculado());
            fila.setPorcentaje(linea.getPorcentaje());
            // Las cuatro copias denormalizadas se graban desde la lista de precios de hoy,
            // no desde lo que tuviera la fila: asi dejan de desfasarse al recalcular.
            fila.setCodSucursal(linea.getCodSucursal());
            fila.setListNum(linea.getListNum());
            fila.setNombrePrecio(linea.getNombrePrecio());
            fila.setVpp(linea.getVpp());
            fila.setAudUsuario(audUsuario);

            if (nueva) precioPropuestaDao.insertar(fila);
            else precioPropuestaDao.actualizar(fila);
            escritas++;
        }
        return escritas;
    }

    private void grabarCostoSugerido(long idPropuesta, int codigoFamilia, BigDecimal costo,
                                     long audUsuario) {
        CostoSug costoSug = new CostoSug();
        costoSug.setIdPropuesta(idPropuesta);
        costoSug.setCodigoFamilia(codigoFamilia);
        costoSug.setCostoSug(costo);
        costoSug.setAudUsuario(audUsuario);
        // 'I' es un upsert sobre (propuesta, familia): recargar la familia no duplica.
        costoSugDao.registrar(costoSug);
    }

    private long crearPropuesta(int tipo, String titulo, String obs, Editor editor) {
        Propuesta propuesta = new Propuesta();
        propuesta.setCodEmpresa(editor.getCodEmpresa());
        propuesta.setTipo(tipo);
        propuesta.setTitulo(titulo.trim());
        propuesta.setObs(obs.trim());
        propuesta.setEstado(0);
        propuesta.setAudUsuario(editor.getCodUsuario());

        // 'I' tambien da de alta la fila de tpr_autorizacion en Pendiente.
        RespuestaSp respuesta = propuestaDao.registrarPropuesta(propuesta, "I");
        if (respuesta.getIdGenerado() <= 0) {
            throw new SpBusinessException(
                    "La base no devolvió el número de la propuesta nueva. No se guardó nada.");
        }
        return respuesta.getIdGenerado();
    }

    /**
     * Que la propuesta exista, sea del tipo que se esta armando, este Pendiente y quien
     * opera pueda tocarla.
     */
    private Propuesta exigirEditable(long idPropuesta, int tipoEsperado, Editor editor) {
        Propuesta propuesta = propuestaDao.obtenerPorId(idPropuesta);
        if (propuesta == null) {
            throw new SpBusinessException("La propuesta N.º " + idPropuesta + " no existe.");
        }
        if (propuesta.getTipo() == null || propuesta.getTipo() != tipoEsperado) {
            throw new SpBusinessException(tipoEsperado == TIPO_POR_FAMILIA
                    ? "La propuesta N.º " + idPropuesta + " es por artículo: no se le pueden cargar familias."
                    : "La propuesta N.º " + idPropuesta + " es por familia: no se le pueden agregar artículos.");
        }

        PropuestaDto detalle = propuestaDao.obtenerDetalle(idPropuesta);
        Integer estado = detalle != null ? detalle.getEsAprobada() : null;
        if (estado == null || estado != ESTADO_PENDIENTE) {
            throw new SpBusinessException("La propuesta N.º " + idPropuesta + " "
                    + describirEstado(estado) + ": solo se modifica una propuesta pendiente.");
        }

        boolean esSuya = propuesta.getAudUsuario() != null
                && propuesta.getAudUsuario() == editor.getCodUsuario();
        if (!esSuya && !editor.puedeEditarAjenas()) {
            throw new SpBusinessException("La propuesta N.º " + idPropuesta
                    + " la armó otra persona. Para modificarla hace falta el permiso de enviar "
                    + "propuestas a autorizar.");
        }
        return propuesta;
    }

    private static String describirEstado(Integer estado) {
        if (estado == null) return "no tiene estado de autorización";
        switch (estado) {
            case 1: return "ya está aprobada";
            case 2: return "fue rechazada";
            case 3: return "está en espera de autorización";
            default: return "tiene un estado desconocido (" + estado + ")";
        }
    }

    private static void validarCabecera(String titulo, String obs) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new SpBusinessException("Escriba el título de la propuesta.");
        }
        if (titulo.trim().length() > MAX_TITULO) {
            throw new SpBusinessException("El título admite hasta " + MAX_TITULO + " caracteres.");
        }
        if (obs == null || obs.trim().isEmpty()) {
            throw new SpBusinessException("Escriba las observaciones de la propuesta.");
        }
        if (obs.trim().length() > MAX_OBS) {
            throw new SpBusinessException("Las observaciones admiten hasta " + MAX_OBS + " caracteres.");
        }
    }

    /** Sin repetidos, cada uno con sucursal y un valor no negativo. */
    private static List<CostoIncreSucursalDto> fletesValidos(List<CostoIncreSucursalDto> fletes) {
        if (fletes == null || fletes.isEmpty()) {
            throw new SpBusinessException("Indique el costo de flete de cada sucursal.");
        }
        Map<Long, CostoIncreSucursalDto> porSucursal = new LinkedHashMap<>();
        for (CostoIncreSucursalDto flete : fletes) {
            if (flete == null || flete.getCodSucursal() == null || flete.getCodSucursal() <= 0) {
                throw new SpBusinessException("Hay un flete sin sucursal.");
            }
            String nombre = texto(flete.getNombre(), "la sucursal " + flete.getCodSucursal());
            if (flete.getValor() == null) {
                throw new SpBusinessException("Indique el flete de " + nombre + " (puede ser 0).");
            }
            if (flete.getValor().signum() < 0) {
                throw new SpBusinessException("El flete de " + nombre + " no puede ser negativo.");
            }
            porSucursal.put(flete.getCodSucursal(), flete);
        }
        return new ArrayList<>(porSucursal.values());
    }

    /** En el alta, cada sucursal que tiene listas de la familia necesita su flete. */
    private static void exigirFleteParaCadaSucursal(CalculoFamiliaDto calculo, Map<Long, BigDecimal> fletes) {
        for (LineaArmadoDto linea : calculo.getLineas()) {
            if (!fletes.containsKey(linea.getCodSucursal())) {
                throw new SpBusinessException("Falta el flete de "
                        + texto(linea.getNombreSucursal(), "la sucursal " + linea.getCodSucursal())
                        + ", que tiene listas de precios de esta familia.");
            }
        }
    }

    private static void exigirSinErrores(CalculoFamiliaDto calculo, String prefijo) {
        List<String> errores = calculo.getErrores();
        if (errores == null || errores.isEmpty()) return;
        String extra = errores.size() > 1 ? " (y " + (errores.size() - 1) + " más)" : "";
        throw new SpBusinessException(prefijo + errores.get(0) + extra + " No se guardó nada.");
    }

    private static void exigirEmpresa(Editor editor) {
        if (editor.getCodEmpresa() <= 0) {
            throw new SpBusinessException(
                    "Su sesión no indica la empresa. Cierre sesión y vuelva a entrar.");
        }
    }

    /**
     * Que venga el codigo de familia.
     *
     * <p><b>El 0 es una familia.</b> {@code tpr_producto} tiene la familia 0 activa, con
     * precios en 21 listas y propuestas aprobadas en el legacy (la ultima, la 207). Los
     * procedimientos la filtran por igualdad: su comodin es NULL, no 0. Rechazarla dejaba
     * sin repreciar una familia real.
     */
    private static int exigirCodigoFamilia(Integer codigoFamilia) {
        if (codigoFamilia == null || codigoFamilia < 0) {
            throw new SpBusinessException("Debe indicar el código de familia.");
        }
        return codigoFamilia;
    }

    private static long exigirId(Long idPropuesta) {
        if (!esId(idPropuesta)) {
            throw new SpBusinessException("Debe indicar la propuesta sobre la que desea operar.");
        }
        return idPropuesta;
    }

    private static boolean esId(Long id) {
        return id != null && id > 0L;
    }

    /** Las propuestas por familia mas recientes del listado de autorizacion. */
    private List<Long> ultimasPropuestasPorFamilia() {
        List<Long> ids = new ArrayList<>();
        for (AutorizacionDto fila : nuncaNulo(autorizacionDao.listAutorizacion())) {
            if (fila.getTipo() != null && fila.getTipo() == TIPO_POR_FAMILIA && fila.getIdPropuesta() != null) {
                ids.add(fila.getIdPropuesta());
            }
        }
        ids.sort(Comparator.reverseOrder());
        return ids.size() > PROPUESTAS_A_REVISAR ? ids.subList(0, PROPUESTAS_A_REVISAR) : ids;
    }

    /** tpr_precioPropuesta.codigoFamilia es varchar(15): se lee con cuidado. */
    static Integer codigoFamiliaDe(String codigo) {
        if (codigo == null) return null;
        try {
            return Integer.valueOf(codigo.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> codigosLimpios(List<String> codigos) {
        if (codigos == null) return Collections.emptyList();
        Map<String, String> unicos = new LinkedHashMap<>();
        for (String codigo : codigos) {
            if (codigo == null || codigo.trim().isEmpty()) continue;
            unicos.putIfAbsent(claveArticulo(codigo), codigo.trim());
        }
        return new ArrayList<>(unicos.values());
    }

    /** SQL Server compara sin distinguir mayusculas: la clave tampoco. */
    private static String claveArticulo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    static boolean mismoMonto(BigDecimal a, BigDecimal b) {
        return noNulo(a).subtract(noNulo(b)).abs().compareTo(TOLERANCIA) < 0;
    }

    private static BigDecimal noNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static <T> List<T> nuncaNulo(List<T> lista) {
        return lista == null ? new ArrayList<>() : lista;
    }

    private static String texto(String valor, String siFalta) {
        return valor == null || valor.trim().isEmpty() ? siFalta : valor.trim();
    }

    private static String etiquetaLista(LineaArmadoDto linea) {
        String nombre = linea.getNombrePrecio() == null ? "" : linea.getNombrePrecio().trim();
        return nombre.isEmpty() ? String.valueOf(linea.getVpp()) : linea.getVpp() + " (" + nombre + ")";
    }

    /** "1.481,65": como se escriben los montos en Bolivia, igual que en la pantalla. */
    private static String monto(BigDecimal valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.00", simbolos).format(valor);
    }
}
