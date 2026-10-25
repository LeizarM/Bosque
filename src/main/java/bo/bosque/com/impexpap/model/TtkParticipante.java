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
 * Fila de ttk_Participante: columnas de p_list_ttk_Participante L y E, y parámetros de
 * p_abm_ttk_Participante. Un participante es empleado (codEmpleado) o invitado (nombreInvitado).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkParticipante implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codParticipante;
    private Long codEvento;
    private Long codEmpleado;
    private String nombreCompleto;
    private String nombreInvitado;
    private String documento;
    private Long codEmpleadoAnfitrion;
    private String nombreAnfitrion;
    private String cargo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fechaIngreso;
    private String categoria;
    private String categoriaTexto;
    private Integer categoriaManual;
    private Integer totalVales;
    private String resumenVales;
    private String observacion;
    private Integer puedeEditar;

    /** Filtro por nombre de L y E (@buscar); no es columna. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String buscar;

    /** Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date audFecha;
}
