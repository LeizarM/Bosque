package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Si el cheque ya tiene su documento PDF en la carpeta del servidor (el legacy lo comprobaba mirando el archivo: no hay nada en
 * la base de datos).
 *
 * <ul>
 *   <li>{@code nombreArchivo}: siempre {@code "<codCheque>.pdf"}, exista o no el archivo.</li>
 *   <li>{@code tamanoBytes} y {@code fechaModificacion}: {@code null} si no existe. La fecha es la hora local del servidor,
 *       {@code yyyy-MM-dd'T'HH:mm:ss} y sin zona.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChequePdfEstadoDto implements Serializable {

    private boolean existe;
    private String nombreArchivo;
    private Long tamanoBytes;
    private String fechaModificacion;
}
