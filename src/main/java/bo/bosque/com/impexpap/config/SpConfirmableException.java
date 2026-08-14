package bo.bosque.com.impexpap.config;

/**
 * Un <b>400 que el usuario puede confirmar</b>: lo que se hizo es inusual pero legítimo, y
 * reintentar con {@code confirmado = true} lo deja pasar.
 *
 * <p>Se traduce en un 400 igual que {@link SpBusinessException} —de la que hereda, así que todo lo
 * que ya la atrapaba la sigue atrapando— pero {@code GlobalExceptionHandler} le agrega
 * {@code "confirmable": true} al cuerpo.
 *
 * <h3>Por qué hacía falta un tipo aparte</h3>
 * Los tres estados que puede devolver una escritura de este módulo son distintos y el cliente
 * <b>no puede distinguirlos por el texto del mensaje</b>:
 * <ul>
 *   <li><b>400 sin marcador</b> — regla de negocio rota (motivo vacío, fecha que no es
 *       aniversario). No hay nada que confirmar: se corrige el formulario.</li>
 *   <li><b>400 con {@code confirmable: true}</b> — esto. La pantalla ofrece "Confirmar" y
 *       reintenta el MISMO cuerpo con {@code confirmado: true}.</li>
 *   <li><b>409</b> ({@link SpConflictException}) — doble toque o carrera. <b>Reintentar no lo
 *       arregla</b>: se muestra el mensaje y se recarga la grilla, sin botón de reintento.</li>
 * </ul>
 * Adivinar cuál es cuál buscando la palabra "confirme" en el mensaje es exactamente lo que este
 * tipo viene a evitar: el día que alguien reescriba un mensaje, la pantalla dejaría de ofrecer el
 * botón sin que se caiga ningún test.
 */
public class SpConfirmableException extends SpBusinessException {

    public SpConfirmableException(String message) {
        super(message);
    }
}
