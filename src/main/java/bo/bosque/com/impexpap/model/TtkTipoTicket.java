package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/** Fila de ttk_TipoTicket: columnas de p_list_ttk_TipoTicket L y parámetros de p_abm_ttk_TipoTicket. */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkTipoTicket implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codTipoTicket;
    private Long codEvento;
    private String nombre;
    private String leyenda;
    private Integer cantEmpleado;
    private Integer cantReciente;
    private Integer cantInvitado;
    private String colorHex;
    private Integer orden;
    private Integer totalVales;
    private Integer totalParticipantes;
    /** 1 si es el tipo enlazado al bingo del evento: cada vale da un cartón. */
    private Integer daCartonBingo;
    private Integer puedeEditar;

    /** Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date audFecha;
}
