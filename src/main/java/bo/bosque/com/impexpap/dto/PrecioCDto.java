package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_precio @ACCION = 'C'</b> — los precios de
 * una familia, uno por clasificacion/sucursal. 7 columnas.
 *
 * <p>Es la version corta de la rama D: sirve para elegir una lista de precio,
 * no para calcular.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrecioCDto {

    /** tpr_precio.codigoFamilia (int). */
    private Integer codigoFamilia;

    /** tb_sucursal.codSucursal (bigint). */
    private Long codSucursal;

    /** Nombre de la SUCURSAL (tb_sucursal.nombre), no del precio. */
    private String nombre;

    /** tpr_clasificacionPrecio.idClasificacion. */
    private Long idClasificacion;

    /**
     * Viene concatenado por el SP: {@code c.nombrePrecio + ' ' + convert(varchar(10), c.vpp)}.
     * O sea "Precio Mayorista 3", no el nombre limpio.
     */
    private String nombrePrecio;

    /** tpr_precio.idPrecio. */
    private Long idPrecio;

    /** tpr_precio.precio. */
    private BigDecimal precio;
}
