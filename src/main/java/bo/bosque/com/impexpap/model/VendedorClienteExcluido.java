package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Cliente cuyas notas NO cuentan para el total de un vendedor
 * (tabla tcom_vendedorClienteExcluido).
 * <p>
 * No tiene nada que ver con el descuento Bond: es otra regla. Vivia escrita a
 * mano dentro de p_list_paraPagar como `case when vs.idVendedor = 64` contra
 * tres cardCode fijos, y se trajo a tabla para que el proximo cliente sea una
 * fila y no un ALTER PROCEDURE.
 * <p>
 * origen en null significa «en todas las empresas»: el mismo cardCode puede
 * existir en IMPEXPAP y en ESPPAPEL.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class VendedorClienteExcluido implements Serializable {

    private long   idVenCliExc;
    private long   idVendedor;

    /** Solo lectura: el nombre del vendedor en SAP. */
    private String nomVenSAP;

    private String cardCode;
    private String origen;
    private Date   vigenteDesde;
    private Date   vigenteHasta;
    private int    activo;
    private String motivo;
    private long   audUsuario;
    private Date   audFecha;
}
