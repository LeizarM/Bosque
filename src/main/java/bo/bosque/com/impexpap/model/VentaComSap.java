package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class VentaComSap implements Serializable {

    // No es una tabla: es el result set de p_list_tcom_Ventas, que lee de SAP.
    private long       idVendedor;
    private int        codVenSap;
    private String     nomVenSap;
    private int        codEmpresa;
    private String     siglaEmpresa;
    private int        mes;
    private int        anio;
    private BigDecimal ventaTotal;
    private BigDecimal pendiente;
    private BigDecimal cobrado;
    private BigDecimal cobradoCerrado;
    private BigDecimal usdV;
    private BigDecimal bsV;
    private BigDecimal usdC;
    private BigDecimal bsC;
    private String     grupo;
    private BigDecimal porcentaje;
    private BigDecimal comision;
    private long       audUsuario;

}
