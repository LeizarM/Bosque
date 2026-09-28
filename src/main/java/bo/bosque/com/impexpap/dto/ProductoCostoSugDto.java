package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Forma de las ramas <b>N y O</b> de {@code p_list_producto}, ambas sobre
 * tpr_costoSug.
 * <ul>
 *   <li>N: historial de costos sugeridos de una familia, solo de propuestas
 *       aprobadas, desde {@code fechaI} hasta hoy. Devuelve las tres columnas.</li>
 *   <li>O: el costo sugerido de una familia en una propuesta puntual. Devuelve
 *       solo {@code costoSug}; los otros dos campos quedan en null.</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCostoSugDto {

    /** Solo lo devuelve la rama N. */
    private Integer codigoFamilia;

    /** float(53) en la base; BigDecimal para no perder centavos. */
    private BigDecimal costoSug;

    /** Solo lo devuelve la rama N. */
    private Date fechaI;
}
