package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Agrupacion de TIPOS de talonario (no de talonarios). Tabla tmto_talonarioGrupo.
 * Sirve para filtrar reportes y listados.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TalonarioGrupo implements Serializable {

    private long codGrupo;
    private String nombre;
    private String detalle;
    private long audUsuario;
    private Date audFecha;

    // ---- solo lectura, los llena p_list_tmto_TalonarioGrupo ----
    /** Tipos asignados. Si es > 0 el grupo no se puede eliminar. */
    private int cantTipos;
    private int cantTalonarios;

}
