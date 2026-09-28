package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * La vista preliminar de una propuesta tal como la imprime el PDF: las mismas filas que
 * arman rptPropuArt (por familia) y RptArtPropuPorArticulo (por articulo), con las listas
 * de precio activas para rotular las columnas.
 *
 * <p>Sale de {@code ReportesPreciosService.vista}, que lee con los mismos procedimientos
 * y las mismas reglas que el PDF: lo que se ve en la pantalla es lo que se imprime.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class VistaPropuestaDto implements Serializable {

    /** 1 por familia, 2 por articulo. */
    private Integer tipo;

    /**
     * Si hay precio actual contra el cual comparar. Como en el PDF, una propuesta aprobada
     * o generada trae solo el precio y no lleva la leyenda de colores.
     */
    private boolean conComparacion;

    /**
     * Si trae los precios por unidad (Bs, USD, Bs Productiva). Salen de la rama D (por
     * familia) o G (por articulo), las mismas que lee el Excel de la generacion.
     */
    private boolean preciosPorUnidad;

    /**
     * El tipo de cambio con que se convirtieron: el ultimo USD de SAP (IMPEXPAP ortt), que
     * es el que usa el procedimiento. Null sin precios por unidad.
     */
    private BigDecimal tipoCambio;

    /** Si los precios por unidad no se pudieron leer, por que. */
    private String avisoPreciosPorUnidad;

    /** Las listas activas por VPP, en orden: rotulan las doce columnas. */
    private List<ListaVistaPropuestaDto> listas = new ArrayList<>();

    /** Un articulo por fila, agrupados por familia en el orden del PDF. */
    private List<FilaVistaPropuestaDto> filas = new ArrayList<>();
}
