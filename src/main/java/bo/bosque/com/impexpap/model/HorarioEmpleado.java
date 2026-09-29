package bo.bosque.com.impexpap.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class HorarioEmpleado {
    private Long codEmpleado;
    private Long codPersona;
    private String nombreCompleto;
    private String cargo;
    private Integer posicionCargo;
    private String nombreHorario;
    private String horaIngreso;
    private String horaSalida;
    private Integer cantMinutos;
}
