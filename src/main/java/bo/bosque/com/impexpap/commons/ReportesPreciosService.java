package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IArticuloPropuesto;
import bo.bosque.com.impexpap.dao.IAutorizacion;
import bo.bosque.com.impexpap.dao.IClasificacionPrecio;
import bo.bosque.com.impexpap.dao.ICostoIncre;
import bo.bosque.com.impexpap.dao.ICostoIvaIt;
import bo.bosque.com.impexpap.dao.IProducto;
import bo.bosque.com.impexpap.dao.IPropuesta;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoDDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoFDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoGDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoHDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoIDto;
import bo.bosque.com.impexpap.dto.AutorizacionDto;
import bo.bosque.com.impexpap.dto.ClasificacionPrecioDto;
import bo.bosque.com.impexpap.dto.CostoIncreSucursalDto;
import bo.bosque.com.impexpap.dto.CostoIvaItDto;
import bo.bosque.com.impexpap.dto.FilaRptPropuestaArticuloDto;
import bo.bosque.com.impexpap.dto.FilaRptPropuestaFamiliaDto;
import bo.bosque.com.impexpap.dto.FilaVistaPropuestaDto;
import bo.bosque.com.impexpap.dto.ListaVistaPropuestaDto;
import bo.bosque.com.impexpap.dto.PrecioTonFamiliaDto;
import bo.bosque.com.impexpap.dto.ProductoDto;
import bo.bosque.com.impexpap.dto.ProductoExportDto;
import bo.bosque.com.impexpap.dto.PropuestaDto;
import bo.bosque.com.impexpap.dto.PropuestaItemNoCreadoDto;
import bo.bosque.com.impexpap.dto.VistaPropuestaDto;
import bo.bosque.com.impexpap.model.CostoIvaIt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.sf.jasperreports.engine.JRParameter;

import java.io.InputStream;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Los PDF del modulo de precios: los mismos reportes Jasper del JSF
 * ({@code tprAutorizacion/Autorizacion.xhtml}), con el mismo diseno.
 *
 * <table>
 *   <tr><th>Reporte</th><th>Aca</th><th>Datos</th></tr>
 *   <tr><td>rptPropuArt + subRptItemsNoCreados + subRptFamiliasNoActualizadas</td>
 *       <td>{@link #propuesta} (tipo 1)</td>
 *       <td>p_list_ArticuloProp F e I, p_list_propuesta E</td></tr>
 *   <tr><td>RptArtPropuPorArticulo + subRptItemsNoCreados</td>
 *       <td>{@link #propuesta} (tipo 2)</td>
 *       <td>p_list_ArticuloProp H, p_list_propuesta F</td></tr>
 *   <tr><td>RptPrecioActFam</td><td>{@link #preciosPorGrupo}</td><td>p_list_producto I</td></tr>
 *   <tr><td>RptTodoPrecioFam</td><td>{@link #preciosTodas}</td><td>p_list_producto K</td></tr>
 *   <tr><td>RptFamiliasActivas</td><td>{@link #familiasActivas}</td><td>p_list_producto P</td></tr>
 * </table>
 *
 * <p><b>Que cambio respecto del JSF.</b> Solo la forma de llenarlos: alla el reporte
 * ejecutaba el proc desde su {@code queryString} con la conexion que le pasaba el bean;
 * aca las filas las lee el DAO (SpHelper) y el jrxml no tiene SQL. Las filas se pasan
 * en el orden en que las devuelve el proc, que es el orden en que las imprimia el JSF.
 * El port de los jrxml lo hace {@code herramientas/portar_jrxml_legacy.py} (fuera del
 * repo) y se prueba en {@code ReportesPreciosSmokeTest}.
 *
 * <p><b>Lo que rptPropuArt agrega al del JSF</b> (pedido del 2026-09-24, sobre el diseno
 * del legacy): observaciones, quien hizo la propuesta y el costo de flete por sucursal en
 * la cabecera; cada articulo en una sola fila con su codigo, descripcion y UTM como celda
 * combinada sobre sus tres filas de valores, la familia combinada sobre sus articulos
 * ({@link #filasCombinadas}) y el costo repetido en cada articulo (2026-09-25); y el
 * precio propuesto marcado cuando sube o baja respecto del actual.
 *
 * <p><b>Las consultas remotas no tumban el PDF.</b> Los articulos no creados
 * (p_list_propuesta E/F) y las familias no actualizadas (p_list_ArticuloProp I) salen de
 * un OPENROWSET a otro servidor. Si ese servidor no contesta, el PDF sale igual con los
 * precios y sin ese subreporte, que es lo que hacia el JSF (se tragaba el error).
 */
@Component
public class ReportesPreciosService {

    private static final Logger logger = LoggerFactory.getLogger(ReportesPreciosService.class);

    static final String RPT_PROPUESTA_FAMILIA = "rptPropuArt";
    static final String RPT_PROPUESTA_ARTICULO = "RptArtPropuPorArticulo";
    static final String SUB_ITEMS = "subRptItemsNoCreados";
    static final String SUB_FAMILIAS = "subRptFamiliasNoActualizadas";
    static final String RPT_PRECIOS_GRUPO = "RptPrecioActFam";
    static final String RPT_PRECIOS_TODAS = "RptTodoPrecioFam";
    static final String RPT_FAMILIAS = "RptFamiliasActivas";

    private static final int LISTAS = 12;

    private final IArticuloPropuesto articuloPropuestoDao;
    private final IPropuesta propuestaDao;
    private final IProducto productoDao;
    private final IAutorizacion autorizacionDao;
    private final ICostoIncre costoIncreDao;
    private final ICostoIvaIt costoIvaItDao;
    private final IClasificacionPrecio clasificacionDao;
    private final JasperReportExport jasper;

    public ReportesPreciosService(IArticuloPropuesto articuloPropuestoDao,
                                  IPropuesta propuestaDao,
                                  IProducto productoDao,
                                  IAutorizacion autorizacionDao,
                                  ICostoIncre costoIncreDao,
                                  ICostoIvaIt costoIvaItDao,
                                  IClasificacionPrecio clasificacionDao,
                                  JasperReportExport jasper) {
        this.articuloPropuestoDao = articuloPropuestoDao;
        this.propuestaDao = propuestaDao;
        this.productoDao = productoDao;
        this.autorizacionDao = autorizacionDao;
        this.costoIncreDao = costoIncreDao;
        this.costoIvaItDao = costoIvaItDao;
        this.clasificacionDao = clasificacionDao;
        this.jasper = jasper;
    }

    /**
     * El PDF de una propuesta: rptPropuArt si es por familia (tipo 1),
     * RptArtPropuPorArticulo si es por articulo (tipo 2).
     */
    public byte[] propuesta(long idPropuesta) {
        PropuestaDto cabecera = propuestaDao.obtenerDetalle(idPropuesta);
        if (cabecera == null) {
            throw new SpBusinessException("La propuesta N.º " + idPropuesta + " no existe.");
        }
        Integer tipo = propuestaDao.obtenerTipo(idPropuesta);
        boolean porArticulo = tipo != null && tipo == 2;

        Map<String, Object> p = parametros();
        p.put("idPropuesta", (int) idPropuesta);

        if (porArticulo) {
            List<ArticuloPropuestoHDto> crudas = articuloPropuestoDao.listarReportePreciosPorArticulo(idPropuesta);
            exigirFilas(crudas, idPropuesta);
            Map<Integer, Long> ultimas = new HashMap<>();
            List<FilaRptPropuestaArticuloDto> filas = filasPorArticulo(crudas,
                    codigo -> ultimas.computeIfAbsent(codigo, this::ultimaPropuestaAprobada));
            cabecera(p, cabecera, idPropuesta);
            ponerSeccion(p, "itemsNoCreados", leerRemoto("los artículos no creados",
                    () -> propuestaDao.listarItemsNoCreadosPorArticulo(idPropuesta)), SIN_ITEMS);
            return jasper.exportPDFDesdeColeccionConSubreportes(RPT_PROPUESTA_ARTICULO, filas, p, SUB_ITEMS);
        }

        List<ArticuloPropuestoFDto> crudas = articuloPropuestoDao.listarComparativoPropuesta(idPropuesta);
        exigirFilas(crudas, idPropuesta);
        List<FilaRptPropuestaFamiliaDto> filas = filasCombinadas(crudas);
        cabecera(p, cabecera, idPropuesta);
        // La leyenda de colores solo tiene sentido si hay un precio actual contra el cual
        // comparar: una propuesta aprobada (o ya generada) trae solo el precio.
        p.put("conComparacion", filas.stream().anyMatch(FilaRptPropuestaFamiliaDto::isConFilaActual));
        ponerSeccion(p, "itemsNoCreados", leerRemoto("los artículos no creados",
                () -> propuestaDao.listarItemsNoCreadosPorFamilia(idPropuesta)), SIN_ITEMS);
        ponerSeccion(p, "familiasNoActualizadas", leerRemoto("las familias no actualizadas",
                () -> articuloPropuestoDao.listarFamiliasNoActualizadas(idPropuesta)), SIN_FAMILIAS);
        return jasper.exportPDFDesdeColeccionConSubreportes(RPT_PROPUESTA_FAMILIA, filas, p,
                SUB_ITEMS, SUB_FAMILIAS);
    }

    /**
     * La vista preliminar de una propuesta con las MISMAS filas que su PDF: se leen con los
     * mismos procedimientos (ramas F y H de {@code p_list_ArticuloProp}) y se arman con
     * {@link #filasCombinadas} y {@link #filasPorArticulo}. Asi la pantalla y el papel no
     * pueden contar dos historias distintas.
     *
     * <p>A diferencia del PDF, una propuesta sin articulos no es un error: la pantalla
     * dice que no tiene.
     */
    public VistaPropuestaDto vista(long idPropuesta) {
        if (propuestaDao.obtenerDetalle(idPropuesta) == null) {
            throw new SpBusinessException("La propuesta N.º " + idPropuesta + " no existe.");
        }
        Integer tipo = propuestaDao.obtenerTipo(idPropuesta);
        boolean porArticulo = tipo != null && tipo == 2;

        VistaPropuestaDto vista = new VistaPropuestaDto();
        vista.setTipo(porArticulo ? 2 : 1);
        vista.setListas(listasActivas());

        if (porArticulo) {
            Map<Integer, Long> ultimas = new HashMap<>();
            for (FilaRptPropuestaArticuloDto a : filasPorArticulo(
                    articuloPropuestoDao.listarReportePreciosPorArticulo(idPropuesta),
                    codigo -> ultimas.computeIfAbsent(codigo, this::ultimaPropuestaAprobada))) {
                FilaVistaPropuestaDto f = new FilaVistaPropuestaDto();
                f.setCodigoFamilia(a.getCodigoFamilia());
                f.setUltimaPropuesta(a.getUltimaPropuesta());
                f.setCodArticulo(a.getCodArticulo());
                f.setDescripcion(a.getDatoArticulo());
                f.setUtm(a.getUtm());
                f.setCostoTM(a.getCostoTM());
                for (int n = 1; n <= LISTAS; n++) {
                    f.getPropuestos().add(a.precio(n));
                }
                vista.getFilas().add(f);
            }
            ponerPreciosPorUnidad(vista, idPropuesta, true);
            return vista;
        }

        for (FilaRptPropuestaFamiliaDto a : filasCombinadas(
                articuloPropuestoDao.listarComparativoPropuesta(idPropuesta))) {
            FilaVistaPropuestaDto f = new FilaVistaPropuestaDto();
            f.setCodigoFamilia(a.getCodigoFamilia());
            f.setCodArticulo(a.getCodArticulo());
            f.setDescripcion(a.getDatoArt());
            f.setUtm(a.getUtm());
            f.setCostoTM(a.getCostoTM());
            f.setConFilaActual(a.isConFilaActual());
            for (int n = 1; n <= LISTAS; n++) {
                f.getPorcentajes().add(a.porcentaje(n));
                f.getPropuestos().add(a.propuesto(n));
                if (a.isConFilaActual()) {
                    f.getActuales().add(a.actual(n));
                    f.getCambios().add(a.cambio(n));
                }
            }
            vista.getFilas().add(f);
        }
        vista.setConComparacion(vista.getFilas().stream().anyMatch(FilaVistaPropuestaDto::isConFilaActual));
        ponerPreciosPorUnidad(vista, idPropuesta, false);
        return vista;
    }

    static final String SIN_PRECIOS_POR_UNIDAD =
            "No se pudo consultar el tipo de cambio en SAP: los precios por unidad no están "
                    + "disponibles. Los precios por tonelada sí.";

    /**
     * Los precios por unidad de cada articulo y lista: de la rama D por familia y de la G
     * por articulo, las MISMAS que lee el Excel de la generacion
     * ({@link GeneracionPropuestaService}). Asi lo que se ve en Bs es lo que se carga en
     * SAP. Productiva sale de {@code precioUnitBsProdpap}, como en el Excel: la otra
     * columna viene en dolares en la G con bitacora.
     *
     * <p>Las dos ramas piden el tipo de cambio a SAP por OPENROWSET. Si eso falla la vista
     * sigue: los precios por tonelada no dependen de SAP.
     */
    private void ponerPreciosPorUnidad(VistaPropuestaDto vista, long idPropuesta, boolean porArticulo) {
        Map<String, BigDecimal[]> porClave = new HashMap<>();
        Map<Integer, String> descripciones = new HashMap<>();
        try {
            if (porArticulo) {
                for (ArticuloPropuestoGDto g : articuloPropuestoDao.listarPreciosVigentes(idPropuesta)) {
                    anotarUnidad(porClave, descripciones, g.getCodArticulo(), g.getListaPrecio(),
                            g.getCodigoFamilia(), g.getGrpFam(), g.getProveedorExtSap(),
                            g.getPrecioUnitUsdIpx(), g.getPrecioUnitBsIpx(), g.getPrecioUnitBsProdpap());
                }
            } else {
                for (ArticuloPropuestoDDto d : articuloPropuestoDao.listarPreciosPropuestos(idPropuesta)) {
                    anotarUnidad(porClave, descripciones, d.getCodArticulo(), d.getListaPrecio(),
                            d.getCodigoFamilia(), d.getGrpFam(), d.getProveedorExtSap(),
                            d.getPrecioUnitUsdIpx(), d.getPrecioUnitBsIpx(), d.getPrecioUnitBsProdpap());
                }
            }
        } catch (RuntimeException ex) {
            logger.warn("Vista de la propuesta {}: no se pudieron leer los precios por unidad", idPropuesta, ex);
            vista.setAvisoPreciosPorUnidad(SIN_PRECIOS_POR_UNIDAD);
            return;
        }
        if (porClave.isEmpty()) return;

        BigDecimal tipoCambio = null;
        for (FilaVistaPropuestaDto f : vista.getFilas()) {
            f.setDescripcionFamilia(descripciones.getOrDefault(f.getCodigoFamilia(), ""));
            for (int n = 1; n <= LISTAS; n++) {
                BigDecimal[] u = porClave.get(claveUnidad(f.getCodArticulo(), n));
                f.getUnidadUsd().add(u == null ? null : u[0]);
                f.getUnidadBs().add(u == null ? null : u[1]);
                f.getUnidadBsProductiva().add(u == null ? null : u[2]);
                // El procedimiento no devuelve el tipo de cambio: se lee de la cuenta.
                if (tipoCambio == null && u != null && u[0] != null && u[1] != null
                        && u[0].signum() > 0) {
                    tipoCambio = u[1].divide(u[0], 4, RoundingMode.HALF_EVEN);
                }
            }
        }
        vista.setPreciosPorUnidad(true);
        vista.setTipoCambio(tipoCambio);
    }

    private static String claveUnidad(String codArticulo, Integer vpp) {
        return texto(codArticulo).trim() + "|" + vpp;
    }

    private static void anotarUnidad(Map<String, BigDecimal[]> porClave, Map<Integer, String> descripciones,
                                     String codArticulo, Integer vpp, Integer codigoFamilia,
                                     String grupo, String proveedor,
                                     BigDecimal usd, BigDecimal bs, BigDecimal bsProductiva) {
        if (vpp == null || vpp < 1 || vpp > LISTAS) return;
        porClave.put(claveUnidad(codArticulo, vpp), new BigDecimal[]{usd, bs, bsProductiva});
        if (codigoFamilia != null && !descripciones.containsKey(codigoFamilia)) {
            StringBuilder d = new StringBuilder(texto(grupo).trim());
            String prov = texto(proveedor).trim();
            if (!prov.isEmpty()) d.append(d.length() > 0 ? " · " : "").append(prov);
            if (d.length() > 0) descripciones.put(codigoFamilia, d.toString());
        }
    }

    /**
     * Las listas activas por VPP, con su sucursal. Si dos sucursales comparten un VPP (en
     * PRUEBA llego a pasar) queda la primera: en el PDF es una sola columna.
     */
    private List<ListaVistaPropuestaDto> listasActivas() {
        Map<Integer, String> porVpp = new TreeMap<>();
        for (ClasificacionPrecioDto c : clasificacionDao.obtenerClasificacionPrecioConSucursal()) {
            if (c.getVpp() == null || c.getVpp() < 1 || c.getVpp() > LISTAS) continue;
            if (c.getEstado() == null || c.getEstado() != 1) continue;
            porVpp.putIfAbsent(c.getVpp(), texto(c.getNombreSucursal()).trim());
        }
        List<ListaVistaPropuestaDto> listas = new ArrayList<>();
        porVpp.forEach((vpp, sucursal) -> listas.add(new ListaVistaPropuestaDto(vpp, sucursal)));
        return listas;
    }

    /** Precios por tonelada vigentes de las familias de un grupo de familia SAP. */
    public byte[] preciosPorGrupo(long idGrpFamiliaSap) {
        List<PrecioTonFamiliaDto> filas = productoDao.listarPreciosPivotPorGrupoFamilia(idGrpFamiliaSap);
        if (filas.isEmpty()) {
            throw new SpBusinessException("El grupo no tiene familias con precio.");
        }
        Map<String, Object> p = parametros();
        p.put("idFam", (int) idGrpFamiliaSap);
        p.put("logoEmpresa", logo());
        return jasper.exportPDFDesdeColeccion(RPT_PRECIOS_GRUPO, filas, p);
    }

    /** Precios por tonelada vigentes de todas las familias activas. */
    public byte[] preciosTodas() {
        List<PrecioTonFamiliaDto> filas = productoDao.listarPreciosPivotTodos();
        if (filas.isEmpty()) {
            throw new SpBusinessException("No hay familias activas con precio.");
        }
        Map<String, Object> p = parametros();
        p.put("logoEmpresa", logo());
        return jasper.exportPDFDesdeColeccion(RPT_PRECIOS_TODAS, filas, p);
    }

    /** El catalogo de familias activas. */
    public byte[] familiasActivas() {
        List<ProductoExportDto> filas = productoDao.listarParaExportar();
        if (filas.isEmpty()) {
            throw new SpBusinessException("No hay familias activas.");
        }
        Map<String, Object> p = parametros();
        p.put("logoEmpresa", logo());
        return jasper.exportPDFDesdeColeccion(RPT_FAMILIAS, filas, p);
    }

    /**
     * Las filas de la rama F juntadas por articulo, en el orden en que las devuelve el
     * proc (familia, articulo): una fila por articulo con sus tres conceptos.
     *
     * <p>Marca el primero y el ultimo articulo de cada familia -el jrxml combina ahi la
     * celda de familia- y, por lista, si el precio propuesto sube o baja respecto
     * del actual.
     */
    static List<FilaRptPropuestaFamiliaDto> filasCombinadas(List<ArticuloPropuestoFDto> datos) {
        Map<String, FilaRptPropuestaFamiliaDto> porArticulo = new LinkedHashMap<>();
        for (ArticuloPropuestoFDto d : datos) {
            String clave = d.getCodigoFamilia() + "|" + texto(d.getCodArticulo()).trim();
            FilaRptPropuestaFamiliaDto f = porArticulo.get(clave);
            if (f == null) {
                f = new FilaRptPropuestaFamiliaDto();
                f.setCodigoFamilia(d.getCodigoFamilia());
                f.setCodArticulo(texto(d.getCodArticulo()).trim());
                f.setDatoArt(texto(d.getDatoArt()).trim());
                f.setUtm(d.getUtm());
                f.setCostoTM(d.getCostoTM());
                porArticulo.put(clave, f);
            }
            if (f.getCostoTM() == null) {
                f.setCostoTM(d.getCostoTM());
            }
            String det = texto(d.getDet()).trim();
            String clase = det.toLowerCase(Locale.ROOT);
            for (int n = 1; n <= LISTAS; n++) {
                BigDecimal v = d.valorDeLista(n);
                if (clase.startsWith("porcentaje")) {
                    f.ponerPorcentaje(n, v);
                } else if (clase.contains("actual")) {
                    f.ponerActual(n, v);
                } else {
                    // "Precios Ton. Propuesto", o "Precios Ton." en una propuesta aprobada.
                    f.ponerPropuesto(n, v);
                }
            }
            if (clase.startsWith("porcentaje")) {
                f.setEtiqueta1(det);
            } else if (clase.contains("actual")) {
                f.setEtiqueta2(det);
                f.setConFilaActual(true);
            } else {
                f.setEtiqueta3(det);
            }
        }

        List<FilaRptPropuestaFamiliaDto> filas = new ArrayList<>(porArticulo.values());
        for (int i = 0; i < filas.size(); i++) {
            FilaRptPropuestaFamiliaDto f = filas.get(i);
            Integer familia = f.getCodigoFamilia();
            f.setPrimeroDeFamilia(i == 0 || !mismo(familia, filas.get(i - 1).getCodigoFamilia()));
            f.setUltimoDeFamilia(i == filas.size() - 1 || !mismo(familia, filas.get(i + 1).getCodigoFamilia()));
            if (f.getEtiqueta1() == null) f.setEtiqueta1("Porcentajes %");
            if (f.isConFilaActual()) {
                if (f.getEtiqueta3() == null) f.setEtiqueta3("Precios Ton. Propuesto");
                for (int n = 1; n <= LISTAS; n++) {
                    BigDecimal actual = f.actual(n);
                    BigDecimal propuesto = f.propuesto(n);
                    f.ponerCambio(n, actual == null || propuesto == null ? null : propuesto.compareTo(actual));
                }
            } else {
                // Aprobada: la segunda fila es la del precio ("Precios Ton.").
                f.setEtiqueta2(f.getEtiqueta3() == null ? "Precios Ton." : f.getEtiqueta3());
                f.setEtiqueta3(null);
            }
        }
        return filas;
    }

    /** Los datos de la cabecera, iguales en los dos reportes de propuesta. */
    private void cabecera(Map<String, Object> p, PropuestaDto cabecera, long idPropuesta) {
        p.put("logoEmpresa", logo());
        p.put("titulo", texto(cabecera.getTitulo()));
        p.put("obs", texto(cabecera.getObs()));
        p.put("propuestoPor", propuestoPor(idPropuesta));
        p.put("fletes", fletes(idPropuesta));
        BigDecimal[] ivaIt = ivaIt(idPropuesta);
        p.put("iva", porcentaje(ivaIt[0]));
        p.put("it", porcentaje(ivaIt[1]));
    }

    /**
     * Las filas de la rama H: una por articulo, agrupadas por familia -el jrxml combina la
     * celda de familia y ultima propuesta- y con la ultima propuesta aprobada de cada
     * familia.
     *
     * <p>La rama no ordena: se agrupa por familia en el orden en que aparece cada una, sin
     * tocar el orden de los articulos dentro de ella.
     */
    static List<FilaRptPropuestaArticuloDto> filasPorArticulo(List<ArticuloPropuestoHDto> datos,
                                                              Function<Integer, Long> ultimaPropuesta) {
        Map<Integer, List<ArticuloPropuestoHDto>> porFamilia = new LinkedHashMap<>();
        for (ArticuloPropuestoHDto h : datos) {
            porFamilia.computeIfAbsent(h.getCodigoFamilia(), k -> new ArrayList<>()).add(h);
        }
        List<FilaRptPropuestaArticuloDto> filas = new ArrayList<>();
        for (Map.Entry<Integer, List<ArticuloPropuestoHDto>> e : porFamilia.entrySet()) {
            List<ArticuloPropuestoHDto> articulos = e.getValue();
            Long ultima = e.getKey() == null ? null : ultimaPropuesta.apply(e.getKey());
            for (int i = 0; i < articulos.size(); i++) {
                ArticuloPropuestoHDto h = articulos.get(i);
                FilaRptPropuestaArticuloDto f = new FilaRptPropuestaArticuloDto();
                f.setCodigoFamilia(h.getCodigoFamilia());
                f.setUltimaPropuesta(ultima);
                f.setCostoTM(h.getCostoTM());
                f.setCodArticulo(texto(h.getCodArticulo()).trim());
                f.setDatoArticulo(texto(h.getDatoArticulo()).trim());
                f.setUtm(h.getUtm());
                f.setPrimeroDeFamilia(i == 0);
                f.setUltimoDeFamilia(i == articulos.size() - 1);
                for (int n = 1; n <= LISTAS; n++) {
                    f.ponerPrecio(n, h.precioDeLista(n));
                }
                filas.add(f);
            }
        }
        return filas;
    }

    /**
     * La ultima propuesta aprobada de la familia: tpr_producto.idPropuestaAprobada, que se
     * actualiza al aprobar (en PRUEBA coincide en las 704 familias con la ultima aprobada
     * que la contiene). Cero o nulo es "nunca tuvo una".
     */
    private Long ultimaPropuestaAprobada(Integer codigoFamilia) {
        try {
            ProductoDto familia = productoDao.obtenerConDescripcion(codigoFamilia);
            Long id = familia == null ? null : familia.getIdPropuestaAprobada();
            return id == null || id <= 0 ? null : id;
        } catch (RuntimeException ex) {
            logger.warn("No se pudo leer la ultima propuesta aprobada de la familia {}", codigoFamilia, ex);
            return null;
        }
    }

    private static boolean mismo(Integer a, Integer b) {
        return a == null ? b == null : a.equals(b);
    }

    /** "JAIMES MARCELO JAVIER el 20/08/2026", del mismo listado que muestra la pantalla. */
    private String propuestoPor(long idPropuesta) {
        try {
            for (AutorizacionDto a : autorizacionDao.listAutorizacion()) {
                if (a.getIdPropuesta() != null && a.getIdPropuesta() == idPropuesta) {
                    String quien = texto(a.getDatoPersonaP()).trim();
                    Date cuando = a.getAudFechaPropuesta();
                    String fecha = cuando == null ? "" : new SimpleDateFormat("dd/MM/yyyy").format(cuando);
                    if (quien.isEmpty()) return fecha;
                    return fecha.isEmpty() ? quien : quien + " el " + fecha;
                }
            }
        } catch (RuntimeException ex) {
            // Es solo la cabecera: sin el nombre el PDF sirve igual.
            logger.warn("No se pudo leer quien hizo la propuesta {}", idPropuesta, ex);
        }
        return "";
    }

    /** "Central: 0,00     Sucursal 6 Cochabamba: 15,00     ..." con el flete de cada sucursal. */
    private String fletes(long idPropuesta) {
        try {
            List<CostoIncreSucursalDto> fletes = costoIncreDao.listarPorPropuesta(idPropuesta);
            return textoFletes(fletes == null ? Collections.<CostoIncreSucursalDto>emptyList() : fletes);
        } catch (RuntimeException ex) {
            logger.warn("No se pudieron leer los fletes de la propuesta {}", idPropuesta, ex);
            return "";
        }
    }

    /**
     * El IVA y el IT con que se calcularon los precios: la fila de tpr_costoIvaIt de la
     * propuesta si la tiene, si no la vigente (p_list_costoIvaIt 'V').
     */
    private BigDecimal[] ivaIt(long idPropuesta) {
        try {
            List<CostoIvaIt> propios = costoIvaItDao.listarPorPropuesta(idPropuesta);
            if (propios != null) {
                for (CostoIvaIt c : propios) {
                    if (c.getIdPropuesta() != null && c.getIdPropuesta() == idPropuesta) {
                        return new BigDecimal[]{c.getIva(), c.getIt()};
                    }
                }
            }
            CostoIvaItDto vigente = costoIvaItDao.obtenerVigente();
            if (vigente != null) {
                return new BigDecimal[]{vigente.getIva(), vigente.getIt()};
            }
        } catch (RuntimeException ex) {
            logger.warn("No se pudo leer el IVA/IT para la propuesta {}", idPropuesta, ex);
        }
        return new BigDecimal[]{null, null};
    }

    /**
     * El idioma de los numeros de los reportes del modulo: coma para los miles y punto
     * para los decimales (1,615.85). Lo pidio el usuario el 2026-09-25, como en la
     * planilla del gerente. Solo estos reportes: {@link JasperReportExport} pone "es"
     * cuando el reporte no trae idioma, y los de los otros modulos siguen asi.
     */
    static final Locale IDIOMA_REPORTES = Locale.US;

    /** Los parametros de un reporte del modulo, ya con su idioma. */
    private static Map<String, Object> parametros() {
        Map<String, Object> p = new HashMap<>();
        p.put(JRParameter.REPORT_LOCALE, IDIOMA_REPORTES);
        return p;
    }

    /** "15.476 %": hasta tres decimales, sin ceros de relleno. */
    static String porcentaje(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        DecimalFormat formato = new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(IDIOMA_REPORTES));
        return formato.format(valor) + " %";
    }

    static String textoFletes(List<CostoIncreSucursalDto> fletes) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(IDIOMA_REPORTES));
        List<String> partes = new ArrayList<>();
        for (CostoIncreSucursalDto f : fletes) {
            String nombre = texto(f.getNombre()).trim();
            BigDecimal valor = f.getValor() == null ? BigDecimal.ZERO : f.getValor();
            partes.add((nombre.isEmpty() ? "Sucursal " + f.getCodSucursal() : nombre) + ": " + formato.format(valor));
        }
        return String.join("     ", partes);
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }

    /** Sin filas el JSF sacaba una hoja en blanco; aca se dice por que. */
    private static void exigirFilas(List<?> filas, long idPropuesta) {
        if (filas == null || filas.isEmpty()) {
            throw new SpBusinessException("La propuesta N.º " + idPropuesta
                    + " todavía no tiene precios cargados: no hay nada que imprimir.");
        }
    }

    /** Una lectura que depende del servidor remoto: si falla, el subreporte sale vacio. */
    /** Lo que dice la seccion de articulos no creados cuando no hay ninguno. */
    static final String SIN_ITEMS =
            "No hay artículos pendientes: todos están creados en las tres empresas y tienen unidad de medida.";

    /** Lo que dice la seccion de familias no actualizadas cuando no hay ninguna. */
    static final String SIN_FAMILIAS =
            "Todas las familias del mismo grupo y tipo están en la propuesta.";

    /**
     * Lo que dice una seccion cuando el servidor remoto no respondio. Tiene que decirlo:
     * una lista vacia por falla se leeria como "ninguno", y eso seria falso.
     */
    static final String NO_CONSULTADO =
            "No se pudo consultar el servidor de SAP: esta lista no está disponible. "
                    + "Vuelva a generar el PDF más tarde.";

    /** Una lectura al servidor remoto: sus filas, o que no respondio. */
    static final class Remoto<T> {
        final List<T> filas;
        final boolean fallo;

        Remoto(List<T> filas, boolean fallo) {
            this.filas = filas;
            this.fallo = fallo;
        }
    }

    /**
     * Las secciones de articulos no creados y familias no actualizadas salen de un
     * OPENROWSET a otro servidor. Si no responde, el PDF sale igual y la seccion lo dice.
     */
    private static <T> Remoto<T> leerRemoto(String que, Supplier<List<T>> lectura) {
        try {
            List<T> r = lectura.get();
            return new Remoto<>(r == null ? Collections.<T>emptyList() : r, false);
        } catch (RuntimeException ex) {
            logger.warn("El PDF de la propuesta sale sin {}: el servidor remoto no respondió", que, ex);
            return new Remoto<>(Collections.<T>emptyList(), true);
        }
    }

    /**
     * Las filas de una seccion y el mensaje que muestra si no tiene ninguna. Las dos
     * secciones se imprimen siempre (pedido del usuario): sin filas, su subreporte muestra
     * el titulo y este mensaje.
     */
    static <T> void ponerSeccion(Map<String, Object> p, String parametro, Remoto<T> leido, String siVacia) {
        p.put(parametro, leido.filas);
        p.put(parametro + "Mensaje", leido.fallo ? NO_CONSULTADO : siVacia);
    }

    /** Un stream nuevo por reporte: el llenado lo consume. */
    private InputStream logo() {
        return getClass().getResourceAsStream("/logos/logoEmpresa.jpg");
    }
}
