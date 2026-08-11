package bo.bosque.com.impexpap.dto;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Proyección de {@code p_list_trs_Rol @ACCION='I'} — todo lo que un humano tocó
 * sobre el horario por defecto.
 *
 * <p>No es un rol: es UNA fila por intervención, y sale de la UNION de cuatro
 * fuentes ({@code trs_Programacion}, {@code trs_Cambio}, {@code trs_Convocatoria}
 * y las celdas con {@code origen='M'}). Por eso vive en {@code dto/} y no en
 * {@code model/}.
 *
 * <p>Es lo que reemplazó a la conformidad: el rol se acepta por defecto, así que
 * lo único que hay que mirar es lo que se salió de la regla. <b>Pocas filas = el
 * default está bien calibrado; muchas = alguna regla no representa cómo se
 * trabaja de verdad</b>, y conviene arreglar la regla en vez de seguir
 * corrigiendo a mano.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class IntervencionRolDto implements Serializable {

    private long   idRol;
    private long   idSabado;
    private Date   fechaSabado;
    /** PROGRAMACION · CAMBIO · CONVOCATORIA:CONVOCADO · CONVOCATORIA:EXCUSADO · CORRECCION MANUAL */
    private String tipoIntervencion;
    private long   codEmpleado;
    private String nombreRol;
    /** La letra con la que quedó la celda de esa persona ese sábado. */
    private String quedoEn;
    /**
     * Quién tomó la decisión: el jefe, el aprobador o el usuario de RR.HH.
     * Wrapper porque las tres columnas de origen —{@code codAprobador},
     * {@code codEmpleadoAutoriza} y {@code audUsuario}— admiten NULL.
     */
    private Long    loHizo;
    private Date    cuando;
    /** Wrapper: en la rama CAMBIO sale de un DATEDIFF sobre una fecha que puede ser NULL. */
    private Integer diasAntelacion;
    private String  motivo;
}
