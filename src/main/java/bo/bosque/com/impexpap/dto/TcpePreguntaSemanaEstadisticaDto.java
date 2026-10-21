package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Fila de {@code p_list_tcpe_PreguntaSemana 'E'} (gestión): cuántos eligieron cada opción, también las que nadie
 * eligió. {@code porcentaje} es sobre el total de respuestas.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpePreguntaSemanaEstadisticaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codRsptaOp;

    private String opcionRspta;

    private Boolean esValido;

    private Integer veces;

    private BigDecimal porcentaje;
}
