package bo.bosque.com.impexpap.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/** Un vale numerado listo para imprimir: fila de p_list_ttk_ParticipanteTicket P. */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkValeImpresion implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codEvento;
    private String nombreEvento;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fechaEvento;
    private String lugar;
    private Long codTipoTicket;
    private String nombreTipo;
    private String leyenda;
    private String colorHex;
    private Long codParticipante;
    private String nombreParticipante;
    private String categoriaTexto;
    private Integer nroTicket;
    private String nroTicketTexto;
}
