package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Vendedor al que NO se le aplica el descuento por familia
 * (tabla tcom_vendedorExentoBond).
 * <p>
 * Para los supervisores la exencion se evalua sobre el que cobra, no sobre el
 * vendedor de la nota: por eso Alexandro Zaballa comisiona sobre el total sin
 * descontar mientras Paolo y Alvaro no.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class VendedorExentoBond implements Serializable {

    private long   idVenExento;
    private long   idVendedor;

    /** Solo lectura: el nombre del vendedor en SAP. */
    private String nomVenSAP;

    private Date   vigenteDesde;
    private Date   vigenteHasta;
    private int    activo;
    private String motivo;
    private long   audUsuario;
    private Date   audFecha;
}
