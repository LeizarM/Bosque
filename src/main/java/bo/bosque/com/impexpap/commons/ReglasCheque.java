package bo.bosque.com.impexpap.commons;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Las reglas de negocio del modulo de cheques que el legacy aplicaba en {@code WizardCheque},
 * {@code ChequeManagedBean} y {@code AccionManagedBean}, sin base de datos y sin Spring.
 *
 * <p><b>Paridad con el legacy (decision 2 de CLAUDE.md).</b> Cada regla reproduce lo que el
 * codigo hace hoy, no lo que dicen los mensajes. Los formatos usan la <b>misma expresion regular
 * del validador JSF</b>, copiada literalmente (con los acentos como escapes {@code \\u00xx}); el
 * unico cambio es que aqui se evaluan con {@code matches()} en el servidor y no solo en el
 * navegador. Donde la regla copiada hace algo distinto de lo que su mensaje promete esta anotado.
 *
 * <p>Los metodos devuelven el mensaje de error o {@code null} si la regla se cumple. Los mensajes
 * son los del legacy.
 */
public final class ReglasCheque {

    private ReglasCheque() {
    }

    // ------------------------------------------------------------------ formatos

    /**
     * {@code ChequeManagedBean.validarNroChk}: {@code "[0-9?-]*{2,25}"}. <b>El {@code {2,25}} no
     * limita nada</b>: en Java un cuantificador despues de {@code *} se aplica a una expresion
     * vacia, asi que lo que rige es {@code [0-9?-]*} (digitos, {@code ?} y guion, de cualquier
     * largo; el largo real lo limita {@code maxlength=45} del campo, y {@code required} el vacio).
     * El mensaje dice "entre 2 y 25", pero no se hace cumplir. Se conserva tal cual.
     */
    static final Pattern NRO_CHEQUE = Pattern.compile("[0-9?-]*{2,25}");

    /** {@code validarDestinatarioCheque}: letras, acentos, espacios y apostrofe, 0 a 100. */
    static final Pattern A_ORDEN_DE = Pattern.compile(
            "^[a-zA-Z ñÑáÁéÉíÍóÓúÚ ' ' ]{0,100}");

    /** {@code validarRecManual}: letras, numeros, espacios y apostrofe, 1 a 25. */
    static final Pattern RECIBO_MANUAL = Pattern.compile("^[a-zA-Z0-9  ' ' ]{1,25}");

    /** {@code validarNroTalonarioManual}: letras, numeros y espacios, 1 a 25. */
    static final Pattern NRO_TALONARIO = Pattern.compile("^[a-zA-Z0-9 ]{1,25}");

    /** {@code AccionManagedBean.validarObservacion}: letras, numeros, acentos, espacio y {@code ' , ; : .}, 2 a 200. */
    static final Pattern OBSERVACION = Pattern.compile(
            "^[a-zA-Z0-9 ñÑáÁéÉíÍóÓúÚ  ' ' ',' ';' ':' '.' ]{2,200}");

    /** {@code AccionManagedBean.validarNroSap}: letras, numeros, acentos, espacio y apostrofe, 2 a 40. */
    static final Pattern NRO_SAP = Pattern.compile(
            "^[a-zA-Z0-9 ñÑáÁéÉíÍóÓúÚ ' ' ]{2,40}");

    /**
     * {@code BancoManagedBean.validarNombre}: {@code "^[a-zA-z0-9 ' ' '-'  /S]{3,50}"}. Se copia tal cual,
     * con sus rarezas: {@code A-z} incluye {@code [ \ ] ^ _ `}, la {@code S} suelta es la letra S y
     * <b>el {@code '-'} no es un guion</b>: entre dos apostrofes es el rango {@code '} a {@code '}, un solo
     * caracter. El nombre NO admite guion, punto, acentos ni parentesis (el mensaje del propio legacy
     * promete "letras y numeros"). Fijado en {@code ReglasChequeTest#nombreBanco}.
     */
    static final Pattern NOMBRE_BANCO = Pattern.compile("^[a-zA-z0-9 ' ' '-'  /S]{3,50}");

    // Un caracter permitido de cada campo. SOLO sirven para que el mensaje diga cuales caracteres sobran: quien decide
    // si el valor es valido sigue siendo el patron completo de arriba (ReglasChequeTest comprueba que coinciden).
    static final Pattern NRO_CHEQUE_CAR = Pattern.compile("[0-9?-]");
    static final Pattern A_ORDEN_DE_CAR = Pattern.compile("[a-zA-Z ñÑáÁéÉíÍóÓúÚ']");
    static final Pattern RECIBO_MANUAL_CAR = Pattern.compile("[a-zA-Z0-9 ']");
    static final Pattern NRO_TALONARIO_CAR = Pattern.compile("[a-zA-Z0-9 ]");
    static final Pattern OBSERVACION_CAR = Pattern.compile("[a-zA-Z0-9 ñÑáÁéÉíÍóÓúÚ',;:.]");
    static final Pattern NRO_SAP_CAR = Pattern.compile("[a-zA-Z0-9 ñÑáÁéÉíÍóÓúÚ']");
    static final Pattern NOMBRE_BANCO_CAR = Pattern.compile("[a-zA-z0-9 '/]");

    public static String errorNombreBanco(String v) {
        return v != null && NOMBRE_BANCO.matcher(v).matches() ? null
                : MensajesCheque.formato("El nombre del banco", v, NOMBRE_BANCO_CAR,
                        "letras sin tilde, números, espacios, apóstrofe y «/» (sin guiones, puntos ni paréntesis)", 3, 50);
    }

    /** El numero de cheque. El largo NO se impone (ver {@link #NRO_CHEQUE}), asi que el mensaje no lo promete. */
    public static String errorNroCheque(String v) {
        return v != null && NRO_CHEQUE.matcher(v).matches() ? null
                : MensajesCheque.formato("El número de cheque", v, NRO_CHEQUE_CAR, "números y guion medio (-)", 0, Integer.MAX_VALUE);
    }

    public static String errorAOrdenDe(String v) {
        return v != null && A_ORDEN_DE.matcher(v).matches() ? null
                : MensajesCheque.formato("«A la orden de»", v, A_ORDEN_DE_CAR,
                        "letras (con tildes y ñ), espacios y apóstrofe; sin números, puntos ni otros símbolos", 0, 100);
    }

    public static String errorReciboManual(String v) {
        return v != null && RECIBO_MANUAL.matcher(v).matches() ? null
                : MensajesCheque.formato("El recibo manual", v, RECIBO_MANUAL_CAR, "letras, números, espacios y apóstrofe", 1, 25);
    }

    public static String errorNroTalonario(String v) {
        return v != null && NRO_TALONARIO.matcher(v).matches() ? null
                : MensajesCheque.formato("El talonario manual", v, NRO_TALONARIO_CAR,
                        "letras, números y espacios (sin guiones ni otros símbolos)", 1, 25);
    }

    /**
     * Observacion de una accion. <b>Vacia es valida</b>: en el JSF el campo tiene validador pero no
     * {@code required}, y JSF no corre los validadores sobre un valor vacio.
     */
    public static String errorObservacion(String v) {
        if (v == null || v.isEmpty()) return null;
        return OBSERVACION.matcher(v).matches() ? null
                : MensajesCheque.formato("La observación", v, OBSERVACION_CAR,
                        "letras (con tildes y ñ), números, espacios y los signos ' , ; : .", 2, 200);
    }

    /** Nro SAP: obligatorio solo cuando la accion cierra el cheque (lo decide quien llama). */
    public static String errorNroSap(String v) {
        return v != null && NRO_SAP.matcher(v).matches() ? null
                : MensajesCheque.formato("El número de SAP", v, NRO_SAP_CAR,
                        "letras (con tildes y ñ), números, espacios y apóstrofe", 2, 40);
    }

    // ------------------------------------------------------------------ cheque

    /**
     * {@code ChequeManagedBean.eliminarCerosIzq}: quita los ceros a la izquierda del Nro de cheque.
     * El legacy revienta (StringIndexOutOfBounds) con un valor de solo ceros; aqui devuelve el vacio
     * y quien llama lo rechaza.
     */
    public static String sinCerosIzquierda(String nroCheque) {
        if (nroCheque == null) return null;
        int i = 0;
        while (i < nroCheque.length() && nroCheque.charAt(i) == '0') i++;
        return nroCheque.substring(i);
    }

    /**
     * Coherencia entre quien entrego el cheque, el recibo manual y el talonario
     * ({@code saveCheque} / {@code saveChequeTal}). Son TRES reglas independientes y el legacy
     * acumula todos los mensajes; el cuarto control (que el talonario y el recibo existan juntos en
     * {@code tmto_talonario}) necesita la base y lo hace quien llama cuando
     * {@link #requiereValidarTalonario} es verdadero.
     *
     * @param codEmpleado  0 = lo dejo el cliente
     * @param reciboManual "0" = sin recibo manual (comparacion exacta, como {@code equals("0")})
     * @param nroTalonario "0" = sin talonario
     */
    public static List<String> erroresReciboTalonario(int codEmpleado, String reciboManual, String nroTalonario) {
        List<String> errores = new ArrayList<>();
        boolean reciboCero = "0".equals(reciboManual);
        boolean talonarioCero = "0".equals(nroTalonario);

        if (codEmpleado == 0 && !reciboCero) {
            errores.add(MensajesCheque.clienteConRecibo(reciboManual));
        }
        if (codEmpleado != 0 && reciboCero) {
            errores.add(MensajesCheque.empleadoSinRecibo());
        }
        if (!reciboCero && talonarioCero) {
            errores.add(MensajesCheque.reciboSinTalonario(reciboManual));
        }
        return errores;
    }

    /** Si hay que comprobar el par talonario/recibo contra {@code tmto_talonario} (rama 'M'). */
    public static boolean requiereValidarTalonario(String reciboManual, String nroTalonario) {
        return !"0".equals(nroTalonario) && !"0".equals(reciboManual);
    }

    // ------------------------------------------------------------------ fechas

    /** Dias de tolerancia de la fecha de cobro respecto de la fecha del cheque (legacy: -28 / +28). */
    public static final int DIAS_TOLERANCIA_COBRO = 28;

    /**
     * La fecha de cobro cae entre {@code fechaCheque - 28} y {@code fechaCheque + 28}, ambos
     * incluidos ({@code !before(min) && !after(max)} del legacy). Compara dias, no instantes.
     */
    public static boolean cobroEnRango(Date fechaCobrar, Date fechaCheque) {
        if (fechaCobrar == null || fechaCheque == null) return false;
        LocalDate cobro = dia(fechaCobrar);
        LocalDate cheque = dia(fechaCheque);
        return !cobro.isBefore(cheque.minusDays(DIAS_TOLERANCIA_COBRO))
                && !cobro.isAfter(cheque.plusDays(DIAS_TOLERANCIA_COBRO));
    }

    /** La fecha no es anterior a hoy (el calendario del legacy usa {@code mindate = fechaHoy}). */
    public static boolean noAnteriorA(Date fecha, LocalDate hoy) {
        return fecha != null && !dia(fecha).isBefore(hoy);
    }

    /** El dia de una fecha, en la zona del servidor (la misma con la que el driver arma los {@code date}). */
    public static LocalDate dia(Date d) {
        return new java.sql.Date(d.getTime()).toLocalDate();
    }

    // ------------------------------------------------------------------ acciones

    /** Estados que {@code saveAcciones} no deja guardar a mano: los crean otros flujos. */
    public static boolean esEstadoReservado(String estado) {
        return "REC".equals(estado) || "CUS".equals(estado) || "TRASP".equals(estado);
    }

    /**
     * El historial minimo para poder devolver o cerrar: el cheque tiene que haber salido de caja y
     * haberse dado en custodia, es decir 3 o mas acciones (REC + TRASP + CUS).
     * {@code saveAcciones}: "Verifique que el Cheque Haya salido de Caja y que se diese en Custodia".
     */
    public static final int ACCIONES_MINIMAS_PARA_CERRAR = 3;

    /** Prefijo de la observacion de una nueva fecha de cobro ({@code saveFechacobro}). */
    public static String observacionFechaCobro(LocalDate nuevaFechaCobro, String observacion) {
        String fecha = String.format("%02d/%02d/%04d",
                nuevaFechaCobro.getDayOfMonth(), nuevaFechaCobro.getMonthValue(), nuevaFechaCobro.getYear());
        return "Nueva Fecha de Cobro = " + fecha + " . " + (observacion == null ? "" : observacion);
    }
}
