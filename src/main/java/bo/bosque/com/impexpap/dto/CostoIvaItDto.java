package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Forma del resultset de la accion <b>'V' (vigente)</b> de
 * {@code p_list_costoIvaIt}: la fila de IVA/IT que rige hoy, mas la suma
 * {@code totalIvaIt} ya calculada por el SP.
 *
 * <p>Existe como DTO y no como modelo porque {@code totalIvaIt} no es una
 * columna de tpr_costoIvaIt: es {@code ISNULL(iva,0) + ISNULL(it,0)}. Es la
 * misma cuenta que el legacy hacia inline dentro de
 * {@code p_list_precioPropuesta} rama 'A'
 * ({@code (select top 1 iva+it from tpr_costoIvaIt)}), pero ahora en un solo
 * lugar y con orden determinista.
 *
 * <p>Los nombres de los campos son identicos a los alias del SELECT: asi los
 * mapea BeanPropertyRowMapper.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CostoIvaItDto {

    /** PK de la fila vigente (la de mayor idCII). */
    private Integer idCII;

    /** FK logica a tpr_propuesta; puede venir null. */
    private Long idPropuesta;

    /** Porcentaje de IVA (float(53) en la BD, BigDecimal aca para no perder decimales). */
    private BigDecimal iva;

    /** Porcentaje de IT. */
    private BigDecimal it;

    /**
     * {@code ISNULL(iva,0) + ISNULL(it,0)} calculado por el SP. Es el valor que
     * la formula de precios suma al costo sugerido.
     */
    private BigDecimal totalIvaIt;

    /** Usuario de auditoria de la fila vigente. */
    private Long audUsuario;

    /** Fecha de auditoria de la fila vigente. */
    private Date audFecha;
}
