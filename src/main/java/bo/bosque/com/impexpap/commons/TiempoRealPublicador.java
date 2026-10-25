package bo.bosque.com.impexpap.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica por STOMP a todos los suscriptos de un tópico (/topic/...), para cualquier módulo.
 * Un fallo al enviar queda en el log y no corta a quien publica: los clientes recuperan el estado por POST.
 */
@Component
public class TiempoRealPublicador {

    private static final Logger log = LoggerFactory.getLogger(TiempoRealPublicador.class);

    private final SimpMessagingTemplate plantilla;

    public TiempoRealPublicador(SimpMessagingTemplate plantilla) {
        this.plantilla = plantilla;
    }

    /** Envía la carga como JSON; devuelve false si no se pudo. */
    public boolean publicar(String topico, Object carga) {
        try {
            plantilla.convertAndSend(topico, carga);
            return true;
        } catch (RuntimeException ex) {
            log.warn("No se pudo publicar en {}", topico, ex);
            return false;
        }
    }
}
