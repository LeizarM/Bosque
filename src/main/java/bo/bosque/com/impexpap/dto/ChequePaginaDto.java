package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una pagina de la grilla de cheques. {@code total} es el de todas las filas que cumplen el filtro. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChequePaginaDto implements Serializable {

    private int total;
    private int pagina;
    private int tamanio;
    private List<ChequeFilaDto> filas;
}
