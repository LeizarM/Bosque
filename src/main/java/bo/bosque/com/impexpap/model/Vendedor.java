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
public class Vendedor implements Serializable {

    private long       idVendedor;
    private String     nomVenSap;
    private BigDecimal comision;        // decimal(9,6): 0.05 = 5%
    private int        esInterno;
    private int        activo;
    private long       audUsuario;
    private Date       audFecha;

    // Codigos por empresa, planos. Los sirve la vista v_tcom_vendedor.
    // BIGINT en la tabla, no INT.
    private Long codVenPAPIRUS;
    private Long codVenIMPEXPAP;
    private Long codVenPAPELBOL;
    private Long codVenESPPAPEL;
    private Long codVenPRODPAP;

    // Solo en ACCION 'B' (listado por empresa).
    private Integer bd;
    private String  siglaEmpresa;
    private Long    codVenSap;

    // Codigos por empresa en el alta/modificacion. SQL Server 2008 no tiene JSON,
    // por eso XML: <e><i c="1" v="1234"/></e>  (c = bd, v = codVenSap)
    private String empresasXml;

}
