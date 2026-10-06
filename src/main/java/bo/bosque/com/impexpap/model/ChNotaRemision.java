package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Mapea 1:1 la tabla <b>tch_notaRemision</b>: las notas de remision (y la factura SAP) de un cheque.
 *
 * <pre>
 *   codCheque    int          NULL   FK a tch_cheque
 *   notaRemision varchar(10)  NULL
 *   nroFactura   int          NULL
 *   fechaFactura date         NULL
 *   audUsuario   int          NULL
 *   audFecha     datetime     NULL
 * </pre>
 *
 * <p><b>La tabla no tiene PK ni indices (es un heap)</b>: la clave de hecho es {@code codCheque} + {@code notaRemision}
 * y nada impide repetirla (en {@code BOSQUE2PRUEBA} hay 41 pares repetidos). {@code p_abm_NotaRemision} 'D' borra
 * <b>todas</b> las filas del par. No tiene triggers.
 *
 * <p>Todos los tipos son wrappers: {@code BeanPropertyRowMapper} revienta al escribir un NULL sobre un primitivo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChNotaRemision implements Serializable {

    /** int NULL. Cheque al que pertenece ({@code tch_cheque.codCheque}). */
    private Integer codCheque;

    /** varchar(10). Solo digitos y espacios, 5 a 10 caracteres (el validador JSF). */
    private String notaRemision;

    /** int. Numero de factura de SAP. */
    private Integer nroFactura;

    /** date. En JSON va como {@code yyyy-MM-dd}, sin hora ni zona. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaFactura;

    /** int. Sale del token, nunca del cuerpo. */
    private Integer audUsuario;

    /** datetime. La sella el procedimiento con GETDATE(). */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date audFecha;
}
