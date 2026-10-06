package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Un cheque que todavia no tiene una verificacion valida: fila de {@code p_list_VerificacionDeposito} ramas
 * {@code C} y {@code D}. Se lee POR POSICION (13 columnas, algunas con relleno que no se usa):
 * <pre>
 *    1 codvd (siempre 0)      2 codCheque       3 elCodBanco (banco DEL CHEQUE)   4 fechaVerificacion (GETDATE)
 *    5 observacion (' ')      6 datoBanco ('- Sin Verificar -')
 *    7 nroCheq                8 montCheq        9 datoBancoCheq
 *   10 datoEstado (del cheque)   11 fechaCobrar   12 estado ('Y')   13 datoEstado ('Valido')
 * </pre>
 * Solo se conservan los datos del cheque; el relleno ({@code codvd}, la observacion en blanco, la fecha de hoy y la
 * verificacion 'Y' / 'Valido' de mentira) no es de nadie todavia.
 *
 * <p>{@code nroCheque} es TEXTO (ver {@link VerificacionDepositoFilaDto}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ChequePendienteFilaDto implements FilaConChequeDto, Serializable {

    /** Numero de fila global (1..total). Lo pone el servicio. */
    private Integer fila;

    private Long codCheque;

    /** El banco DEL CHEQUE: es el que el formulario propone como banco de la verificacion. */
    private Long codBanco;

    private String nroCheque;

    private Double montoCheque;

    private String datoBancoCheque;

    /** Descripcion del estado del cheque (v_tipos 23): "PENDIENTE" / "CERRADO". */
    private String datoEstadoCheque;

    @FechaDiaJson
    private LocalDate fechaCobrarCheque;

    /** La descripcion del estado es CERRADO: el JSF no mostraba "Seleccionar" para esos. Lo pone el servicio. */
    private boolean chequeCerrado;

    private String moneda;

    private String descMoneda;
}
