package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/** Fila de ttk_Bingo para el encargado: columnas de p_list_ttk_Bingo L y parámetros de p_abm_ttk_Bingo. */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkBingo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codBingo;
    private Long codEvento;
    private String nombreEvento;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fechaEvento;
    private Integer nroBolillos;
    private Integer filas;
    private Integer columnas;
    private String letras;
    private Integer conComodin;
    private Integer nroJuegos;
    private Integer nroCartones;
    /** Tipo de vale que da cartones (cada vale, uno). En guardar: 0 quita el enlace y null no lo cambia. */
    private Long codTipoTicket;
    private String nombreTipoTicket;
    private Integer valesBingo;
    private Integer cartonesObjetivo;
    private Integer cartonesFaltantes;
    private Integer maxCartonesEmpleado;
    private Integer tamRanking;
    /** Forma de juego: P en pantalla, I impreso, A ambos (el encargado decide por cartón). En guardar, null no cambia. */
    private String modalidad;
    private String modalidadTexto;
    /** 1 = el participante tiene el botón «¡Bingo!» para avisar al encargado. En guardar, null no cambia. */
    private Integer conAviso;
    private String estado;
    private String estadoTexto;
    private Integer nroGeneracion;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date fechaGeneracion;
    private Integer cartonesGenerados;
    private Integer cartonesAsignados;
    private Integer cartonesDisponibles;
    /** Cartones con dueño que se entregan impresos (lo que imprime el encargado con «solo impresos»). */
    private Integer cartonesImpresos;
    private Integer empleadosConCarton;
    private Integer juegosFinalizados;
    private Integer nroJuegoActual;
    private String nombreJuegoActual;
    private String rangos;
    private String mensajeEstado;
    private Integer puedeConfigurar;
    private Integer puedeGenerar;
    private Integer puedeAbrirEleccion;
    private Integer puedeIniciar;
    private Integer puedeAsignar;
    private Integer puedeFinalizar;
    private Integer puedeImprimir;

    /** Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date audFecha;
}
