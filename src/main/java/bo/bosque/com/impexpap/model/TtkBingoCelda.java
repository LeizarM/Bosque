package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Casilla de un cartón por juego: columnas de p_list_ttk_BingoCelda C / E / M / P.
 * numero 0 es el comodín; nombreEmpleado solo llega en E, M y P (nunca en C).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkBingoCelda implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codCarton;
    private Long codBingo;
    private Integer nroCarton;
    private Long codEmpleado;
    private String nombreEmpleado;
    private Integer nroJuego;
    private String nombreJuego;
    private Integer posicion;
    private Integer fila;
    private Integer columna;
    private Integer numero;
    private Integer esComodin;
    private Integer enFigura;
    private Integer marcado;

    /** Filtros de la impresión P (rango de cartones, solo asignados y solo impresos 1/0); no son columnas. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer nroDesde;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer nroHasta;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer soloAsignados;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer soloImpresos;
}
