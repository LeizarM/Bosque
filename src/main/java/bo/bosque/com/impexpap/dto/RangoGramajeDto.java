package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Un rango de gramaje <b>ya formateado para mostrar</b>: la forma de resultset
 * que devuelven las ramas {@code @ACCION = 'A'} y {@code @ACCION = 'B'} de
 * {@code p_list_rangoGramaje}.
 *
 * <p>Las dos ramas devuelven exactamente las mismas cuatro columnas y en el
 * mismo orden, por eso alcanza con un solo DTO:
 *
 * <pre>
 *   idRangoGram   min     max     rangoGramaje
 *   3             80.00   120.00  [ 80.00 - 120.00 ]
 * </pre>
 *
 * <p>Lo que cambia entre ellas es el conjunto de filas, no la forma: 'A' trae
 * el catalogo completo (para combos) y 'B' solo los rangos asignados a un grupo
 * de familia SAP y un tipo de papel.
 *
 * <p>Vive aparte del model porque {@code rangoGramaje} no es una columna de
 * tpr_RangoGramaje sino una concatenacion armada en el SELECT; si estuviera en
 * el model, {@code SpHelper.ejecutarAbm} lo mandaria como parametro inexistente
 * a {@code p_abm_rangoGramaje} y el EXEC fallaria.
 *
 * <p>Los nombres de los campos son los alias del SELECT en camelCase.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RangoGramajeDto implements Serializable {

    /** tpr_RangoGramaje.idRangoGram (bigint). Es el valor a guardar cuando se elige el rango. */
    private Long idRangoGram;

    /** Limite inferior en g/m2. decimal(16,2): {@link BigDecimal}, nunca float. */
    private BigDecimal min;

    /** Limite superior en g/m2. decimal(16,2): {@link BigDecimal}, nunca float. */
    private BigDecimal max;

    /**
     * Etiqueta lista para mostrar, armada por el SP como
     * {@code '[ ' + CAST(min AS varchar(15)) + ' - ' + CAST(max AS varchar(15)) + ' ]'}.
     * Al ser decimal(16,2) sale con dos decimales ("[ 80.00 - 120.00 ]").
     * No formatear de nuevo en el cliente: es texto ya resuelto.
     */
    private String rangoGramaje;
}
