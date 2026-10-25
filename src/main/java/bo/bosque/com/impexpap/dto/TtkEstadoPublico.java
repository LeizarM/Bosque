package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.TtkBingoSorteo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Lo que ven los participantes en vivo: estado del bingo y tablero, sin ranking, nombres ni avisos.
 * Inmutable: la misma instancia se comparte desde la caché con todos los espectadores.
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TtkEstadoPublico implements Serializable {

    private static final long serialVersionUID = 1L;

    private long version;
    private TtkBingoPublico publico;
    private List<TtkBingoSorteo> tablero;
}
