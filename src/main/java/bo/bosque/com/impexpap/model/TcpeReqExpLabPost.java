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
 * Fila de {@code tcpe_reqExpLabPost} (módulo de exámenes). Mismos nombres que las columnas, con tipos envoltorio
 * para que un NULL de la base siga siendo null (y no un 0 que el SP tomaría como filtro o como dato).
 * Se lee con {@code p_list_tcpe_ReqExpLabPost} y se escribe con {@code p_abm_tcpe_ReqExpLabPost}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeReqExpLabPost implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codExpLabPost;

    /** bigint. NULL. */
    private Long codPersonaPost;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String nombreEmpresa;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String cargo;

    /** varchar(200). NULL. */
    @Size(max = 200, message = "Máximo 200 caracteres.")
    private String descripcion;

    /** date. NULL. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate fechaFin;

    /** date. NULL. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate fechaInicio;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_ReqExpLabPost}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
