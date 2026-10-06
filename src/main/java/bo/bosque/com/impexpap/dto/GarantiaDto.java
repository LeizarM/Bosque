package bo.bosque.com.impexpap.dto;

import java.math.BigDecimal;
import java.util.Date;

import bo.bosque.com.impexpap.model.GarantiaCbr;
import lombok.*;

/**
 * Una garantia con sus datos calculados: fila de {@code p_list_GarantiaCbr} ACCION 'G'.
 *
 * <p>Hereda las columnas de {@link GarantiaCbr} y agrega lo que sale de JOIN o de calculo.
 * Los nombres coinciden con los alias del SELECT porque {@code BeanPropertyRowMapper} mapea
 * por nombre: un alias distinto deja el campo en null sin avisar.
 *
 * <p><b>Cliente deduplicado:</b> {@code v_clientesSAP} trae una fila por empresa SAP; la rama
 * 'G' agrupa por CardCode, asi que cada garantia sale una sola vez y {@code creditLine} /
 * {@code balance} son la SUMA de todas las empresas del cliente.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class GarantiaDto extends GarantiaCbr {

    /** Nombre del cliente en SAP ('SIN NOMBRE EN SAP' si no esta). */
    private String datoCliente;

    /** VIGENTE / CADUCADO / CERRADO. Calculado: no se guarda en ninguna columna. */
    private String datoEstado;

    /** Dias hasta fechaExpiracion. Negativo = ya vencio. */
    private Integer diasParaVencer;

    /** Linea de credito del cliente en SAP, sumada entre empresas. */
    private BigDecimal creditLine;

    /** Saldo del cliente en SAP, sumado entre empresas. */
    private BigDecimal balance;

    /** 1 si la garantia ya tiene su accion TRASP. Todo lo que no sea una nota la exige. */
    private Integer traspasada;

    /** Cantidad de detalles (tcbr_cbrDetalle). */
    private Integer cantDetalles;

    /** Descripciones de los tipos de sus detalles, separadas por coma. */
    private String tiposGarantia;

    /** Fecha de la accion REG, es decir, de la recepcion de la garantia. */
    private Date fechaRegistro;

    /** Observacion de la accion REG. */
    private String observacionRegistro;

    /** Nombre del empleado que registro la garantia. */
    private String realizoEmp;

    /** Atajo para las validaciones del controlador. */
    public boolean esCerrada() {
        return "CERRADO".equals(datoEstado);
    }
}
