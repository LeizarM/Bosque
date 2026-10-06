package bo.bosque.com.impexpap.commons;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Las reglas de los tres paneles del detalle del cheque (notas de remision, transacciones bancarias y postergaciones)
 * que el legacy aplicaba en {@code WizardCheque}, {@code NotaRemisionManagedBean} y {@code cheque.xhtml}, sin base de datos
 * y sin Spring.
 *
 * <p><b>Paridad con el legacy (decision 2 de CLAUDE.md).</b> Cada regla reproduce lo que el codigo hace hoy. Los metodos
 * devuelven el mensaje de error o {@code null} si la regla se cumple; los textos viven en {@link MensajesCheque}.
 *
 * <h3>Lo que se replica, con archivo y linea del legacy (Bosque v2)</h3>
 * <ul>
 *   <li><b>Nota de remision</b> ({@code cheque.xhtml:967-980}, {@code NotaRemisionManagedBean.java:167}): la nota es obligatoria y
 *       cumple el regex literal {@code "^[0-9  ]{5,10}"} (digitos y espacios, de 5 a 10). El nro de factura es obligatorio y
 *       {@code maxlength=6} ({@code cheque.xhtml:972}); <b>no tiene validador regex</b>: {@code validarNroFactura}
 *       ({@code NotaRemisionManagedBean.java:194}) existe pero el campo no lo enlaza, asi que la regla efectiva es un entero de
 *       a lo sumo 6 caracteres ({@code IntegerConverter}) y mayor que cero ({@code WizardCheque.java:1043}). La fecha de la
 *       factura es obligatoria ({@code cheque.xhtml:977-979}) y puede ser cualquiera.</li>
 *   <li><b>Transaccion bancaria</b> ({@code cheque.xhtml:996-1018}, {@code WizardCheque.java:2193}): el numero es obligatorio y
 *       {@code length() > 4} sobre el valor tal cual llega (sin recortar); no hay regex. La fecha es obligatoria
 *       ({@code cheque.xhtml:1009-1013}) y sin rango.</li>
 *   <li><b>Postergacion</b> ({@code cheque.xhtml:442-467}, {@code WizardCheque.java:2247}): la fecha es obligatoria; la
 *       observacion cumple {@code length() > 2} (sin recortar) y no tiene regex (los datos reales traen acentos y saltos de
 *       linea).</li>
 * </ul>
 *
 * <h3>Lo que el servidor agrega (el JSF lo hacia con el {@code maxlength} del campo o con un desplegable)</h3>
 * Factura entre 1 y 999999 (replica {@code maxlength=6}); numero de transaccion de hasta 30 caracteres y observacion de hasta
 * 200 (las columnas son {@code varchar(30)} y {@code varchar(200)} y el procedimiento truncaria en silencio al recibirlos).
 */
public final class ReglasPanelesCheque {

    private ReglasPanelesCheque() {
    }

    // ------------------------------------------------------------------ formatos

    /**
     * {@code NotaRemisionManagedBean.validarNotaRemision}: {@code "^[0-9  ]{5,10}"}. Los dos espacios dentro de la clase son
     * espacios normales (0x20), uno repetido: la clase admite digitos y el espacio. Se copia tal cual.
     */
    static final Pattern NOTA_REMISION = Pattern.compile("^[0-9  ]{5,10}");

    /** Un caracter permitido de la nota. SOLO sirve para que el mensaje diga cuales sobran: decide {@link #NOTA_REMISION}. */
    static final Pattern NOTA_REMISION_CAR = Pattern.compile("[0-9 ]");

    /** Mayor factura que cabe en el campo ({@code maxlength=6}). */
    public static final int FACTURA_MAXIMA = 999_999;

    /** La transaccion tiene que tener MAS de 4 caracteres ({@code length() > 4}). */
    public static final int TRANSACCION_LARGO_EXCLUSIVO = 4;

    /** {@code tch_chTransaccionBancaria.nroTransaccion} es {@code varchar(30)}. */
    public static final int TRANSACCION_LARGO_MAXIMO = 30;

    /** La observacion tiene que tener MAS de 2 caracteres ({@code length() > 2}). */
    public static final int OBSERVACION_LARGO_EXCLUSIVO = 2;

    /** {@code maxlength=200} del textarea y {@code varchar(200)} de la columna. */
    public static final int OBSERVACION_LARGO_MAXIMO = 200;

    // ------------------------------------------------------------------ nota de remision

    /** Nota obligatoria y con el formato del legacy (el campo es {@code required} y tiene el validador). */
    public static String errorNotaRemision(String v) {
        if (v == null || v.isEmpty()) return MensajesCheque.FALTA_NOTA_REMISION;
        return NOTA_REMISION.matcher(v).matches() ? null : MensajesCheque.notaRemisionNoValida(v, NOTA_REMISION_CAR);
    }

    /** Factura obligatoria, mayor que cero y de a lo sumo 6 digitos. */
    public static String errorNroFactura(Integer v) {
        if (v == null) return MensajesCheque.FALTA_NRO_FACTURA;
        if (v <= 0) return MensajesCheque.nroFacturaNoPositivo(v);
        if (v > FACTURA_MAXIMA) return MensajesCheque.nroFacturaMuyLargo(v, FACTURA_MAXIMA);
        return null;
    }

    public static String errorFechaFactura(Date v) {
        return v == null ? MensajesCheque.FALTA_FECHA_FACTURA : null;
    }

    /** Todos los errores de campo de una nota, en el orden del formulario: nota, factura, fecha. */
    public static List<String> erroresNotaRemision(String nota, Integer nroFactura, Date fechaFactura) {
        List<String> errores = new ArrayList<>();
        agregar(errores, errorNotaRemision(nota));
        agregar(errores, errorNroFactura(nroFactura));
        agregar(errores, errorFechaFactura(fechaFactura));
        return errores;
    }

    // ------------------------------------------------------------------ transaccion bancaria

    /** Lo que el JSF exigia con {@code required} (antes de llamar al guardado): el numero no puede estar vacio. */
    public static String errorNroTransaccionObligatorio(String v) {
        return v == null || v.isEmpty() ? MensajesCheque.FALTA_NRO_TRANSACCION : null;
    }

    public static String errorFechaTransaccion(Date v) {
        return v == null ? MensajesCheque.FALTA_FECHA_TRANSACCION : null;
    }

    /**
     * {@code saveNroTransaccion}: {@code length() > 4} ({@code "Cargue el Nro de Transaccion"}) y, agregado por el servidor,
     * de hasta 30 caracteres. Se mide el texto tal cual llega.
     */
    public static String errorLargoNroTransaccion(String v) {
        if (v == null || v.isEmpty()) return MensajesCheque.FALTA_NRO_TRANSACCION;
        if (v.length() <= TRANSACCION_LARGO_EXCLUSIVO) return MensajesCheque.nroTransaccionCorto(v, v.length());
        if (v.length() > TRANSACCION_LARGO_MAXIMO) {
            return MensajesCheque.nroTransaccionLargo(v, v.length(), TRANSACCION_LARGO_MAXIMO);
        }
        return null;
    }

    // ------------------------------------------------------------------ postergacion

    public static String errorFechaPostergacion(Date v) {
        return v == null ? MensajesCheque.FALTA_FECHA_POSTERGACION : null;
    }

    /**
     * {@code savePostePostrgcn}: {@code observacion.length() > 2} ({@code "Ingrese la Observacion"}) y, agregado por el
     * servidor, de hasta 200. Se mide el texto tal cual llega: sin regex y sin recortar.
     */
    public static String errorObservacionPostergacion(String v) {
        if (v == null || v.isEmpty()) return MensajesCheque.FALTA_OBSERVACION_POSTERGACION;
        if (v.length() <= OBSERVACION_LARGO_EXCLUSIVO) return MensajesCheque.observacionPostergacionCorta(v, v.length());
        if (v.length() > OBSERVACION_LARGO_MAXIMO) {
            return MensajesCheque.observacionPostergacionLarga(v.length(), OBSERVACION_LARGO_MAXIMO);
        }
        return null;
    }

    // ------------------------------------------------------------------ apoyo

    /** Compara dos valores como lo hace {@code =} de SQL Server con {@code varchar}: sin mirar los espacios del final. */
    public static boolean mismoValorSql(String a, String b) {
        if (a == null || b == null) return false;
        return sinEspaciosFinales(a).equalsIgnoreCase(sinEspaciosFinales(b));
    }

    private static String sinEspaciosFinales(String s) {
        int fin = s.length();
        while (fin > 0 && s.charAt(fin - 1) == ' ') fin--;
        return s.substring(0, fin);
    }

    private static void agregar(List<String> errores, String error) {
        if (error != null) errores.add(error);
    }
}
