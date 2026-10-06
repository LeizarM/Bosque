package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lo que quedo guardado al subir el PDF de un cheque. {@code reemplazo} = ya habia uno y se cambio por el nuevo. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChequePdfSubidoDto implements Serializable {

    private String nombreArchivo;
    private long tamanoBytes;
    private boolean reemplazo;
}
