package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de ttk_BingoCarton: columnas de p_list_ttk_BingoCarton L / D / M y parámetros de p_abm_.
 * D (participante) solo llena codCarton, codBingo y nroCarton: el dueño nunca viaja ahí.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkBingoCarton implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codCarton;
    private Long codBingo;
    private Integer nroCarton;
    private Long codEmpleado;
    /** Dueño por vale de bingo (empleado o invitado); nombreEmpleado es el nombre de ese dueño. */
    private Long codParticipante;
    private String nombreEmpleado;
    private Integer esInvitado;
    private String origen;
    private String origenTexto;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fechaAsignacion;
    /** 1 = se entrega impreso. Con modalidad «ambos» el encargado lo indica al asignar (A) o al cambiar la forma (F). */
    private Integer impreso;
    private String formaTexto;
    private Integer esGanador;
    private Integer puedeAsignar;
    private Integer puedeCambiarForma;
    private Integer puedeLiberar;

    /** Filtros de L (@soloDisponibles 1/0 y @buscar por número o nombre); no son columnas. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer soloDisponibles;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String buscar;

    /** Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date audFecha;
}
