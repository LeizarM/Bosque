package bo.bosque.com.impexpap.model;

import bo.bosque.com.impexpap.dto.FechaDiaJson;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * Fila de {@code tcpe_preguntaSemana} (v12): una pregunta libre ("¿Qué tal tu día?") con su plazo para el inicio.
 * Tipos envoltorio para que un NULL de la base siga siendo null. Se lee con {@code p_list_tcpe_PreguntaSemana} y se
 * escribe con {@code p_abm_tcpe_PreguntaSemana}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpePreguntaSemana implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codPreguntaSemana;

    /** nvarchar(100). NULL: sin título se muestra el de la configuración. */
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String titulo;

    /** nvarchar(500). NOT NULL: lo que se pregunta. */
    @Size(max = 500, message = "Máximo 500 caracteres.")
    private String pregunta;

    /** date. NOT NULL. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate fechaInicio;

    /** date. NOT NULL, no antes de {@code fechaInicio}. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate fechaFin;

    /** bit. NOT NULL, default 1 si llega nulo. */
    private Boolean activo;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_PreguntaSemana}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
