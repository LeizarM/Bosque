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
 * Fila de ttk_Evento: columnas de p_list_ttk_Evento L y parámetros de p_abm_ttk_Evento.
 * Envoltorios para que un valor ausente viaje como null y el SP no lo tome como filtro.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkEvento implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codEvento;
    private String nombre;
    private String descripcion;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fecha;
    private String lugar;
    private Integer usaTickets;
    private Integer usaBingo;
    private Integer mesesReciente;
    private String estado;
    private String estadoTexto;
    private Integer totalParticipantes;
    private Integer totalEmpleados;
    private Integer totalRecientes;
    private Integer totalInvitados;
    private Integer totalTipos;
    private Integer totalTickets;
    private Long codBingo;
    private String estadoBingo;
    private Integer puedeEditar;
    private Integer puedeEmitir;
    private Integer puedeVolverBorrador;
    private Integer puedeImprimir;
    private Integer puedeCerrar;
    private Integer puedeEliminar;

    /** Filtro de L (@anio); no es columna. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer anio;

    /** Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date audFecha;
}
