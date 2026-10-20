package bo.bosque.com.impexpap.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Cuerpo de {@code /examenes/lotes/agregar}: más exámenes para un lote existente. Modos y límites los decide
 * {@code p_abm_tcpe_Lote 'V'}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeLoteAgregarDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Es obligatorio.")
    @Positive(message = "Debe ser un código válido.")
    private Long codRolExamen;

    private Integer cantidad;

    @Valid
    private List<@NotNull(message = "Hay una persona vacía en la lista.") TcpePersonaExamenDto> personas;

    private List<@NotNull(message = "Hay un empleado vacío en la lista.") Long> codEmpleados;
}
