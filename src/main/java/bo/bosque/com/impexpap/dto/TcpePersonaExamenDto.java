package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Una persona del modo {@code personas} de generar o agregar: externo identificado por CI. Si ya tiene usuario
 * externo, {@code p_abm_tcpe_GenerarExamen} lo reutiliza; si no, lo crea con estos datos.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TcpePersonaExamenDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "El CI es obligatorio.")
    @Size(max = 20, message = "El CI admite hasta 20 caracteres.")
    private String ci;

    @NotBlank(message = "Los nombres son obligatorios.")
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String nombres;

    // Basta un apellido, paterno o materno: lo exige p_abm_tcpe_GenerarExamen.
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String apellidoPaterno;

    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String apellidoMaterno;
}
