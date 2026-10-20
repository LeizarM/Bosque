package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de {@code tcpe_seleccionPreguntas} (módulo de exámenes). Mismos nombres que las columnas, con tipos envoltorio
 * para que un NULL de la base siga siendo null (y no un 0 que el SP tomaría como filtro o como dato).
 * Se lee con {@code p_list_tcpe_SeleccionPreguntas} y se escribe con {@code p_abm_tcpe_SeleccionPreguntas}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeSeleccionPreguntas implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codSeleccionPreguntas;

    /** bigint. NULL. */
    private Long codExamen;

    /** bigint. NULL. */
    private Long codPregunta;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_SeleccionPreguntas}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
