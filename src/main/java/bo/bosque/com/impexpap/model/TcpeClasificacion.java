package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de {@code tcpe_clasificacion} (módulo de exámenes). Mismos nombres que las columnas, con tipos envoltorio
 * para que un NULL de la base siga siendo null (y no un 0 que el SP tomaría como filtro o como dato).
 * Se lee con {@code p_list_tcpe_Clasificacion} y se escribe con {@code p_abm_tcpe_Clasificacion}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeClasificacion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codClasificacion;

    /** bigint. NULL. */
    private Long codArea;

    /** bigint. NULL. */
    private Long codCurso;

    /** varchar(150). NULL. */
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String nombre;

    /** char(1). NULL. */
    @Size(max = 1, message = "Máximo 1 caracteres.")
    @Pattern(regexp = "^[EO]$", message = "La modalidad es E (escrito) u O (online).")
    private String modalidad;

    /** bit. NOT NULL, default 1 si llega nulo. */
    private Boolean activo;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_Clasificacion}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
