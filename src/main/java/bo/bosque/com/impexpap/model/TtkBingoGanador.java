package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/** Ganador confirmado de un juego (solo encargado): columnas de p_list_ttk_BingoGanador L y parámetros de p_abm_. */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkBingoGanador implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codGanador;
    private Long codJuego;
    private Long codBingo;
    private Integer nroJuego;
    private String nombreJuego;
    private Long codCarton;
    private Integer nroCarton;
    private Long codEmpleado;
    private String nombreEmpleado;
    private Integer ordenBolillo;
    private Integer nroBolillo;
    private Integer puedeAnular;
    /** 1 = ganó el desempate entre los confirmados del juego; los demás «empataron». */
    private Integer ganoDesempate;
    /** Cómo se resolvió el empate; en /ganador/desempate es el cuerpo junto con codGanador. */
    private String motivoDesempate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fechaDesempate;
    /** 1 con dos o más confirmados y el juego en curso: el panel ofrece «Resolver empate». */
    private Integer puedeResolverEmpate;

    /** Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date audFecha;
}
