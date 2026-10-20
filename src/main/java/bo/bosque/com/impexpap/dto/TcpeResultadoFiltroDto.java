package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Cuerpo de {@code /examenes/resultados/*}: cada acción de {@code p_list_tcpe_Resultado} usa una parte.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeResultadoFiltroDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codRolExamen;

    private Long codExamen;

    @Size(max = 20, message = "Máximo 20 caracteres.")
    private String ci;

    private Long codClasificacion;

    @FechaDiaJson
    private LocalDate fechaDesde;

    @FechaDiaJson
    private LocalDate fechaHasta;
}
