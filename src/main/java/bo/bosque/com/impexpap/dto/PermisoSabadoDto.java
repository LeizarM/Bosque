package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Proyección de {@code p_list_trs_PermisoSabado} — un permiso de RR.HH. que cae
 * en un sábado del rol.
 *
 * <p>Sale de cruzar {@code trh_permiso} con {@code trs_Sabado}. Vive en
 * {@code dto/} y no en {@code model/} porque no es una tabla: es el cruce entre
 * dos módulos que no se conocen entre sí.
 *
 * <p><b>{@code letraActual} vs {@code letraQueCorresponde} es lo único que hay
 * que mirar.</b> Si no coinciden, la grilla dice que esa persona viene a
 * trabajar un día que RR.HH. ya le dio libre — y nadie se entera hasta el
 * sábado.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PermisoSabadoDto implements Serializable {

    private long   idSabado;
    private Date   fecha;
    private long   idParticipante;
    private long   codEmpleado;
    private String nombreRol;

    /** vac · baja · clb · libre · otro · pva · pcr · sinsuel · def */
    private String tipoPermiso;

    /** La letra que debería tener la celda: V, B o P. */
    private String letraQueCorresponde;

    /** La que tiene hoy. {@code (libre)} si no hay celda: ese día no le tocaba. */
    private String letraActual;

    /** 1 = la grilla ya lo refleja. Sólo viene en la acción L. */
    private Integer coincide;

    /** G · M · P. Un desfase con origen M lo decidió una persona: no es error. */
    private String origen;

    private String motivo;
}
