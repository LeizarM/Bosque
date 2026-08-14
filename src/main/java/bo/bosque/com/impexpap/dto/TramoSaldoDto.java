package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.utils.Fmt;
import lombok.*;

import java.io.Serializable;

/**
 * Uno de los 5 tramos que explican de dónde sale el saldo de vacación.
 *
 * <p>El SP {@code p_list_Permiso @ACCION='D'} devuelve los tramos APLANADOS: 5 columnas de
 * etiqueta y 5 de monto, en dos bloques distintos del SELECT y emparejadas sólo por el orden.
 * Este DTO las vuelve a juntar de a pares, que es como se pinta la pantalla —una lista de 5
 * filas en el móvil, una tabla de 2 columnas en el escritorio.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TramoSaldoDto implements Serializable {

    // ── claves estables (el cliente switchea por esto, NO por la etiqueta) ──
    public static final String SALDO_PENULTIMO = "SALDO_PENULTIMO";
    public static final String ASIGNADA_ANIO   = "ASIGNADA_ANIO";
    public static final String ACUMULADA       = "ACUMULADA";
    public static final String UTILIZADA       = "UTILIZADA";
    public static final String PROGRAMADA      = "PROGRAMADA";

    // ── signos ─────────────────────────────────────────────────────────────
    /** Suma al total. */
    public static final String DEBE  = "DEBE";
    /** Resta del total. */
    public static final String HABER = "HABER";
    /** <b>No entra en el total.</b> Ver {@link #signo}. */
    public static final String INFO  = "INFO";

    /** Una de las 5 constantes de arriba. Invariable aunque el SP cambie el texto. */
    private String  clave;
    /**
     * La etiqueta que generó el SP, <b>literal</b>. Se muestra tal cual, con sus espacios
     * raros incluidos: el criterio de aceptación #4 la compara carácter por carácter contra el
     * modal del ERP legacy. Nada de {@code trim()}, {@code replaceAll} ni capitalizar.
     */
    private String  etiqueta;
    private double  monto;
    /** {@link #monto} ya formateado — ver {@link Fmt#dias(double)}. */
    private String  montoTxt;
    /**
     * {@link #DEBE} · {@link #HABER} · {@link #INFO}.
     *
     * <p><b>SUPUESTO D1 — pendiente de confirmación de RR.HH. (ver plan §5).</b> Existe para
     * que el contrato diga en voz alta que el tramo {@link #ACUMULADA} (las duodécimas del año
     * en curso) NO entra en el total, igual que en el legacy. Sin este campo, un cliente que
     * sumara los 5 montos obtendría un total distinto del que muestra el ERP y nadie sabría
     * cuál está mal.
     */
    private String  signo;
    /**
     * Si el tramo tiene drill-down. <b>{@code false} en TODA la Fase 1</b>: los endpoints de
     * detalle son de la Fase 2. Viaja igual desde ahora para que activarlos después no cambie
     * el contrato ni obligue a tocar el modelo Dart.
     */
    private boolean tieneDetalle;

    /**
     * Un tramo de Fase 1: sin detalle y con el {@code montoTxt} ya resuelto.
     *
     * <p>Está acá y no en el DAO para que los 5 tramos se armen todos igual — cinco bloques
     * copiados a mano son cinco oportunidades de olvidarse el {@code Fmt.dias} en uno solo.
     */
    public static TramoSaldoDto de(String clave, String etiqueta, double monto, String signo) {
        return new TramoSaldoDto(clave, etiqueta, monto, Fmt.dias(monto), signo, false);
    }
}
