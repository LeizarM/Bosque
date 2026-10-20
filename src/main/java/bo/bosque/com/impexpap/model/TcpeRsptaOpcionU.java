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
 * Fila de {@code tcpe_rsptaOpcionU} (módulo de exámenes). Mismos nombres que las columnas, con tipos envoltorio
 * para que un NULL de la base siga siendo null (y no un 0 que el SP tomaría como filtro o como dato).
 * Se lee con {@code p_list_tcpe_RsptaOpcionU} y se escribe con {@code p_abm_tcpe_RsptaOpcionU}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRsptaOpcionU implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codRsptaOpU;

    /** bigint. NULL. */
    private Long codRsptaOpH;

    /** bigint. NULL. */
    private Long codExamen;

    /** bigint. NULL. */
    private Long codRespuesta;

    /** bigint. NULL. */
    private Long codRsptaOp;

    /** varchar(300). NULL. */
    @Size(max = 300, message = "Máximo 300 caracteres.")
    private String opcionRspta;

    /** bit. NOT NULL, default 0 si llega nulo. */
    private Boolean esValido;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_RsptaOpcionU}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
