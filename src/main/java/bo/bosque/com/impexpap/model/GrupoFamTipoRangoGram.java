package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Mapea la tabla <b>tpr_grupoFamTipoRangoGram</b> del modulo de Precios.
 *
 * <p>Es la tabla puente que dice, para cada grupo de familia SAP y cada tipo de
 * papel (Liviano / Mediano / Pesado), que rango de gramaje le corresponde.
 *
 * <p><b>Contiene EXACTAMENTE las 5 columnas de la tabla.</b> No agregar aqui
 * campos de despliegue que vengan de un JOIN (nombre del grupo, nombre del tipo,
 * texto del rango): {@code SpHelper.ejecutarAbm} serializa este POJO con Jackson
 * y manda todos sus campos como parametros de {@code p_abm_grupoFamTipoRangoGram};
 * un campo que no exista como parametro del SP hace fallar el EXEC. Para eso
 * esta {@link bo.bosque.com.impexpap.dto.GrupoFamTipoRangoGramDto}.
 *
 * <p><b>Trampas de esta tabla:</b>
 * <ul>
 *   <li>Es un HEAP: no tiene PRIMARY KEY ni columna IDENTITY. No existe un
 *       "idGrupoFamTipoRangoGram" que buscar; la fila se identifica por su clave
 *       natural {@code (idGrpFamiliaSap, idTipo)}, verificada unica en la base de
 *       prueba (61 pares distintos sobre 61 filas). Por eso el ABM no devuelve
 *       ningun id generado.</li>
 *   <li>Todas las columnas admiten NULL, por eso todos los campos son wrappers:
 *       {@code BeanPropertyRowMapper} revienta al escribir un NULL en un primitivo.</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GrupoFamTipoRangoGram implements Serializable {

    /**
     * int NULL en esta tabla, pero <b>bigint</b> en la tabla padre
     * tpr_grupoFamiliaSap.idGrpFamiliaSap. El tipo Java sigue a la columna real
     * (Integer), no al padre; quien traiga el id del padre como Long debe
     * estrecharlo antes (el DAO lo hace en un solo lugar). Parte 1 de la clave
     * natural.
     */
    private Integer idGrpFamiliaSap;

    /**
     * int NULL aqui, pero <b>bigint</b> en la tabla padre tpr_tipo.idTipo. Mismo
     * desajuste que idGrpFamiliaSap. Parte 2 de la clave natural.
     */
    private Integer idTipo;

    /**
     * int NULL aqui, pero <b>bigint</b> en la tabla padre tpr_RangoGramaje.idRangoGram.
     * Es el unico dato editable de la fila: la clave natural fija el par
     * (grupo de familia, tipo) y este campo dice que rango de gramaje se le asigna.
     */
    private Integer idRangoGram;

    /** bigint NULL. Usuario que hizo el ultimo movimiento. */
    private Long audUsuario;

    /**
     * datetime NULL. <b>Solo lectura desde la aplicacion:</b> el SP de ABM ignora
     * el valor que se le mande y siempre estampa GETDATE(). Se conserva el campo
     * porque la columna existe y el listado la devuelve.
     */
    private Date audFecha;
}
