package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Una pregunta de la importación. Nada se valida aquí: lo vacío o incoherente lo marca el SP en su fila.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoImportarPreguntaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String pregunta;

    /** true = cerrada, false = abierta; nulo = el SP lo deduce de las opciones. */
    private Boolean esCerrada;

    /** Respuesta modelo de una abierta. */
    private String respuesta;

    private Integer prioridad;

    private List<TcpeBancoImportarOpcionDto> opciones;
}
