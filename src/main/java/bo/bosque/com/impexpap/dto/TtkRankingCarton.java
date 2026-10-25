package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Avance de un cartón en el juego en curso: p_list_ttk_BingoCarton R (ranking) o V (verificación).
 * Lleva el dueño: solo para el encargado. esNuevo = 1 si completó con el último bolillo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkRankingCarton implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codCarton;
    private Integer nroCarton;
    private Long codEmpleado;
    private String nombreEmpleado;
    private Integer nroJuego;
    private Integer aciertos;
    private Integer totalFigura;
    private Integer faltantes;
    private Integer esBingo;
    private Integer ordenBingo;
    private Integer bolilloBingo;
    private Integer esNuevo;
    private Integer esGanadorConfirmado;
    private Long puesto;
    /** 1 = el dueño juega con el cartón impreso (el encargado sabe dónde buscarlo). */
    private Integer impreso;
    private String mensaje;
}
