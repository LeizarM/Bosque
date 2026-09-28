package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Mapea 1:1 la tabla <b>tpr_autorizacion</b> (modulo de precios).
 *
 * <p>Estructura real de la tabla (ver diccionario de datos):
 * <pre>
 *   idAutorizacion bigint NOT NULL IDENTITY PK
 *   idPropuesta    bigint NULL  FK -&gt; tpr_propuesta
 *   esAprobada     tinyint NULL (dominio = v_tipos grupo 38)
 *   audUsuario     bigint NULL
 *   audFecha       datetime NULL
 * </pre>
 *
 * <p><b>Por que no hay campos de display aqui:</b> {@code SpHelper.ejecutarAbm}
 * serializa el POJO con Jackson y manda TODOS sus campos como parametros de
 * {@code p_abm_autorizacion}. Cualquier campo que no exista como {@code @parametro}
 * del procedimiento hace fallar el EXEC. Los datos que vienen de JOIN
 * (titulo de la propuesta, nombres de las personas, descripcion del estado)
 * viven en {@link bo.bosque.com.impexpap.dto.AutorizacionDto}.
 *
 * <p>Todos los tipos son wrappers a proposito: las columnas son NULL-ables y
 * {@code BeanPropertyRowMapper} revienta al escribir un NULL sobre un primitivo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Autorizacion implements Serializable {

    /** bigint IDENTITY. Lo devuelve el ABM en @idGenerado al insertar. */
    private Long idAutorizacion;

    /** bigint NULL. FK a tpr_propuesta. */
    private Long idPropuesta;

    /**
     * tinyint NULL. <b>Trampa:</b> NO es un booleano aunque el nombre lo sugiera.
     * Maneja cuatro estados (dominio v_tipos grupo 38):
     * <ul>
     *   <li>0 = Pendiente</li>
     *   <li>1 = Aprobada</li>
     *   <li>2 = No Aprobada</li>
     *   <li>3 = En Espera</li>
     * </ul>
     * Por eso es {@code Integer} y nunca {@code Boolean}.
     */
    private Integer esAprobada;

    /** bigint NULL. Usuario que aprobo/rechazo. La ACCION 'A' lo deja en NULL cuando esAprobada es 0 o 3. */
    private Long audUsuario;

    /** datetime NULL. Fecha de la aprobacion/rechazo. La ACCION 'A' la deja en NULL cuando esAprobada es 0 o 3. */
    private Date audFecha;

}
