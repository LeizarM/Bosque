package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Bolillo del tablero: columnas de p_list_ttk_BingoSorteo L / T y parámetros de p_abm_.
 * En T vienen todos los números del bingo, con marcado 0/1 y su orden de salida.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkBingoSorteo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codSorteo;
    private Long codBingo;
    private Integer nroJuego;
    private Integer nroBolillo;
    private Integer columna;
    private String letra;
    private String etiqueta;
    private Integer marcado;
    private Integer orden;

    /** Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date audFecha;
}
