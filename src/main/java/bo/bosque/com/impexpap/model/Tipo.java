package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Mapea 1 a 1 la tabla <b>tpr_tipo</b> (catalogo de tipos de papel / tipos de
 * familia del modulo de precios).
 *
 * <p>Tiene EXACTAMENTE las cinco columnas de la tabla y ninguna mas:
 * {@code SpHelper.ejecutarAbm} serializa el POJO completo con Jackson y manda
 * cada campo como un {@code @parametro} de {@code p_abm_tipo}. Un campo de
 * display que no exista como parametro del procedimiento hace fallar el EXEC
 * entero, asi que cualquier dato que venga de un JOIN va en un DTO aparte.
 *
 * <p>Todos los tipos son wrappers a proposito: salvo {@code idTipo}, todas las
 * columnas admiten NULL en la base y {@code BeanPropertyRowMapper} revienta al
 * poner un NULL sobre un primitivo. La version anterior de esta clase usaba
 * {@code int} en idTipo, estado y audUsuario, y le faltaba audFecha.
 *
 * <p><b>No confundir</b> con {@code bo.bosque.com.impexpap.utils.Tipos}, que es
 * otra cosa: una clase de constantes de los grupos de la vista v_tipos.
 *
 * <p>Procedimientos asociados: ABM {@code p_abm_tipo} (acciones I, U, D) y
 * listado {@code p_list_tipo} (acciones L y A).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Tipo implements Serializable {

    /** tpr_tipo.idTipo - bigint NOT NULL IDENTITY, PK. Wrapper por consistencia: en un alta todavia no existe. */
    private Long idTipo;

    /**
     * tpr_tipo.tipo - varchar(150) NULL. Nombre del tipo.
     * Ojo: el procedimiento declaraba el parametro como VARCHAR(50) y truncaba
     * en silencio; el script tpr_Tipo.sql lo lleva a VARCHAR(150).
     */
    private String tipo;

    /**
     * tpr_tipo.estado - int NULL. 1 = activo, 0 = inactivo.
     * Se deja Integer y NO Boolean: en la base es int y el legacy lo trata como
     * numero. En el alta el SP fuerza 1 e ignora lo que se le mande.
     */
    private Integer estado;

    /** tpr_tipo.audUsuario - bigint NULL. Usuario que hizo el ultimo movimiento. */
    private Long audUsuario;

    /**
     * tpr_tipo.audFecha - datetime NULL. Esta en el model porque es columna de
     * la tabla y el SP declara el parametro, pero {@code p_abm_tipo} lo ignora
     * y sella GETDATE() tanto en el alta como en la modificacion.
     */
    private Date audFecha;
}
