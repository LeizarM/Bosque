package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Forma del resultset de <b>p_list_autorizacion, ACCION 'A'</b> (vista principal
 * de propuestas por autorizar, TOP 100 ordenado por idPropuesta DESC).
 *
 * <p>No es un model: mezcla columnas de tpr_autorizacion con columnas de
 * tpr_propuesta, con subconsultas escalares a tb_usuario/tb_empleado/trh_persona
 * y con la descripcion del estado que aporta la vista v_tipos (grupo 38).
 *
 * <p>Los nombres de los campos son exactamente los alias del SELECT en camelCase,
 * porque {@code SpHelper.ejecutarListado} mapea con {@code BeanPropertyRowMapper}
 * (por nombre de columna, no por posicion).
 *
 * <p><b>Requiere el ALTER de {@code src/main/resources/sql/tpr_Autorizacion.sql}:</b>
 * el proc original devolvia la columna {@code audFecha} DOS veces (indice 6 la de la
 * propuesta, indice 9 la de la autorizacion). BeanPropertyRowMapper escribe la misma
 * propiedad dos veces y gana la ultima, asi que la fecha de la propuesta se perdia en
 * silencio. El ALTER las renombra a {@code audFechaPropuesta} y
 * {@code audFechaAutorizacion}; el orden y la cantidad de columnas no cambian, por lo
 * que el JSF legacy (que lee por indice) sigue funcionando igual.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AutorizacionDto implements Serializable {

    /** tpr_autorizacion.idAutorizacion */
    private Long idAutorizacion;

    /** tpr_propuesta.idPropuesta */
    private Long idPropuesta;

    /** tpr_propuesta.tipo (tinyint). 2 = propuesta por articulo. */
    private Integer tipo;

    /** tpr_propuesta.titulo */
    private String titulo;

    /** Nombre completo de quien creo la propuesta (tpr_propuesta.audUsuario). */
    private String datoPersonaP;

    /** tpr_propuesta.audFecha. Antes del ALTER llegaba como 'audFecha' y se perdia. */
    private Date audFechaPropuesta;

    /** Descripcion del estado (v_tipos grupo 38): Pendiente / Aprobada / No Aprobada / En Espera. */
    private String estado;

    /** Nombre completo de quien autorizo (tpr_autorizacion.audUsuario). */
    private String datoPersonaA;

    /** tpr_autorizacion.audFecha. Antes del ALTER se llamaba 'audFecha' y pisaba a la de la propuesta. */
    private Date audFechaAutorizacion;

    /** Nombre completo de quien genero/exporto la propuesta (tpr_propuesta.audUsGenerado). */
    private String datoPersonaG;

    /** tpr_propuesta.audFecGenerado */
    private Date audFecGenerado;

}
