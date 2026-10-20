package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Una opción de una pregunta cerrada de la importación.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoImportarOpcionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String opcionRspta;

    /** Si es una respuesta correcta; nulo cuenta como no válida. */
    private Boolean esValido;
}
