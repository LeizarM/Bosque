package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Banco 'D'}: una pregunta de la categoría igual al texto escrito ({@code igual}) o
 * parecida (lo contiene).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoDuplicadoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPregunta;

    private String pregunta;

    private Boolean activo;

    /** true = mismo texto normalizado; false = solo parecida. */
    private Boolean igual;
}
