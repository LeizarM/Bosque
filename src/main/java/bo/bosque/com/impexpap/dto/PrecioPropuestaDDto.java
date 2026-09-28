package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_precioPropuesta ACCION 'D'</b>: las filas de una propuesta
 * para UNA familia, que es lo que se edita en la pantalla de precios propuestos.
 *
 * <p>Filtra por {@code @idPropuesta} + {@code @codigoFamilia} (este ultimo comparado contra
 * {@code tpr_precio.codigoFamilia}, que es int, no contra el varchar(15) de
 * tpr_precioPropuesta).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrecioPropuestaDDto {

    /** tpr_precioPropuesta.idPrecioPropuesto. */
    private Long idPrecioPropuesto;

    /**
     * Alias {@code codigo} del SP, pero el dato es el <b>codigoFamilia</b> de tpr_precio
     * (int). Se llama codigo porque asi lo nombra el SELECT y el mapeo es por nombre.
     */
    private Integer codigo;

    /** tb_sucursal.nombre. */
    private String nombre;

    /** tpr_clasificacionPrecio.nombrePrecio (el bueno, no el denormalizado de la tabla). */
    private String nombrePrecio;

    /** tpr_clasificacionPrecio.vpp (el bueno, derivado por JOIN). */
    private Integer vpp;

    /**
     * Subconsulta {@code select idClasificacion from tpr_clasificacionPrecio where vpp=cp.vpp}:
     * si hay mas de una clasificacion con el mismo vpp, el SP revienta (subconsulta que
     * devuelve mas de una fila). Reportado como riesgo; no es un dato nuevo, sale del mismo
     * JOIN que ya trae cp.
     */
    private Long idClasificacion;

    /** tpr_porcentaje.porcen resuelto por subconsulta, con ISNULL(...,0). */
    private BigDecimal porcentaje;

    /** Precio vigente. */
    private BigDecimal precioActual;

    /** Precio propuesto (editable). */
    private BigDecimal precioPropuesto;
}
