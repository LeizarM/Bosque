package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Mapea la tabla <b>dbo.tpr_precio</b> — el precio vigente de una familia de
 * producto para una clasificacion de precio (lista/sucursal).
 *
 * <p><b>Contiene EXACTAMENTE las 6 columnas de la tabla, ni una mas.</b>
 * {@code SpHelper.ejecutarAbm} serializa este POJO con Jackson y manda cada
 * campo como parametro de {@code p_abm_precio}; un campo que no exista como
 * {@code @parametro} del procedimiento hace fallar el EXEC entero.
 * Los campos de display que vienen de JOIN (nombre de sucursal, proveedor,
 * grupo de familia, vpp, iva, it...) viven en los DTO {@code Precio*Dto},
 * nunca aca.
 *
 * <p>Version anterior de esta clase: tenia 23 campos, 17 de ellos inexistentes
 * en la tabla, cargados a mano por indice desde la rama D de
 * {@code p_list_precio}. Esos 17 se movieron a {@link bo.bosque.com.impexpap.dto.PrecioDDto}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Precio implements Serializable {

    /** PK. bigint IDENTITY. Wrapper y no primitivo: en un alta viaja en null. */
    private Long idPrecio;

    /**
     * FK -> tpr_producto.codigoFamilia. Trampa: aca es <b>int</b>, pero en
     * tpr_precioPropuesta la columna homonima es varchar(15).
     */
    private Integer codigoFamilia;

    /** FK -> tpr_clasificacionPrecio.idClasificacion (bigint). */
    private Long idClasificacion;

    /**
     * Es dinero. En la base es float(53), que pierde centavos; se expone como
     * BigDecimal para no arrastrar el error de redondeo binario hasta la vista.
     */
    private BigDecimal precio;

    /** bigint NULL. Usuario que hizo el ultimo movimiento. */
    private Long audUsuario;

    /**
     * datetime NULL. El ABM la pisa con GETDATE() en I y en U, asi que lo que
     * se mande desde Java se ignora. La rama L del listado no la devolvia; el
     * script tpr_Precio.sql la agrega al final del SELECT.
     */
    private Date audFecha;
}
