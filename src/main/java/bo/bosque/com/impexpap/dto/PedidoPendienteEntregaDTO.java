package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class PedidoPendienteEntregaDTO {

    private String empresa;
    private long docEntry;
    private String cardName;
    private Date docDate;
    private String horaCreacion;
    private BigDecimal weight;
    private BigDecimal cantidad;
    private String comments;
    private String direccionEntrega;
    private String vendedor;
    private String sistema;
    private String docNum;
    private String seriesName;
    private String tipoEntrega;
}
