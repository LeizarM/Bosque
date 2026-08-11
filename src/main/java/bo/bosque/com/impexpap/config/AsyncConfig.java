package bo.bosque.com.impexpap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Habilita {@code @Async} en la aplicación y define el pool de hilos
 * <b>{@code notificacionesExecutor}</b>, donde corren los avisos de WhatsApp de entregas.
 *
 * <p><b>Por qué esta clase existe.</b> Hasta ahora el proyecto no tenía
 * {@code @EnableAsync} en ningún lado (solo {@code @EnableScheduling} en
 * {@code BosqueApplication}). Sin esta anotación, {@code @Async} es decorativo: el método
 * corre SÍNCRONO en el hilo que lo llama — o sea, en el hilo del request del chofer que
 * está registrando la entrega, que es exactamente lo que el feature no puede permitirse.
 *
 * <p><b>Por qué un pool propio y no el default.</b> El executor por defecto de Spring
 * ({@code SimpleAsyncTaskExecutor}) crea un hilo NUEVO por cada tarea, sin límite: con
 * openWA caído y muchas entregas seguidas, eso es una fábrica de hilos colgados. Acá el
 * pool está acotado a mano (2 hilos base, 4 en pico, cola de 500) y, cuando se llena,
 * DESCARTA el aviso.
 *
 * <p><b>Descartar es la decisión de diseño, no un descuido.</b> La política de rechazo NO
 * es {@code CallerRunsPolicy}: esa haría que, justo cuando el sistema está saturado, el
 * envío HTTP a openWA se ejecute en el hilo del request del chofer — el peor momento
 * posible. Tampoco es {@code AbortPolicy} a secas, porque
 * {@code TaskRejectedException} viajaría hasta el llamador. Se usa
 * {@link DescartarYLoguearPolicy}: se tira el aviso y se deja un WARN. Perder un mensaje
 * de WhatsApp es molesto; frenar al chofer que está en la calle con mala señal es un
 * incidente.
 *
 * <p><b>Cómo se dimensionó.</b> Cada tarea es un POST HTTP corto por entrega registrada.
 * Con un volumen diario de decenas de entregas, 2 hilos alcanzan de sobra y 500 de cola
 * es un colchón enorme; el pool está chico a propósito para que jamás compita por CPU ni
 * por conexiones con el tráfico del API.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    /** Nombre del bean. Tiene que coincidir EXACTO con el {@code @Async("...")} del servicio. */
    public static final String NOTIFICACIONES_EXECUTOR = "notificacionesExecutor";

    /**
     * Pool de las notificaciones de entregas.
     *
     * <p>El nombre del método ES el nombre del bean, y {@code @Async("notificacionesExecutor")}
     * lo busca por nombre EN TIEMPO DE INVOCACIÓN: si alguien renombra este método, el
     * error no aparece al arrancar la aplicación sino recién cuando se registra la primera
     * entrega, y explota en el hilo del request. No lo renombres.
     */
    @Bean(name = NOTIFICACIONES_EXECUTOR)
    public ThreadPoolTaskExecutor notificacionesExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notif-");
        executor.setRejectedExecutionHandler(new DescartarYLoguearPolicy());
        // Al apagar la aplicación se intenta terminar lo que ya está en vuelo, pero con
        // techo: si openWA no contesta, el shutdown no se queda esperando para siempre.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        // NO se llama a executor.initialize() a mano: ThreadPoolTaskExecutor implementa
        // InitializingBean, así que Spring ya lo inicializa vía afterPropertiesSet(). Y
        // ExecutorConfigurationSupport.initialize() no es idempotente — reasigna el executor
        // interno, de modo que llamarlo dos veces deja un ThreadPoolExecutor huérfano al que
        // nunca se le hace shutdown y que no aparece en las métricas del WARN de rechazo.
        logger.info("Pool '{}' listo (core=2, max=4, cola=500, prefijo='notif-', rechazo=descartar)",
                    NOTIFICACIONES_EXECUTOR);
        return executor;
    }

    /**
     * Red de contención final: si algún {@code @Async} que devuelve {@code void} llegara a
     * dejar escapar una excepción, se loguea acá en vez de morir en un stacktrace suelto
     * del pool.
     *
     * <p>{@code NotificacionEntregaService} ya atrapa {@code Throwable} adentro de cada
     * método, así que esto no debería dispararse nunca. Si aparece en los logs, hay un
     * agujero en ese try/catch.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                logger.error("Excepción no atrapada en método @Async {}.{} con parámetros {}: {}",
                             method.getDeclaringClass().getSimpleName(), method.getName(),
                             Arrays.toString(params), ex.getMessage(), ex);
    }

    /**
     * Executor para los {@code @Async} <b>sin</b> qualifier. Existe para que el pool de
     * notificaciones quede de verdad reservado.
     *
     * <p><b>Por qué no alcanza con no sobreescribir esto.</b> Devolver {@code null} (el
     * default de {@link AsyncConfigurer}) no manda los {@code @Async} anónimos al executor
     * de Spring: manda a {@code AsyncExecutionAspectSupport.getDefaultExecutor()} a hacer
     * {@code beanFactory.getBean(TaskExecutor.class)}, y como {@code notificacionesExecutor}
     * es el ÚNICO {@code TaskExecutor} de la aplicación, todos caerían justamente acá.
     * Peor: declarar un bean {@code Executor} hace que {@code TaskExecutionAutoConfiguration}
     * se retire ({@code @ConditionalOnMissingBean(Executor.class)}), así que
     * {@code applicationTaskExecutor} ni siquiera existe como alternativa.
     *
     * <p>El escenario concreto que esto evita: alguien agrega mañana un
     * {@code @Async public void enviarCorreo(...)} en cualquier bean; con openWA colgado
     * (3 s + 5 s de timeout por envío, 2 hilos) la cola se llena de avisos de WhatsApp y
     * {@link DescartarYLoguearPolicy} descarta el correo en silencio, dejando un WARN que
     * habla de "notificaciones" y no menciona el correo por ningún lado.
     */
    @Override
    public java.util.concurrent.Executor getAsyncExecutor() {
        return new org.springframework.core.task.SimpleAsyncTaskExecutor("async-");
    }

    /**
     * Política de rechazo: descarta la tarea y deja constancia.
     *
     * <p>Equivale a {@code ThreadPoolExecutor.DiscardPolicy}, pero esa tira la tarea en
     * absoluto silencio y entonces nadie se entera nunca de que se perdieron avisos. Acá
     * se loguea un WARN con el estado del pool, que es el dato que sirve para decidir si
     * hay que agrandarlo o si openWA está colgado.
     */
    static class DescartarYLoguearPolicy implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (executor.isShutdown()) {
                logger.warn("Notificación descartada: el pool '{}' está apagándose.",
                            NOTIFICACIONES_EXECUTOR);
                return;
            }
            logger.warn("Notificación DESCARTADA por saturación del pool '{}' "
                      + "(activos={}, en cola={}, completadas={}). Se prefiere perder el aviso "
                      + "antes que frenar el registro de la entrega. Revisar si openWA responde.",
                        NOTIFICACIONES_EXECUTOR, executor.getActiveCount(),
                        executor.getQueue().size(), executor.getCompletedTaskCount());
        }
    }
}
