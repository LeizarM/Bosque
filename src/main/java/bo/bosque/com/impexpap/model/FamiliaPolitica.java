package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

/**
 * Politica de descuento de una familia SAP (tabla tcom_familiaPolitica).
 * <p>
 * <b>porcentajePago es LO QUE SE PAGA, no lo que se descuenta.</b> 50 significa
 * que ese item entra a la mitad; 100 es no descontar nada. Se eligio asi porque
 * es como lo dijo el negocio -«se les va a dar el 50%»- y porque apagar la regla
 * es poner 100, que se lee solo.
 * <p>
 * grpFam y alias vienen del listado para mostrar y no viajan al SP de escritura.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FamiliaPolitica implements Serializable {

    private long       idFamPolitica;
    private long       idGrpFamiliaSap;

    /** Solo lectura: el nombre de la familia. */
    private String     grpFam;
    /** Solo lectura: el alias corto de la familia. */
    private String     alias;

    private BigDecimal porcentajePago;
    private Date       vigenteDesde;
    private Date       vigenteHasta;
    private int        activo;
    private long       audUsuario;
    private Date       audFecha;
}
