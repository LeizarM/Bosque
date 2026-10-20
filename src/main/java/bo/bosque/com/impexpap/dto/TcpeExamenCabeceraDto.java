package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila de {@code p_list_tcpe_Resultado 'E'}: cabecera de cada examen de un lote, para el PDF del escrito y del
 * solucionario. Sin usuario (modo cantidad), {@code nombreCompleto} y {@code ci} vienen nulos.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeExamenCabeceraDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codExamen;

    /** Número del examen dentro del lote (ROW_NUMBER). */
    private Long numero;

    private String tituloLote;

    private String nombreClasificacion;

    private String nombreArea;

    @FechaDiaJson
    private LocalDate fecha;

    private Integer duracionMinutos;

    private BigDecimal notaAprobacion;

    private String login;

    private String nombreCompleto;

    private String ci;
}
