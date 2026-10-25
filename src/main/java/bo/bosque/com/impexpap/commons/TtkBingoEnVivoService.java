package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.ITtkBingo;
import bo.bosque.com.impexpap.dao.ITtkBingoAviso;
import bo.bosque.com.impexpap.dao.ITtkBingoCarton;
import bo.bosque.com.impexpap.dao.ITtkBingoSorteo;
import bo.bosque.com.impexpap.dto.TtkBingoPublico;
import bo.bosque.com.impexpap.dto.TtkEstadoEncargado;
import bo.bosque.com.impexpap.dto.TtkEstadoPublico;
import bo.bosque.com.impexpap.dto.TtkRankingCarton;
import bo.bosque.com.impexpap.model.TtkBingoAviso;
import bo.bosque.com.impexpap.model.TtkBingoSorteo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Estado en vivo de cada bingo en memoria (sin Redis): se calcula una vez por cambio con los SP de
 * lectura y se reparte por STOMP a todos los espectadores. Ningún espectador dispara consultas a la base:
 * los endpoints de estado leen de aquí y solo la primera lectura de un bingo lo carga.
 */
@Service
public class TtkBingoEnVivoService {

    private static final Logger log = LoggerFactory.getLogger(TtkBingoEnVivoService.class);

    private final ITtkBingo bingos;
    private final ITtkBingoSorteo sorteos;
    private final ITtkBingoCarton cartones;
    private final ITtkBingoAviso avisos;
    private final TiempoRealPublicador publicador;

    private final ConcurrentHashMap<Long, TtkEstadoEncargado> encargados = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, TtkEstadoPublico> publicos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Object> candados = new ConcurrentHashMap<>();

    /** Arranca en la hora actual: tras un reinicio la versión sigue siendo mayor que la que tienen los clientes. */
    private final AtomicLong versiones = new AtomicLong(System.currentTimeMillis());

    public TtkBingoEnVivoService(ITtkBingo bingos, ITtkBingoSorteo sorteos, ITtkBingoCarton cartones,
                                 ITtkBingoAviso avisos, TiempoRealPublicador publicador) {
        this.bingos = bingos;
        this.sorteos = sorteos;
        this.cartones = cartones;
        this.avisos = avisos;
        this.publicador = publicador;
    }

    /**
     * Recalcula el bingo tras una mutación (P, T, R y avisos una sola vez), lo guarda y lo publica en ambos tópicos.
     * El candado del bingo ordena los refrescos: la versión y el envío salen en el mismo orden.
     * Si falla, se descarta la caché (la próxima lectura recarga) y la mutación ya hecha no se revierte.
     */
    public TtkEstadoEncargado refrescar(long codBingo, String aviso) {
        synchronized (candado(codBingo)) {
            TtkEstadoEncargado estado;
            try {
                estado = cargar(codBingo, aviso);
            } catch (RuntimeException ex) {
                log.error("Bingo {}: no se pudo recalcular el estado en vivo; se descarta su caché", codBingo, ex);
                olvidar(codBingo);
                return null;
            }
            if (estado == null) {
                olvidar(codBingo);
                return null;
            }
            publicar(codBingo, estado, publicos.get(codBingo));
            return estado;
        }
    }

    /** Estado para los participantes, desde la caché; la primera vez lo carga un solo hilo. Null si no existe. */
    public TtkEstadoPublico obtenerPublico(long codBingo) {
        TtkEstadoPublico p = publicos.get(codBingo);
        if (p != null) return p;
        synchronized (candado(codBingo)) {
            p = publicos.get(codBingo);
            if (p == null && cargar(codBingo, null) != null) p = publicos.get(codBingo);
            return p;
        }
    }

    /** Estado para el panel del encargado, desde la caché; la primera vez lo carga un solo hilo. Null si no existe. */
    public TtkEstadoEncargado obtenerEncargado(long codBingo) {
        TtkEstadoEncargado e = encargados.get(codBingo);
        if (e != null) return e;
        synchronized (candado(codBingo)) {
            e = encargados.get(codBingo);
            return e != null ? e : cargar(codBingo, null);
        }
    }

    /** Lee los cuatro SP, arma ambos estados con la misma versión y los deja en la caché. Llamar con el candado. */
    private TtkEstadoEncargado cargar(long codBingo, String aviso) {
        TtkBingoPublico publico = bingos.publico(codBingo);
        if (publico == null) return null;
        List<TtkBingoSorteo> tablero = inmutable(sorteos.tablero(codBingo));
        List<TtkRankingCarton> ranking = inmutable(cartones.ranking(codBingo));
        List<TtkBingoAviso> pendientes = inmutable(avisos.pendientes(codBingo));
        long version = versiones.incrementAndGet();

        TtkEstadoEncargado encargado = new TtkEstadoEncargado(version, publico, tablero, ranking, aviso, pendientes);
        encargados.put(codBingo, encargado);
        publicos.put(codBingo, new TtkEstadoPublico(version, publico, tablero));
        return encargado;
    }

    /** Un fallo al enviar (lo registra el publicador) no invalida el estado ya guardado: se recupera por POST. */
    private void publicar(long codBingo, TtkEstadoEncargado encargado, TtkEstadoPublico publico) {
        publicador.publicar(TtkBingoReglaSuscripcion.topicoEncargado(codBingo), encargado);
        publicador.publicar(TtkBingoReglaSuscripcion.topicoPublico(codBingo), publico);
    }

    private void olvidar(long codBingo) {
        encargados.remove(codBingo);
        publicos.remove(codBingo);
    }

    private Object candado(long codBingo) {
        return candados.computeIfAbsent(codBingo, k -> new Object());
    }

    private static <T> List<T> inmutable(List<T> filas) {
        return filas == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(filas));
    }
}
