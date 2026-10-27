package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.TtkBingoGanador;
import bo.bosque.com.impexpap.model.TtkBingoSorteo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Lo que ven los participantes en vivo: estado del bingo, tablero y los ganadores ya confirmados del juego
 * (nombre y codEmpleado para la foto), sin ranking ni avisos. Inmutable: se comparte desde la caché.
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
    private List<TtkBingoGanador> ganadores;

    /** Sin ganadores confirmados. */
    public TtkEstadoPublico(long version, TtkBingoPublico publico, List<TtkBingoSorteo> tablero) {
        this(version, publico, tablero, Collections.emptyList());
    }
}
