package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_ArticuloProp ACCION 'I'</b>: las familias que
 * comparten grupo SAP y tipo con la propuesta pero que NO se estan actualizando en ella
 * (el "que me esta faltando tocar"). Los totales de items y disponible salen de un
 * OPENROWSET al servidor 192.168.3.114 volcado en una tabla variable.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPropuestoIDto implements Serializable {

    private Integer codigoFamilia;

    /** tpr_producto.costoTM, float(53). */
    private BigDecimal costoTM;

    /** ISNULL(tpr_proveedorExtSap.proveedorExtSap, '-Sin proveedor SAP asignado-'). */
    private String proveedor;

    /** ISNULL(tpr_grupoFamiliaSap.grpFam, '-Sin grupo de familia SAP Asignado-'). */
    private String familia;

    private String presentacion;
    private String tipo;

    /** Texto armado: "[min - max]" del rango de gramaje. */
    private String rangoGram;

    /** Viene del OPENROWSET remoto (p_list_FamiliasXCantidadItem). */
    private Integer totalItems;

    /** decimal(19,6) en la tabla variable del proc. */
    private BigDecimal totalDisponible;
}
