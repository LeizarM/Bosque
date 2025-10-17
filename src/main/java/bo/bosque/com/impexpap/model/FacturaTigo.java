package bo.bosque.com.impexpap.model;
import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FacturaTigo {
    private int codFactura;
    private String nroFactura;
    private String tipoServicio;
    private int nroContrato;
    private int nroCuenta;
    private String periodoCobrado;
    private String descripcionPlan;
    private Float totalCobradoXCuenta;
    private String estado;
    private int audUsuario;

}
