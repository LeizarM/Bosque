package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Cuerpo de {@code /examenes/banco/importar} y {@code /importar/validar}. Las reglas las aplica el SP; aquí solo va
 * el tope técnico del envío (el de negocio es {@code maxPreguntasPorImportacion} de la configuración).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoImportarDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int MAX_PREGUNTAS = 500;

    private Long codClasificacion;

    @Size(max = MAX_PREGUNTAS, message = "Se admiten hasta " + MAX_PREGUNTAS + " preguntas por envío.")
    private List<TcpeBancoImportarPreguntaDto> preguntas;
}
