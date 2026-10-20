package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.utils.TcpeHora;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_Lote 'L'}: el lote con el conteo de sus exámenes por estado.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeLoteDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codRolExamen;

    private String titulo;

    private String observaciones;

    private Long codArea;

    private String nombreArea;

    private Long codClasificacion;

    private String nombreClasificacion;

    private String modalidad;

    private Integer duracionMinutos;

    private Integer cantidadPreg;

    private Integer cantExamenGenerar;

    @FechaDiaJson
    private LocalDate fecha;

    /** HH:mm:ss. */
    private String horaInicio;

    /** HH:mm:ss. */
    private String horaFin;

    private String ubicacion;

    /** {@code fecha + horaInicio}: antes no se puede iniciar. Nulo = sin límite. */
    private Date inicioVentana;

    /** {@code fecha + horaFin}: después no se inicia y los no iniciados quedan AUSENTE. Nulo = sin límite. */
    private Date finVentana;

    private Integer examenes;

    private Integer sinIniciar;

    private Integer enCurso;

    /** Pasaron su fecha límite sin entregar; se entregan al listar resultados. */
    private Integer vencidos;

    /** No iniciaron antes de {@code finVentana}: cerrados con resultado 0. */
    private Integer ausentes;

    private Integer porRevisar;

    private Integer calificados;

    private Double promedio;

    private BigDecimal notaAprobacion;

    private Boolean mostrarRespuestas;

    /** Calificados con {@code resultado >= notaAprobacion}. */
    private Integer aprobados;

    private Integer reprobados;

    private Long audUsuario;

    private Date audFecha;

    private String nombreUsuario;

    /** jTDS entrega el {@code time(7)} como texto con fracción de segundo. */
    public void setHoraInicio(String horaInicio) {
        this.horaInicio = TcpeHora.normalizar(horaInicio);
    }

    public void setHoraFin(String horaFin) {
        this.horaFin = TcpeHora.normalizar(horaFin);
    }
}
