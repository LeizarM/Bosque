package bo.bosque.com.impexpap.commons;

import java.util.regex.Pattern;

/**
 * Las reglas de formato de Verificar Cheques (vista 77) que el legacy aplicaba en
 * {@code VerificacionDepositoManagedBean}, sin base de datos y sin Spring.
 *
 * <p><b>Paridad con el legacy (decision 2 de CLAUDE.md).</b> La expresion regular es la del validador JSF, copiada
 * literalmente; el unico cambio es que aqui se evalua con {@code matches()} en el servidor y no solo en el navegador.
 * Los metodos devuelven el mensaje de error o {@code null} si la regla se cumple.
 */
public final class ReglasVerificacion {

    private ReglasVerificacion() {
    }

    /** Largo maximo de la observacion: el {@code maxlength="50"} del campo y el {@code {0,50}} del validador. */
    public static final int MAX_OBSERVACION = 50;

    /**
     * {@code VerificacionDepositoManagedBean.validarObservacion} (linea 219): el literal del legacy es
     * {@code "^[a-zA-Z0-9  ' '',''.' ]{0,50}"}. Los apostrofes de adorno no son parte de nada: la clase de
     * caracteres admite letras SIN tilde ni ene, digitos, espacio, apostrofe, coma y punto, de 0 a 50 (no hay ningun
     * guion en ella: no es un rango). Con {@code matches()}, como el legacy. No admite saltos de linea ni
     * {@code ; : -}. El mensaje del propio legacy dice "Letras, Numeros y espacios", pero la regla deja pasar
     * ademas el apostrofe, la coma y el punto: se conserva lo que hace el codigo.
     */
    static final Pattern OBSERVACION = Pattern.compile("^[a-zA-Z0-9  ' '',''.' ]{0,50}");

    /**
     * Un caracter permitido. SOLO sirve para que el mensaje diga cuales caracteres sobran: quien decide si el valor
     * es valido sigue siendo el patron completo ({@code ReglasVerificacionTest} comprueba que coinciden).
     */
    static final Pattern OBSERVACION_CAR = Pattern.compile("[a-zA-Z0-9 ',.]");

    /** El marcador con el que el mensaje nombra un salto de linea (un salto real partiria el mensaje en dos lineas). */
    static final String SALTO = "¶";

    /**
     * La observacion de la verificacion. <b>Vacia es valida</b> (en el JSF el campo tiene validador pero no
     * {@code required}, y JSF no corre los validadores sobre un valor vacio). Mas de 50 caracteres no lo es.
     */
    public static String errorObservacion(String v) {
        if (v == null || v.isEmpty()) return null;
        if (OBSERVACION.matcher(v).matches()) return null;
        // Los saltos de linea se muestran como ¶ para que el mensaje siga siendo una sola linea; ¶ no esta entre los
        // caracteres permitidos, asi que se nombra como sobrante igual que lo que representa.
        String visible = v.replace("\r\n", SALTO).replace("\n", SALTO).replace("\r", SALTO);
        return MensajesCheque.formato("El texto de la observación", visible, OBSERVACION_CAR,
                "letras sin tilde ni ñ, números, espacios y los signos ' , .", 0, MAX_OBSERVACION);
    }

    /**
     * El cheque esta CERRADO. El JSF lo decidia con {@code item.datoEstadoCheque != 'CERRADO'}, comparando la
     * <i>descripcion</i> del estado ({@code v_tipos} grupo 23: {@code CER} = "CERRADO"); se hace la misma comparacion
     * (sin distinguir mayusculas).
     */
    public static boolean esCerrado(String datoEstadoCheque) {
        return datoEstadoCheque != null && "CERRADO".equalsIgnoreCase(datoEstadoCheque.trim());
    }
}
