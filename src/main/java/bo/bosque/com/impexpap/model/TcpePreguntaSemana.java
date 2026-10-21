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
 * Fila de {@code tcpe_preguntaSemana} (v12/v13): una pregunta para el inicio, abierta ("¿Qué tal tu día?") o de
 * escala ("Del 1 al 5, ¿cómo ves el ambiente laboral?"), con su plazo. Tipos envoltorio para que un NULL siga siendo
 * null. Se lee con {@code p_list_tcpe_PreguntaSemana} y se escribe con {@code p_abm_tcpe_PreguntaSemana}.
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

    /** char(1). NOT NULL: A abierta (se responde con un texto) o E escala (se elige un número). Nulo: el SP decide. */
    private String tipo;

    /** tinyint. Solo escala: el primer número. Nulo en una escala nueva: el SP propone el de la configuración. */
    private Integer escalaMin;

    /** tinyint. Solo escala: el último número, mayor que {@code escalaMin} y hasta el tope de la configuración. */
    private Integer escalaMax;

    /** nvarchar(40). Solo escala, opcional: qué significa {@code escalaMin} (por ejemplo, "Muy mal"). */
    @Size(max = 40, message = "Máximo 40 caracteres.")
    private String etiquetaMin;

    /** nvarchar(40). Solo escala, opcional: qué significa {@code escalaMax} (por ejemplo, "Excelente"). */
    @Size(max = 40, message = "Máximo 40 caracteres.")
    private String etiquetaMax;

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
