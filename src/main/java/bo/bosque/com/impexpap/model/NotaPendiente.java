package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

/**
 * Nota de venta cerrada pero todavia no pagada (p_list_noPagado, rama B).
 * <p>
 * El resultado mezcla detalle con totales. Las filas de total llegan con
 * idNoPagado en null y el nombre del vendedor como "Total X" o
 * "TOTAL PENDIENTES"; el resto de las columnas viene vacio.
 * <p>
 * La rama B filtra internamente por bd = 1 y excluye al codVendedor 2: son
 * reglas escritas dentro del SP, no parametros.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotaPendiente implements Serializable {

    private Long       fila;
    private Long       idNoPagado;
    private Long       codVendedor;
    private String     nombreVen;
    private Date       fechaDoc;
    private Integer    mes;
    private Integer    anio;
    private Long       docNum;
    private String     valido;      // ya viene traducido: Valido / Anulado
    private String     indicador;
    private String     estado;      // ya viene traducido: Abierta / Cerrada
    private BigDecimal montoTotalBS;
    private BigDecimal montoCerradoBS;
    private String     origen;
    private BigDecimal saldoPendiente;

    /** Sin idNoPagado la fila es un total, no una nota. */
    public boolean esTotal() {
        return idNoPagado == null;
    }
}
