package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Costo sugerido por familia dentro de una propuesta de precios.
 *
 * <p>Mapea 1 a 1 la tabla <b>tpr_costoSug</b> (modulo de precios). Contiene
 * EXACTAMENTE las 7 columnas de la tabla y ninguna mas: {@code SpHelper.ejecutarAbm}
 * serializa el POJO completo con Jackson y manda cada campo como parametro del
 * procedimiento, asi que un campo de adorno (por ejemplo el nombre de la familia
 * que llega por JOIN) haria fallar el EXEC de {@code p_abm_costoSug}. Los campos
 * de display van en un DTO aparte.
 *
 * <p>Procedimientos asociados:
 * <ul>
 *   <li>ABM: {@code p_abm_costoSug} (acciones I, U, D)</li>
 *   <li>Listado: {@code p_list_costSug} (accion L) — el nombre va sin la "o"
 *       de "costo" a proposito; asi lo invoca el JSF legacy y no se renombra.</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CostoSug implements Serializable {

    private static final long serialVersionUID = 1L;

    /** PK. bigint IDENTITY. Wrapper: viene nulo cuando todavia no se inserto. */
    private Long idCosSug;

    /** FK logica a tpr_propuesta.idPropuesta. bigint NULL. */
    private Long idPropuesta;

    /**
     * FK a tpr_producto.codigoFamilia. Es <b>int</b> en esta tabla (y en casi todo
     * el modulo), pero ojo: en tpr_precioPropuesta la misma columna es varchar(15).
     */
    private Integer codigoFamilia;

    /**
     * Costo sugerido de la familia. En la base es <b>float(53)</b>, igual que todo
     * el dinero de este modulo. Se expone como BigDecimal porque el float binario
     * no representa 0.01 y se pierden centavos al redondear; el redondeo a 2
     * decimales se hace en la capa de servicio/vista, no aca.
     */
    private BigDecimal costoSug;

    /**
     * Fecha de inicio de vigencia del costo. datetime NULL.
     * <p>Trampa: en la accion I y en la U el procedimiento pisa esta columna con
     * {@code GETDATE()} — lo que se mande aqui se ignora en el insert.</p>
     */
    private Date fechaI;

    /** Usuario de auditoria. bigint NULL. */
    private Long audUsuario;

    /**
     * Fecha de auditoria. datetime NULL.
     * <p>El procedimiento siempre la setea con {@code GETDATE()}; existe como campo
     * porque {@code p_abm_costoSug} declara el parametro {@code @audFecha} y el
     * POJO tiene que poder mapearlo.</p>
     */
    private Date audFecha;

}
