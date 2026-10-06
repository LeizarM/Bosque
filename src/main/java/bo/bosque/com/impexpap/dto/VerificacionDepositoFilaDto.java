package bo.bosque.com.impexpap.dto;

import java.time.LocalDate;

import bo.bosque.com.impexpap.model.ChVerificacionDeposito;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Una verificacion con los datos de su cheque y de los bancos: fila de {@code p_list_VerificacionDeposito} rama
 * {@code A}. Esa rama devuelve columnas con nombres repetidos ({@code datoEstado} dos veces: la del cheque y la de la
 * verificacion), asi que se lee POR POSICION, igual que el legacy:
 * <pre>
 *    1 codvd          2 codCheque       3 codBanco       4 fechaBanco     5 observacion
 *    6 datoBanco      7 nroCheque       8 montoCheque    9 datoBancoCheque
 *   10 datoEstadoCheque  11 fechaCobrar  12 estado       13 datoEstado
 * </pre>
 *
 * <p>Hereda las columnas de {@link ChVerificacionDeposito}; {@code audUsuario} y {@code audFecha} no vienen en esta
 * rama y quedan en null. {@code nroCheque} es TEXTO: hay cheques con numeros como {@code 12-345} o con {@code ?} y el
 * legacy, que lo lee con {@code getInt}, perdia la lista entera con uno solo.
 *
 * <p>{@code moneda} y {@code descMoneda} no salen del procedimiento: el servicio los completa leyendo el cheque (una
 * lectura por cheque distinto de la pagina) y quedan en null si esa lectura falla.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class VerificacionDepositoFilaDto extends ChVerificacionDeposito implements FilaConChequeDto {

    /** Numero de fila global (1..total) tras ordenar. Lo pone el servicio. */
    private Integer fila;

    /** Nombre del banco donde se verifico. */
    private String datoBanco;

    private String nroCheque;

    /** {@code float} en la base; aqui un double. */
    private Double montoCheque;

    /** Nombre del banco del cheque. */
    private String datoBancoCheque;

    /** Descripcion del estado del cheque (v_tipos 23): "PENDIENTE" / "CERRADO". */
    private String datoEstadoCheque;

    @FechaDiaJson
    private LocalDate fechaCobrarCheque;

    /** La descripcion del estado del cheque es CERRADO: la misma comparacion que hacia el JSF. Lo pone el servicio. */
    private boolean chequeCerrado;

    /** Descripcion del estado de la verificacion (v_tipos 36): "Valido" / "Anulado". */
    private String datoEstado;

    /** {@code BS} / {@code SUS}; null si no se pudo leer. */
    private String moneda;

    /** "Bs" / "$us"; null si no se pudo leer. */
    private String descMoneda;
}
