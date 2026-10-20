package bo.bosque.com.impexpap.model;

import bo.bosque.com.impexpap.dto.FechaDiaJson;
import bo.bosque.com.impexpap.utils.TcpeHora;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * Fila de {@code tcpe_rolExamen} (módulo de exámenes). Mismos nombres que las columnas, con tipos envoltorio
 * para que un NULL de la base siga siendo null (y no un 0 que el SP tomaría como filtro o como dato).
 * Se lee con {@code p_list_tcpe_RolExamen} y se escribe con {@code p_abm_tcpe_RolExamen}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRolExamen implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codRolExamen;

    /** bigint. NULL. */
    private Long codCapacitacion;

    /** bigint. NULL. */
    private Long codConvocatoria;

    /** bigint. NULL. */
    private Long codArea;

    /** int. NULL. */
    private Integer ponderacion;

    /** int. NULL. */
    private Integer puntos;

    /** int. NULL. */
    private Integer duracionMinutos;

    /** int. NULL. */
    private Integer cantidadPreg;

    /** int. NULL. */
    private Integer cantExamenGenerar;

    /** varchar(150). NULL. */
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String ubicacion;

    /** char(1). NULL. */
    @Size(max = 1, message = "Máximo 1 caracteres.")
    private String modalidad;

    /** bit. NULL. */
    private Boolean esRolCapacitacion;

    /** date. NULL. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate fecha;

    /** time(7). NULL. Viaja como HH:mm:ss. */
    @Pattern(regexp = TcpeHora.PATRON, message = TcpeHora.MENSAJE)
    private String horaInicio;

    /** time(7). NULL. Viaja como HH:mm:ss. */
    @Pattern(regexp = TcpeHora.PATRON, message = TcpeHora.MENSAJE)
    private String horaFin;

    /** bigint. NULL. */
    private Long codClasificacion;

    /** varchar(100). NULL. */
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String titulo;

    /** varchar(200). NULL. */
    @Size(max = 200, message = "Máximo 200 caracteres.")
    private String observaciones;

    /** decimal(5,2). NOT NULL DEFAULT 51. Nota mínima para aprobar ({@code resultado >= notaAprobacion}). */
    private BigDecimal notaAprobacion;

    /** bit. NOT NULL DEFAULT 1. Muestra las respuestas correctas en la revisión al terminar. */
    private Boolean mostrarRespuestas;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_RolExamen}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;

    /** Normaliza el {@code time(7)} que jTDS entrega como texto ({@code 08:30:00.0000000}). */
    public void setHoraInicio(String horaInicio) {
        this.horaInicio = TcpeHora.normalizar(horaInicio);
    }

    /** Normaliza el {@code time(7)} que jTDS entrega como texto ({@code 08:30:00.0000000}). */
    public void setHoraFin(String horaFin) {
        this.horaFin = TcpeHora.normalizar(horaFin);
    }
}
