package bo.bosque.com.impexpap.config;

import bo.bosque.com.impexpap.security.StompSeguridad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Tiempo real para cualquier módulo: STOMP sobre WebSocket nativo (sin SockJS) y broker simple en memoria,
 * sin Redis. El handshake queda abierto en MainSecurity; el JWT se valida en el CONNECT (StompSeguridad) y
 * cada módulo autoriza sus tópicos con un bean ReglaSuscripcion. Para publicar: TiempoRealPublicador.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Endpoint STOMP general. */
    public static final String ENDPOINT = "/ws";
    /** Alias del primer cliente (bingo): mismo broker y mismas reglas. */
    public static final String ENDPOINT_BINGO = "/ws-bingo";
    /** Prefijo de los tópicos que difunde el broker. */
    public static final String TOPICOS = "/topic";

    /** Latido STOMP (ms, enviar y esperar): detecta conexiones caídas de celulares que cambian de red. */
    private static final long[] LATIDO = { 10000, 10000 };

    private final StompSeguridad seguridad;
    private final String[] origenes;
    private TaskScheduler latidos;

    /** Orígenes permitidos en el handshake; si no se define tiemporeal.origenes, los del módulo ttk (o cualquiera). */
    public WebSocketConfig(StompSeguridad seguridad,
                           @Value("${tiemporeal.origenes:${ttk.cors.origenes:*}}") String origenes) {
        this.seguridad = seguridad;
        this.origenes = origenes.trim().split("\\s*,\\s*");
    }

    /** Planificador propio del broker para los latidos; perezoso porque lo crea esta misma configuración. */
    @Autowired
    public void setLatidos(@Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler latidos) {
        this.latidos = latidos;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ENDPOINT, ENDPOINT_BINGO).setAllowedOriginPatterns(origenes);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(TOPICOS).setHeartbeatValue(LATIDO).setTaskScheduler(latidos);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(seguridad);
    }

    /**
     * Con el broker existe otro TaskScheduler y Spring Boot deja de crear el de los @Scheduled, que pasarían
     * a correr en el pool del broker. Se declara el mismo que Boot creaba, con el nombre que ellos buscan.
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler(TaskSchedulerBuilder builder) {
        return builder.build();
    }
}
