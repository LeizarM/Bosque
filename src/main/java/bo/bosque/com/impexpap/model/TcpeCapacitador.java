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
 * Fila de {@code tcpe_capacitador} (módulo de exámenes). Mismos nombres que las columnas, con tipos envoltorio
 * para que un NULL de la base siga siendo null (y no un 0 que el SP tomaría como filtro o como dato).
 * Se lee con {@code p_list_tcpe_Capacitador} y se escribe con {@code p_abm_tcpe_Capacitador}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeCapacitador implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codCapacitador;

    /** int. NULL. */
    private Integer sexo;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String telefonos;

    /** varchar(20). NULL. */
    @Size(max = 20, message = "Máximo 20 caracteres.")
    private String ci;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String nombre;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String apPaterno;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String apMaterno;

    /** varchar(150). NULL. */
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String direccion;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String email;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String pais;

    /** date. NULL. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate fechaNacimiento;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_Capacitador}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
