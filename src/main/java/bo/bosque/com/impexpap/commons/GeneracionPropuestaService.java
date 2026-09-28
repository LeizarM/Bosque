package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IArticuloPropuesto;
import bo.bosque.com.impexpap.dao.IPropuesta;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoDDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoGDto;
import bo.bosque.com.impexpap.dto.PropuestaDto;
import bo.bosque.com.impexpap.model.Propuesta;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * "Generar" una propuesta aprobada: el archivo CambioDePrecios.xlsx con el precio
 * unitario de cada articulo en cada lista de precios de SAP, para cargarlo en las
 * empresas, y la constancia de quien lo genero y cuando
 * ({@code p_abm_propuesta 'B'}: {@code audUsGenerado} y {@code audFecGenerado}).
 *
 * <p>Reemplaza al dialogo "GEN. PROPUESTA" (dlgPropG, propuestas por familia) y al
 * boton EXCEL de "Ver Articulos" (dlgPropH, propuestas por articulo) del JSF. Alli el
 * {@code p:dataExporter} bajaba la grilla como .xls y el {@code actionListener}
 * {@code exportAct} registraba la generacion; aca las dos cosas salen de una sola
 * llamada, asi que no hay archivo sin constancia ni constancia sin archivo.
 *
 * <p><b>Las filas son las del legacy.</b> Por familia, {@code p_list_ArticuloProp 'D'}
 * (el precio propuesto); por articulo, la rama {@code 'G'} (el precio vigente de la
 * familia, que es el que toma el articulo nuevo). Las dos traen las mismas 19 columnas
 * y el archivo lleva las siete que el JSF exportaba, en el mismo orden y con los
 * rotulos de dlgPropG.
 *
 * <p><b>Las columnas son las de la consulta, tal cual.</b> En la rama G con bitacora
 * ({@code tpr_bitArticulo}: las propuestas por articulo ya aprobadas) el legacy exportaba
 * la lista SAP de IPX convertida dos veces, la de Productiva sin convertir en las
 * aprobadas antes del 2026-01-19, y el precio Productiva en dolares. Se corrigio en la
 * consulta (punto 20 de tpr_ArticuloPropuesto.sql), asi el JSF y la app exportan lo
 * mismo. El precio Productiva se lee de {@code precioUnitBsProdpap}, que es el mismo
 * valor que la columna del JSF una vez corregida y que siempre estuvo bien.
 *
 * <p>Se puede volver a generar, como en el JSF: cada generacion pisa quien y cuando.
 */
@Service
public class GeneracionPropuestaService {

    private static final Logger log = LoggerFactory.getLogger(GeneracionPropuestaService.class);

    static final String HOJA = "CambioDePrecios";

    /** Los rotulos de dlgPropG, en su orden. "Moneda BS" va dos veces, como alli. */
    static final String[] COLUMNAS = {
            "Codigo Articulo",
            "Lista Precio SAP(IPX)",
            "Precio Unit. IPX (BS)",
            "Moneda BS",
            "Lista Precio SAP(PRODUCTIVA)",
            "Precio Unit. PRODUCTIVA (BS)",
            "Moneda BS",
    };

    /** El JSF mostraba y exportaba los precios con cuatro decimales. */
    private static final int DECIMALES = 4;

    private static final int ESTADO_APROBADA = 1;
    private static final int TIPO_POR_ARTICULO = 2;

    private final IPropuesta propuestaDao;
    private final IArticuloPropuesto articuloPropuestoDao;

    public GeneracionPropuestaService(IPropuesta propuestaDao, IArticuloPropuesto articuloPropuestoDao) {
        this.propuestaDao = propuestaDao;
        this.articuloPropuestoDao = articuloPropuestoDao;
    }

    /**
     * Arma el archivo y, solo si salio, registra la generacion.
     *
     * @return el .xlsx
     * @throws SpBusinessException si la propuesta no existe, no esta aprobada, no tiene
     *                             articulos con precio o el procedimiento rechaza el registro
     */
    public byte[] generar(long idPropuesta, long audUsuario) {
        Propuesta propuesta = propuestaDao.obtenerPorId(idPropuesta);
        if (propuesta == null) {
            throw new SpBusinessException("La propuesta N.º " + idPropuesta + " no existe.");
        }
        PropuestaDto detalle = propuestaDao.obtenerDetalle(idPropuesta);
        Integer estado = detalle != null ? detalle.getEsAprobada() : null;
        if (estado == null || estado != ESTADO_APROBADA) {
            throw new SpBusinessException("La propuesta N.º " + idPropuesta + " "
                    + describirEstado(estado) + ": solo se genera una propuesta aprobada.");
        }

        boolean porArticulo = propuesta.getTipo() != null && propuesta.getTipo() == TIPO_POR_ARTICULO;
        List<Fila> filas = filasDe(idPropuesta, porArticulo);
        if (filas.isEmpty()) {
            throw new SpBusinessException("La propuesta N.º " + idPropuesta
                    + " no tiene artículos con precio para generar.");
        }
        byte[] archivo = libro(filas, COLUMNAS);

        RespuestaSp res = propuestaDao.registrarGeneracion(idPropuesta, audUsuario);
        if (!res.isExitoso()) {
            throw new SpBusinessException(res.getErrormsg());
        }
        log.info("Propuesta {} generada por el usuario {}: {} filas.", idPropuesta, audUsuario, filas.size());
        return archivo;
    }

    private List<Fila> filasDe(long idPropuesta, boolean porArticulo) {
        List<Fila> filas = new ArrayList<>();
        if (porArticulo) {
            for (ArticuloPropuestoGDto g : articuloPropuestoDao.listarPreciosVigentes(idPropuesta)) {
                filas.add(new Fila(g.getCodArticulo(), g.getListNumIpx(), g.getPrecioUnitBsIpx(), g.getMonedaBs(),
                        g.getListNum(), g.getPrecioUnitBsProdpap(), g.getMonedaUsd()));
            }
        } else {
            for (ArticuloPropuestoDDto d : articuloPropuestoDao.listarPreciosPropuestos(idPropuesta)) {
                filas.add(new Fila(d.getCodArticulo(), d.getListNumIpx(), d.getPrecioUnitBsIpx(), d.getMonedaBs(),
                        d.getListNum(), d.getPrecioUnitBsProdpap(), d.getMonedaUsd()));
            }
        }
        return filas;
    }

    /** El .xlsx: una hoja, la fila de rotulos y una fila por articulo y lista. */
    static byte[] libro(List<Fila> filas, String[] columnas) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(HOJA);

            CellStyle rotulo = libro.createCellStyle();
            Font negrita = libro.createFont();
            negrita.setBold(true);
            rotulo.setFont(negrita);
            CellStyle precio = libro.createCellStyle();
            precio.setDataFormat(libro.createDataFormat().getFormat("#,##0.0000"));

            Row cabecera = hoja.createRow(0);
            for (int c = 0; c < columnas.length; c++) {
                Cell celda = cabecera.createCell(c);
                celda.setCellValue(columnas[c]);
                celda.setCellStyle(rotulo);
            }

            int r = 1;
            for (Fila f : filas) {
                Row fila = hoja.createRow(r++);
                texto(fila, 0, f.codArticulo);
                entero(fila, 1, f.listaIpx);
                importe(fila, 2, f.precioIpx, precio);
                texto(fila, 3, f.monedaIpx);
                entero(fila, 4, f.listaProductiva);
                importe(fila, 5, f.precioProductiva, precio);
                texto(fila, 6, f.monedaProductiva);
            }

            // Anchos fijos y no autoSizeColumn: este mide con las fuentes del sistema, y
            // en el contenedor no tiene por que haber las mismas que en Windows.
            int[] anchos = {22, 22, 22, 12, 30, 30, 12};
            for (int c = 0; c < anchos.length; c++) {
                hoja.setColumnWidth(c, anchos[c] * 256);
            }
            hoja.createFreezePane(0, 1);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo armar el archivo de la generación.", e);
        }
    }

    private static void texto(Row fila, int c, String valor) {
        if (valor != null) {
            fila.createCell(c).setCellValue(valor.trim());
        }
    }

    private static void entero(Row fila, int c, Integer valor) {
        if (valor != null) {
            fila.createCell(c).setCellValue(valor);
        }
    }

    /**
     * Redondeado a cuatro decimales, como la columna del JSF ({@code f:convertNumber}
     * con #,##0.0000, que redondea al par). Numero y no texto: el JSF exportaba el
     * texto formateado y Excel lo dejaba como texto.
     */
    private static void importe(Row fila, int c, BigDecimal valor, CellStyle estilo) {
        if (valor != null) {
            Cell celda = fila.createCell(c);
            celda.setCellValue(valor.setScale(DECIMALES, RoundingMode.HALF_EVEN).doubleValue());
            celda.setCellStyle(estilo);
        }
    }

    private static String describirEstado(Integer estado) {
        if (estado == null) return "no tiene estado de autorización";
        switch (estado) {
            case 0: return "está pendiente";
            case 2: return "fue rechazada";
            case 3: return "está en espera de autorización";
            default: return "tiene un estado desconocido (" + estado + ")";
        }
    }

    /** Las siete columnas del archivo, de la rama D o de la G. */
    static final class Fila {
        final String codArticulo;
        final Integer listaIpx;
        final BigDecimal precioIpx;
        final String monedaIpx;
        final Integer listaProductiva;
        final BigDecimal precioProductiva;
        final String monedaProductiva;

        Fila(String codArticulo, Integer listaIpx, BigDecimal precioIpx, String monedaIpx,
             Integer listaProductiva, BigDecimal precioProductiva, String monedaProductiva) {
            this.codArticulo = codArticulo;
            this.listaIpx = listaIpx;
            this.precioIpx = precioIpx;
            this.monedaIpx = monedaIpx;
            this.listaProductiva = listaProductiva;
            this.precioProductiva = precioProductiva;
            this.monedaProductiva = monedaProductiva;
        }
    }
}
