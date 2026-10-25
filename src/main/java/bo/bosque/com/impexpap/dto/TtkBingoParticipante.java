package bo.bosque.com.impexpap.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/** Bingo visto por el empleado que llama (cuántos cartones tiene, si puede elegir): p_list_ttk_Bingo A. */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkBingoParticipante implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codBingo;
    private Long codEvento;
    private String nombreEvento;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fechaEvento;
    private String estado;
    private String estadoTexto;
    private Integer filas;
    private Integer columnas;
    private String letras;
    private Integer conComodin;
    private Integer nroJuegos;
    /** P en pantalla, I impreso, A ambos; conAviso = 1 si tiene el botón «¡Bingo!». */
    private String modalidad;
    private Integer conAviso;
    private Integer maxCartonesEmpleado;
    private Integer misCartones;
    private Integer cartonesDisponibles;
    private Integer puedeElegir;
    private Integer puedeVerEnVivo;
    private String mensaje;
}
