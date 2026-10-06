package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

/**
 * Una fila por cliente con garantias: {@code p_list_GarantiaCbr} ACCION 'R'. Es la pantalla
 * principal del modulo (en el legacy, la rama 'C').
 *
 * <p>Los montos suman solo las garantias VIGENTES. {@code creditLine} y {@code balance} son
 * de SAP y suman todas las empresas del cliente. La pantalla resalta la fila cuando
 * {@code montoCredito} (lo aprobado por garantia) no coincide con {@code creditLine}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GarantiaResumenClienteDto implements Serializable {

    private String codClienteSAP;
    private String datoCliente;
    private Integer cantGarantias;
    private Integer cantVigentes;
    private BigDecimal montoGarantia;
    private BigDecimal montoCredito;

    /** Vencimiento mas proximo entre las vigentes; null si no tiene ninguna vigente. */
    private Date proximoVencimiento;

    /** Dias hasta {@code proximoVencimiento}; null si no tiene ninguna vigente. */
    private Integer diasParaVencer;

    private BigDecimal creditLine;
    private BigDecimal balance;
}
