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
public class VendedorEmpresa implements Serializable {

    private long   idVendedorEmpresa;
    private long   idVendedor;
    private int    codEmpresa;
    private int    codVenSap;
    private long   audUsuario;
    private Date   audFecha;

}
