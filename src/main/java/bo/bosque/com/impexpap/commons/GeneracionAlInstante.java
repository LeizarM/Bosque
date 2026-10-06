package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.IGeneradorTareasRutinarias;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Genera las ocurrencias del día apenas se asigna una tarea, sin esperar al
 * Job de las 00:05.
 *
 * <p>Marcelo, 2026-10-05: "le acabo de asignar justo ahora otra tarea pero no
 * aparece, quiero que eso también sea al instante no esperar al job".
 *
 * <h3>Por qué el generador entero y no "solo esa tarea"</h3>
 * {@code p_generar_tac_bitTareaRuti} ya sabe todo lo que decide si una
 * ocurrencia nace hoy: la frecuencia, la vigencia de la asignación, el cargo
 * vigente de cada persona, los permisos, los domingos (archivo SQL 67). Una
 * versión "solo para esta tarea" tendría que copiar esa aritmética de fechas,
 * y una copia se desincroniza de la original en la primera modificación
 * (archivo SQL 54). El generador es idempotente y no duplica: correrlo de más
 * no cuesta nada más que su tiempo.
 *
 * <h3>Por qué en segundo plano, y agrupado</h3>
 * Hay pantallas que mandan un pedido por cargo o por tarea, en fila ("Crear
 * tarea" para varios cargos, "Elegir del catálogo"). Si cada pedido esperara
 * una corrida completa antes de responder, agregar cinco tareas serían cinco
 * corridas seguidas con la pantalla bloqueada.
 *
 * <p>Así que el pedido responde enseguida y la corrida arranca en este hilo.
 * Como mucho queda UNA encolada detrás de la que está trabajando: lo que se
 * asigne mientras tanto la encola una vez, y esa segunda corrida ve todo lo
 * que llegó en el medio. Cinco asignaciones seguidas son dos corridas, no
 * cinco.
 *
 * <p>Si una corrida falla —la base no responde, otra corrida tenía el bloqueo
 * del SP (error 98)— no se reintenta aquí: la asignación ya quedó guardada y
 * el Job de las 00:05 la recoge igual. Solo se deja en el log.
 */
@Slf4j
@Service
public class GeneracionAlInstante {

    private final IGeneradorTareasRutinarias generador;

    /** Un solo hilo: dos corridas de aquí nunca se pisan (el SP además tiene su applock). */
    private final ExecutorService hilo = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "generar-tareas-al-instante");
        t.setDaemon(true);
        return t;
    });

    /** Hay una corrida encolada que todavía no empezó. */
    private final AtomicBoolean encolada = new AtomicBoolean(false);

    public GeneracionAlInstante(IGeneradorTareasRutinarias generador) {
        this.generador = generador;
    }

    /**
     * Pide una corrida y vuelve enseguida.
     *
     * @param motivo qué escritura la pidió; solo para el log
     */
    public void pedir(String motivo) {
        if (!encolada.compareAndSet(false, true)) {
            log.debug("Generación al instante ({}): ya hay una encolada que lo va a ver.", motivo);
            return;
        }
        hilo.execute(() -> {
            // La marca se baja ANTES de correr, no después: lo que se asigne
            // mientras esta corrida trabaja tiene que encolar otra, porque esta
            // ya leyó las asignaciones y no lo va a ver.
            encolada.set(false);
            try {
                RespuestaSp res = generador.generarOcurrencias();
                log.info("Generación al instante ({}): {}", motivo, res.getErrormsg());
            } catch (Exception e) {
                log.warn("Generación al instante ({}) falló; la recoge el Job de las 00:05: {}",
                        motivo, e.getMessage());
            }
        });
    }

    @PreDestroy
    void apagar() {
        hilo.shutdown();
    }
}
