package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_precio @ACCION = 'A'</b> — "datos para
 * capturar": la ficha de una familia con su precio, para llenar el formulario.
 *
 * <p>8 columnas. Los nombres son los alias literales del SELECT, en camelCase:
 * BeanPropertyRowMapper mapea por nombre de columna, no por posicion.
 *
 * <p><b>Ojo con esta rama:</b> llama {@code proveedor} y {@code grupoFam} a lo
 * que las ramas D/E llaman {@code proveedorExtSap} y {@code grpFam}. Es el
 * mismo dato con dos nombres distintos, por eso no se puede reusar un solo DTO
 * entre ramas.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrecioADto {

    /** tpr_producto.codigoFamilia (int). */
    private Integer codigoFamilia;

    /** Id crudo de la presentacion, NO el texto. Hay que resolverlo aparte. */
    private Long idPresentacion;

    /** ISNULL(po.proveedorExtSap, '-Sin grupo de proveedor SAP Asignado-'). */
    private String proveedor;

    /** ISNULL(g.grpFam, '-Sin grupo de Familia SAP Asignado-'). */
    private String grupoFam;

    /** tpr_producto.gramaje: varchar(50), no numerico. */
    private String gramaje;

    /** tpr_color.color. */
    private String color;

    /** tpr_precio.precio. float(53) en la base, BigDecimal aca. */
    private BigDecimal precio;

    /** tpr_clasificacionPrecio.nombrePrecio. */
    private String nombrePrecio;
}
