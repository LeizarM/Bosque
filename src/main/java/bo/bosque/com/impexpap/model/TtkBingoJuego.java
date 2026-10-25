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
 * Fila de ttk_BingoJuego (una ronda con su figura 0/1): columnas de p_list_ L y parámetros de p_abm_.
 * No trae nombres de ganadores: también la ven los participantes.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkBingoJuego implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codJuego;
    private Long codBingo;
    private Integer nroJuego;
    private String nombre;
    private String figura;
    private Integer filas;
    private Integer columnas;
    private Integer conComodin;
    private Integer casillasFigura;
    private String estado;
    private String estadoTexto;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fechaInicio;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fechaFin;
    private Integer bolillosMarcados;
    private Integer ganadoresConfirmados;
    private Integer puedeEditarFigura;
    private Integer puedeComenzar;
    private Integer puedeFinalizar;

    /** Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date audFecha;
}
