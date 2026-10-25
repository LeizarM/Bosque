package bo.bosque.com.impexpap.security;

import org.springframework.security.core.Authentication;

/**
 * Regla de suscripción STOMP que aporta cada módulo como bean: dice qué tópicos son suyos y quién los
 * puede escuchar. StompSeguridad rechaza los destinos que no cubre ninguna regla.
 */
public interface ReglaSuscripcion {

    /** Si el destino pertenece a este módulo (por ejemplo, empieza con /topic/bingo/). */
    boolean aplica(String destino);

    /** Si el usuario del CONNECT puede escuchar ese destino; se consulta solo cuando aplica. */
    boolean permite(Authentication auth, String destino);
}
