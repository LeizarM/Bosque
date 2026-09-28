package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Una fila de la tabla de <b>parametros de gramaje</b>: la vista pivoteada que
 * devuelve {@code p_list_grupoFamTipoRangoGram} en su rama {@code @ACCION = 'A'}.
 *
 * <p>Es la unica forma de resultset del modulo que no coincide con las columnas
 * de tpr_grupoFamTipoRangoGram, por eso vive en un DTO y no en el model: cada
 * fila es un grupo de familia SAP con sus tres rangos de gramaje ya resueltos a
 * texto, una columna por tipo de papel.
 *
 * <pre>
 *   GrupoFamilia   Liviano      Mediano      Pesado
 *   BOND           [40.00-70.00] [71.00-120.00] [121.00-250.00]
 * </pre>
 *
 * <p>Los nombres de los campos son los alias del SELECT en camelCase.
 * {@code BeanPropertyRowMapper} baja el alias a minusculas, asi que los alias
 * con mayuscula inicial del SP ({@code GrupoFamilia}, {@code Liviano}...) mapean
 * sin problema contra {@code grupoFamilia}, {@code liviano}...
 *
 * <p>Los cuatro ultimos campos (los ids) se agregaron al final del SELECT para
 * que la pantalla pueda editar la fila: la vista pivoteada sola no alcanza para
 * saber a que registro de la tabla puente hay que aplicarle el UPDATE. Un tipo
 * sin configurar llega en NULL, tanto en el texto como en su id.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GrupoFamTipoRangoGramDto implements Serializable {

    /** tpr_grupoFamiliaSap.grpFam: nombre del grupo de familia SAP. */
    private String grupoFamilia;

    /** Rango de gramaje del tipo "Liviano", ya formateado como "[min-max]". NULL si no esta configurado. */
    private String liviano;

    /** Rango de gramaje del tipo "Mediano", ya formateado como "[min-max]". NULL si no esta configurado. */
    private String mediano;

    /** Rango de gramaje del tipo "Pesado", ya formateado como "[min-max]". NULL si no esta configurado. */
    private String pesado;

    /**
     * Id del grupo de familia. Viene de tpr_grupoFamiliaSap, donde la columna es
     * <b>bigint</b>, por eso es Long y no Integer como el campo homonimo del model
     * (en la tabla puente esa misma columna es int).
     */
    private Long idGrpFamiliaSap;

    /** tpr_grupoFamTipoRangoGram.idRangoGram (int) de la fila del tipo "Liviano". NULL si no hay fila. */
    private Integer idRangoGramLiviano;

    /** tpr_grupoFamTipoRangoGram.idRangoGram (int) de la fila del tipo "Mediano". NULL si no hay fila. */
    private Integer idRangoGramMediano;

    /** tpr_grupoFamTipoRangoGram.idRangoGram (int) de la fila del tipo "Pesado". NULL si no hay fila. */
    private Integer idRangoGramPesado;
}
