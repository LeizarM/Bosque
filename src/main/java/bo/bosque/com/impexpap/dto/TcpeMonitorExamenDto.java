package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_Monitor 'L'} (contrato 8.4): un evaluado del lote en el panel "En vivo", primero los
 * que están en curso. {@code conexion} la calcula SQL con los umbrales de {@code f_tcpe_config}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeMonitorExamenDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codExamen;

    private String login;

    private String nombreCompleto;

    private String ci;

    private String estado;

    private Date fechaInicio;

    private Date fechaLimite;

    private Long segundosRestantes;

    private Integer totalPreguntas;

    private Integer respondidas;

    /** Número de la pregunta que está viendo. */
    private Integer ordenActual;

    private Date ultimaActividad;

    private Long segundosSinActividad;

    /** CONECTADO, INACTIVO, DESCONECTADO o {@code -} si no está en curso. */
    private String conexion;

    private Integer salidasPestana;

    private Date ultimaSalida;

    private Double resultado;
}
