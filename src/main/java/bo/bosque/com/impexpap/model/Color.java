package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Mapea la tabla <b>tpr_color</b> (catalogo de colores del modulo de precios).
 *
 * <p>Tiene EXACTAMENTE las cinco columnas de la tabla y ninguna mas: SpHelper
 * serializa el POJO con Jackson y manda cada campo como parametro del SP, asi
 * que un campo de display que no exista como &#64;parametro en p_abm_color
 * haria fallar el EXEC.
 *
 * <p>Ojo con el nombre: esta clase NO es java.awt.Color. Los usos de
 * {@code Color.WHITE} en commons/service son del AWT y no tienen relacion.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Color implements Serializable {

    /** tpr_color.idColor - bigint NOT NULL IDENTITY, PK. Wrapper por consistencia. */
    private Long idColor;

    /** tpr_color.color - varchar(100). Ojo: el SP declaraba VARCHAR(50) y truncaba. */
    private String color;

    /**
     * tpr_color.estado - int NULL. 1 = activo, 0 = inactivo.
     * Se deja Integer y NO Boolean: es un int en la BD y el legacy lo usa como
     * numero. En el alta el SP fuerza 1 e ignora lo que se le mande.
     */
    private Integer estado;

    /** tpr_color.audUsuario - bigint NULL. */
    private Long audUsuario;

    /**
     * tpr_color.audFecha - datetime NULL. Esta en el model porque es columna de
     * la tabla y el SP declara el parametro, pero p_abm_color lo ignora y sella
     * GETDATE() tanto en el alta como en la modificacion.
     */
    private Date audFecha;
}
