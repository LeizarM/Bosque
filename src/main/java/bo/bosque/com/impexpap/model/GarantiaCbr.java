package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

/**
 * Mapea 1:1 la tabla <b>tcbr_garantia</b> (modulo de garantias de cobranza).
 *
 * <pre>
 *   codGarantia       bigint        NOT NULL IDENTITY PK
 *   codClienteSAP     varchar(50)   NOT NULL   (CardCode de SAP, sin FK: SAP es otra base)
 *   montoGarantia     decimal(15,4) NOT NULL
 *   montoCredito      decimal(15,4) NOT NULL   linea de credito aprobada por la garantia
 *   tiempoPago        int           NOT NULL   dias
 *   fechaInicio       date          NOT NULL
 *   fechaExpiracion   date          NOT NULL
 *   montoGarantiaCalc decimal(15,4) NULL       suma de los detalles (tcbr_cbrDetalle)
 *   recFirmas         varchar(30)   NULL       reconocimiento de firmas
 *   nroProtesta       varchar(30)   NULL
 *   audUsuario        bigint        NOT NULL
 *   audFecha          datetime      NOT NULL
 * </pre>
 *
 * <p><b>El estado no es una columna.</b> VIGENTE / CADUCADO / CERRADO se calcula en cada
 * lectura (fechas + existencia de una accion 'CER' en tcbr_accion) y viaja en
 * {@link bo.bosque.com.impexpap.dto.GarantiaDto}, igual que el resto de los datos de JOIN.
 *
 * <p>Todos los tipos son wrappers: {@code BeanPropertyRowMapper} revienta al escribir un
 * NULL sobre un primitivo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GarantiaCbr implements Serializable {

    /** bigint IDENTITY. El ABM lo devuelve en @idGenerado al insertar. */
    private Long codGarantia;

    /** varchar(50). CardCode del cliente en SAP. No se modifica despues del alta. */
    private String codClienteSAP;

    /** decimal(15,4). Valor de la garantia. */
    private BigDecimal montoGarantia;

    /** decimal(15,4). Linea de credito que respalda la garantia. */
    private BigDecimal montoCredito;

    /** int. Plazo de pago, en dias. */
    private Integer tiempoPago;

    /** date. */
    private Date fechaInicio;

    /** date. Una extension (accion EXT) la mueve hacia adelante. */
    private Date fechaExpiracion;

    /**
     * decimal(15,4) NULL. Suma de {@code montoGarantiaParc} de sus detalles. La calcula el
     * backend en cada cambio de detalle; no se acepta del cliente.
     */
    private BigDecimal montoGarantiaCalc;

    /** varchar(30) NULL. Reconocimiento de firmas. */
    private String recFirmas;

    /** varchar(30) NULL. Numero de protesta. */
    private String nroProtesta;

    /** bigint. Sale del token, nunca del cuerpo. */
    private Long audUsuario;

    /** datetime. La sella el procedimiento con GETDATE(). */
    private Date audFecha;
}
