package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/** Fila de ttk_ParticipanteTicket (vales por tipo): columnas de p_list_ L y parámetros de p_abm_. */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkParticipanteTicket implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codParticipanteTicket;
    private Long codParticipante;
    private Long codTipoTicket;
    private String nombreTipo;
    private String colorHex;
    private Integer cantidad;
    private Integer cantidadDefecto;
    private Integer cantidadManual;
    private Integer nroDesde;
    private Integer nroHasta;
    private String rangoTexto;
    private Integer puedeEditar;

    /** Filtro de L y P (@codEvento); no es columna. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long codEvento;

    /** Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date audFecha;
}
