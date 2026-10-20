package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Filtros de {@code p_list_tcpe_Banco} ({@code L} preguntas y {@code O} opciones). Nulo = sin filtro.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoFiltroDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPregunta;

    private Long codClasificacion;

    private Long codArea;

    private Boolean activo;

    /** Busca con LIKE sobre el texto de la pregunta. */
    @Size(max = 200, message = "Máximo 200 caracteres.")
    private String texto;
}
