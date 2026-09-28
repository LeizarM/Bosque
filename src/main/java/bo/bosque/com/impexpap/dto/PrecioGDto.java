package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_precio @ACCION = 'G'</b> — los precios de
 * una familia dentro de una propuesta concreta (precio actual contra precio
 * propuesto). 14 columnas.
 *
 * <p>Sale casi todo de tpr_precioPropuesta; tpr_precio solo aporta el enlace.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrecioGDto {

    /** tpr_precioPropuesta.idPrecioPropuesto (sin "a" final, asi esta en la tabla). */
    private Long idPrecioPropuesto;

    /** tpr_precioPropuesta.idPropuesta. */
    private Long idPropuesta;

    /** tpr_precio.idPrecio. */
    private Long idPrecio;

    /** tb_sucursal.codSucursal. */
    private Long codSucursal;

    /** Nombre de la SUCURSAL. */
    private String nombre;

    /** tpr_clasificacionPrecio.nombrePrecio. */
    private String nombrePrecio;

    /** tpr_clasificacionPrecio.vpp. */
    private Integer vpp;

    /**
     * Trampa de nombre: el SP le pone alias {@code codigo} a una subconsulta
     * que devuelve {@code tpr_precio.codigoFamilia}. Es el codigo de familia,
     * y la propiedad tiene que llamarse igual que el alias o no mapea.
     */
    private Integer codigo;

    /** tpr_clasificacionPrecio.listNum. */
    private Long listNum;

    /** tpr_precioPropuesta.precioActual. */
    private BigDecimal precioActual;

    /** tpr_precioPropuesta.precioPropuesto. */
    private BigDecimal precioPropuesto;

    /** tpr_precioPropuesta.porcentaje (la columna real, no la subconsulta comentada del SP). */
    private BigDecimal porcentaje;

    /** SELECT TOP 1 iva FROM tpr_costoIvaIt. */
    private BigDecimal iva;

    /** SELECT TOP 1 it FROM tpr_costoIvaIt. */
    private BigDecimal it;
}
