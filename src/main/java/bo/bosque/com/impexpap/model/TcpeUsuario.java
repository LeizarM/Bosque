package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de {@code tcpe_usuario} (módulo de exámenes). Mismos nombres que las columnas, con tipos envoltorio
 * para que un NULL de la base siga siendo null (y no un 0 que el SP tomaría como filtro o como dato).
 * Se lee con {@code p_list_tcpe_Usuario} y se escribe con {@code p_abm_tcpe_Usuario}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeUsuario implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codUsuario;

    /** bigint. NULL. */
    private Long codEmpleado;

    /** bigint. NULL. */
    private Long codTipoUsuario;

    /** varchar(20). NULL. */
    @Size(max = 20, message = "Máximo 20 caracteres.")
    private String ci;

    /** bigint. NULL. */
    private Long codArea;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String nombres;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String apellidoPaterno;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String apellidoMaterno;

    /**
     * varchar(100). Hash BCrypt. <b>Nunca</b> viaja en JSON (ni de ida ni de vuelta) ni en el
     * {@code toString}: {@code p_list_tcpe_Usuario} no la devuelve y {@code p_abm_tcpe_Usuario} no la recibe.
     * Las claves se manejan solo por {@code p_abm_tcpe_Acceso}.
     */
    @JsonIgnore
    @ToString.Exclude
    private String password2;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String login;

    /** bit. NOT NULL, default 1 si llega nulo. */
    private Boolean activo;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_Usuario}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
