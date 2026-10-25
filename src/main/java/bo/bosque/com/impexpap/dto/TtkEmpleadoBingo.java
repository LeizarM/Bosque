package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Quien puede recibir cartón del encargado: p_list_ttk_BingoCarton E. Con vale de bingo es un participante
 * (empleado o invitado, codParticipante) con sus vales; sin vale, un empleado activo (codEmpleado).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkEmpleadoBingo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codParticipante;
    private Long codEmpleado;
    private String nombreCompleto;
    private String cargo;
    private String categoriaTexto;
    private Integer valesBingo;
    private Integer cartonesAsignados;
    private Integer puedeRecibir;
}
