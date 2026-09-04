package bo.bosque.com.impexpap.dto;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Una fila de {@code /rol-sabados/refrescar-excusas-horario}: un participante que,
 * por horario biométrico ({@code tbio_*}), ya cubrió su cuota semanal antes del
 * sábado y por eso se excusa (o se excusaría, en modo {@code soloInformar}) de la
 * celda que le tocaba por la rotación A/B.
 *
 * <p>Es el punto donde el módulo Biométrico <b>pisa</b> al Rol de Sábados: la
 * decisión de si alguien tiene que venir un sábado normalmente la toma sólo
 * {@code trs_sp_generarRol} (rotación) o un humano (evento, programación, corrección);
 * ésta es la primera vez que la toma un cálculo sobre datos de OTRO módulo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ExcusaHorarioDto implements Serializable {

    private long codEmpleado;
    private String nombreEmpleado;
    private long idParticipante;
    private long idSabado;
    private Date fecha;

    /** Cuánto sumó de Lunes a Viernes esa semana, según el horario vigente día por día. */
    private double minutosSemana;
    /** La cuota: total de {@code tbio_bioHrs.cantMinutos} del horario BASE (su primera asignación). */
    private double minutosCuota;
    private String motivo;

    /** false en modo {@code soloInformar}: se hubiera excusado, pero no se tocó la celda. */
    private boolean aplicado;
    /** No nulo si {@code aplicado=false} porque el intento de escritura falló (rol cerrado, etc.). */
    private String error;
}
