package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de {@code tcpe_curso} (módulo de exámenes). Mismos nombres que las columnas, con tipos envoltorio
 * para que un NULL de la base siga siendo null (y no un 0 que el SP tomaría como filtro o como dato).
 * Se lee con {@code p_list_tcpe_Curso} y se escribe con {@code p_abm_tcpe_Curso}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeCurso implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codCurso;

    /** bigint. NULL. */
    private Long codCapacitacion;

    /** float. NULL. */
    private Double inversion;

    /** varchar(100). NULL. */
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String titulo;

    /** varchar(200). NULL. */
    @Size(max = 200, message = "Máximo 200 caracteres.")
    private String descripcion;

    /** varchar(150). NULL. */
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String lugar;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_Curso}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
