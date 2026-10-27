package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/** Estado público de un bingo (juego actual, figura, premio, desempate y último bolillo): p_list_ttk_Bingo P. */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkBingoPublico implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codBingo;
    private String nombreEvento;
    private String estado;
    private String estadoTexto;
    private Integer nroBolillos;
    private Integer filas;
    private Integer columnas;
    private String letras;
    private Integer conComodin;
    private Integer nroJuegos;
    private Integer nroJuego;
    private String nombreJuego;
    private String figura;
    private String estadoJuego;
    private Integer totalMarcados;
    private Integer ultimoBolillo;
    /** 1 si el juego ya tiene un ganador confirmado; sus nombres van en TtkEstadoPublico.ganadores. */
    private Integer hayGanador;
    /** 1 si hay juego en curso y el encargado dejó prendido el botón «¡Bingo!» (solo avisa, nunca dice si ganó). */
    private Integer puedeAvisar;
    private String premio;
    /** «Se está jugando por …» en curso, «Premio: …» con ganador; null sin premio. */
    private String textoPremio;
    private String reglaEmpate;
    /** «Si hay empate: …» antes; «¡Empate! …» con más de un ganador; null si no aplica. */
    private String textoEmpate;
    private Integer nroGanadores;
    private String mensaje;
}
