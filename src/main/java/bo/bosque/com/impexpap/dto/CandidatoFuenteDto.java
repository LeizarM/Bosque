// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\CandidatoFuenteDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Una consulta del cierre (fn_tac_cierre_... o p_tac_cierre_...) que existe
 * en la base y todavía no se registró como fuente
 * ({@code p_list_tac_ResponsableCierre 'O'}, archivo SQL 87).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CandidatoFuenteDto {
    private String fuente;
    private String tipo;
    /** 1 si devuelve las columnas mínimas (clave, titulo). */
    private Integer columnasOk;
}
