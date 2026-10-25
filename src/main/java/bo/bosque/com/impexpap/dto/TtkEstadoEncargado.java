package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.TtkBingoAviso;
import bo.bosque.com.impexpap.model.TtkBingoSorteo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Lo que ve el panel del encargado: el estado público más el ranking (con dueños), el aviso del último
 * cambio (mensaje del SP, incluye «¡BINGO…» si hubo) y los «¡Bingo!» pendientes de los participantes.
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TtkEstadoEncargado implements Serializable {

    private static final long serialVersionUID = 1L;

    private long version;
    private TtkBingoPublico publico;
    private List<TtkBingoSorteo> tablero;
    private List<TtkRankingCarton> ranking;
    private String aviso;
    private List<TtkBingoAviso> avisos;
}
