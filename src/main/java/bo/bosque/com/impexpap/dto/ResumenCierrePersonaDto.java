// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\ResumenCierrePersonaDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * El cumplimiento de una persona en Verificar Cierre en un rango de días
 * ({@code p_list_tac_FirmaCierre 'P'}, archivo SQL 81). Una celda compartida
 * cuenta para todos sus responsables: quien la firmó la suma como hecha; a los
 * demás les queda en {@code porOtro}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ResumenCierrePersonaDto {
    private Long codEmpleado;
    private String persona;
    private Integer leTocaban;
    private Integer hechasATiempo;
    private Integer hechasTarde;
    private Integer porOtro;
    private Integer noHechas;
    private Integer pendientes;
    private Integer autorrevisadas;
    private Integer hallazgos;
}
