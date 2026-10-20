package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.utils.TcpeHora;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpo de {@code /examenes/lotes/generar} y {@code /actualizar}. Solo valida tipos y nulos: modos, límites,
 * rangos y obligatorios de negocio los decide {@code p_abm_tcpe_Lote} ({@code V}, {@code I}, {@code U}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeLoteGuardarDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Nulo al generar; al actualizar, el lote. */
    private Long codRolExamen;

    /** Si no llega, el SP la toma de la categoría. */
    private Long codArea;

    private Long codClasificacion;

    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String titulo;

    @Size(max = 200, message = "Máximo 200 caracteres.")
    private String observaciones;

    /** E (escrito) u O (en línea). */
    private String modalidad;

    private Integer duracionMinutos;

    private Integer cantidadPreg;

    @FechaDiaJson
    private LocalDate fecha;

    @Pattern(regexp = TcpeHora.PATRON, message = TcpeHora.MENSAJE)
    private String horaInicio;

    @Pattern(regexp = TcpeHora.PATRON, message = TcpeHora.MENSAJE)
    private String horaFin;

    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String ubicacion;

    /** Nota mínima para aprobar. Nula: al generar, la de {@code f_tcpe_config}; al actualizar, no se cambia. */
    private BigDecimal notaAprobacion;

    /** Mostrar las respuestas correctas al terminar. Nulo: al generar, true; al actualizar, no se cambia. */
    private Boolean mostrarRespuestas;

    /** Modo cantidad: exámenes anónimos (en línea, con clave al azar; escritos, sin usuario). */
    private Integer cantidad;

    /** Modo personas: externos identificados por CI; se reutiliza su usuario si ya tiene uno. */
    @Valid
    private List<@NotNull(message = "Hay una persona vacía en la lista.") TcpePersonaExamenDto> personas;

    /** Modo empleados: cada uno rinde con su sesión del sistema. */
    private List<@NotNull(message = "Hay un empleado vacío en la lista.") Long> codEmpleados;

    /** jTDS entrega el {@code time(7)} como texto con fracción de segundo. */
    public void setHoraInicio(String horaInicio) {
        this.horaInicio = TcpeHora.normalizar(horaInicio);
    }

    public void setHoraFin(String horaFin) {
        this.horaFin = TcpeHora.normalizar(horaFin);
    }
}
