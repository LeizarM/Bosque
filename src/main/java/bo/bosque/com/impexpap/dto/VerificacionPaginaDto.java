package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una pagina de una lista de Verificar Cheques. {@code total} es el de todas las filas que cumplen el filtro. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionPaginaDto<T> implements Serializable {

    private int total;
    private int pagina;
    private int tamanio;
    private List<T> filas;
}
