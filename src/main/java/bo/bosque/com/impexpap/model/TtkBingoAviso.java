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
 * Aviso «¡Bingo!» de un participante: columnas de p_list_ttk_BingoAviso L y parámetros de p_abm_.
 * Lleva el dueño: solo para el encargado. Nunca dice si el cartón ganó; eso se revisa con Validar.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkBingoAviso implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codAviso;
    private Long codBingo;
    private Long codJuego;
    private Integer nroJuego;
    private Long codCarton;
    private Integer nroCarton;
    private String nombreEmpleado;
    private Integer impreso;
    /** P pendiente, A atendido. */
    private String estado;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fechaAviso;
    private String mensaje;

    /** Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date audFecha;
}
