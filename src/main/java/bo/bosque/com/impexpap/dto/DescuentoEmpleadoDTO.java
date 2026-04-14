package bo.bosque.com.impexpap.dto;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
@Getter
@Setter
@ToString
@NoArgsConstructor
public class DescuentoEmpleadoDTO implements Serializable {
    private int codEmpleado;
    private String descripcion;
    private String moneda;
    private float montoTotal;
    private int totalCuotas;
    private String periodo;
    private String tipoDescuento;
    private String estadoDescuento;
    private int primeraCuotaMes;
    private int ultimaCuotaMes;
    private float montoDescuento;
    private float saldoRestante;
}