package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Filtro de las vistas preliminares de comision.
 * <p>
 * tc es el tipo de cambio y el SP lo usa como divisor para expresar el importe
 * en dolares. Si llega en cero o nulo el SP dividiria por cero, asi que el
 * controller lo rechaza antes de llamarlo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FiltroPreliminarDto implements Serializable {

    private int mes;
    private int anio;
    private BigDecimal tc;
}
