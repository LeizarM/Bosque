package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

/**
 * Una nota que compone una fila del preliminar (p_list_paraPagar, rama G1).
 * <p>
 * Es el desglose que en Bosque v2 abria el boton «Ver Notas a Pagar» de cada
 * fila: las facturas cerradas y no pagadas de ese vendedor, en ese periodo, que
 * cayeron en ese tramo de comision. La suma de {@code montoCerradoBS} es el
 * «Monto base» de la fila que lo origino.
 * <p>
 * El SP cierra el listado con una fila de TOTAL: llega con {@code idVendedor=0},
 * {@code docNum=0} y el resto en null, y {@code montoCerradoBS} con la suma.
 * {@link #isEsTotal()} la identifica para que el cliente no la mezcle con las
 * notas reales.
 * <p>
 * {@code valido} y {@code estado} ya vienen traducidos por el SP: 'V'/'A' salen
 * como Valido/Anulada y 'O'/'C' como Abierta/Cerrada.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotaPreliminar implements Serializable {

    private Long idVendedor;
    private String nombreVen;

    /** Fecha de la factura. */
    private Date fechaDoc;

    private Integer mes;
    private Integer anio;

    /** Numero de documento en SAP. */
    private Long docNum;

    /** Valido o Anulada. */
    private String valido;

    private String indicador;

    /** Abierta o Cerrada. */
    private String estado;

    /** Monto de la venta cerrada, en bolivianos. */
    private BigDecimal montoCerradoBS;

    /** Sistema del que viene la nota: IMPEXPAP, ESPPAPEL, PRODUCTIVA PAPEL. */
    private String origen;

    private BigDecimal tc;

    /** Fecha del ultimo cobro. Con {@code diferenciaDias} define el tramo. */
    private Date fechaUltimoPago;

    /** Dias entre la factura y el cobro. Es lo que decide que porcentaje paga. */
    private Integer diferenciaDias;

    /**
     * La fila de cierre que agrega el SP, no una nota.
     * <p>
     * Se detecta por {@code docNum} en 0 y no por el nombre en null, porque el
     * total es la unica fila sin documento y eso no depende de que columnas
     * decida devolver el SP.
     */
    public boolean isEsTotal() {
        return docNum == null || docNum == 0L;
    }
}
