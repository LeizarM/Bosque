package bo.bosque.com.impexpap.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * De donde sale la IP del que llama.
 *
 * <h3>El agujero que cierra</h3>
 * Tres filtros de seguridad ({@code SecurityFilter}, {@code RateLimitFilter},
 * {@code JwtEntryPoint}) resolvian la IP asi:
 *
 * <pre>
 * String xf = request.getHeader("X-Forwarded-For");
 * if (xf == null) return request.getRemoteAddr();
 * return xf.split(",")[0];
 * </pre>
 *
 * <p><b>Esa cabecera la escribe el cliente.</b> Sin un proxy de confianza
 * delante, mandar un valor distinto en cada request alcanzaba para:
 * <ul>
 *   <li>evadir el limite de 5 intentos por minuto de {@code /auth/login} — o
 *       sea, fuerza bruta sin techo;</li>
 *   <li>evadir el bloqueo progresivo de 1/5/30 minutos de
 *       {@code JwtEntryPoint};</li>
 *   <li>hacer crecer sin limite los mapas indexados por IP, porque la clave la
 *       elegia el atacante.</li>
 * </ul>
 *
 * <h3>La regla</h3>
 * {@code X-Forwarded-For} se cree <b>solo</b> si la conexion viene de una IP que
 * esta en {@code security.proxies-confiables}. En cualquier otro caso vale
 * {@code getRemoteAddr()}, que es el unico dato que el cliente no puede
 * falsificar porque es el otro extremo del socket TCP.
 *
 * <p>La lista va <b>vacia por defecto</b>, y eso es lo correcto mientras el
 * backend atienda directo en el 9223. El dia que entre un nginx adelante hay que
 * poner su IP ahi; si no, todos los clientes se veran como la IP del proxy y el
 * rate limit pasaria a ser global en vez de por cliente. Los dos errores se
 * anuncian en el log al arrancar, porque ninguno de los dos falla ruidoso.
 */
@Component
public class ClienteIp {

    private static final Logger log = LoggerFactory.getLogger(ClienteIp.class);

    private final Set<String> proxiesConfiables;

    public ClienteIp(@Value("${security.proxies-confiables:}") String lista) {
        this.proxiesConfiables = parsear(lista);
    }

    private static Set<String> parsear(String lista) {
        if (lista == null || lista.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> ips = new HashSet<>();
        for (String parte : lista.split(",")) {
            String ip = parte.trim();
            if (!ip.isEmpty()) {
                ips.add(ip);
            }
        }
        return Collections.unmodifiableSet(ips);
    }

    @PostConstruct
    void anunciarModo() {
        if (proxiesConfiables.isEmpty()) {
            log.info("Resolucion de IP: se IGNORA X-Forwarded-For y se usa la IP real de la "
                   + "conexion. Correcto si el backend atiende directo. Si hay un proxy "
                   + "adelante, cargar su IP en security.proxies-confiables o todos los "
                   + "clientes compartiran el mismo cupo de rate limit.");
        } else {
            log.info("Resolucion de IP: se acepta X-Forwarded-For solo desde {}.", proxiesConfiables);
        }
    }

    /**
     * La IP del cliente.
     *
     * @return nunca {@code null}; si no se puede resolver nada, {@code "desconocida"},
     *         que como clave de mapa agrupa a todos los raros en un solo cupo
     */
    public String de(HttpServletRequest request) {
        String remota = request.getRemoteAddr();
        if (remota == null || remota.trim().isEmpty()) {
            return "desconocida";
        }
        if (!proxiesConfiables.contains(remota)) {
            return remota;          // conexion directa: la cabecera no se mira
        }
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada == null || reenviada.trim().isEmpty()) {
            return remota;
        }
        // El primero de la lista es el cliente original; el resto son saltos.
        String primera = reenviada.split(",")[0].trim();
        return primera.isEmpty() ? remota : primera;
    }

    /** Si hay algun proxy declarado. Solo para diagnostico. */
    public boolean hayProxiesConfiables() {
        return !proxiesConfiables.isEmpty();
    }

    /** Copia de la lista configurada. Solo para diagnostico y tests. */
    public Set<String> getProxiesConfiables() {
        return new HashSet<>(Arrays.asList(proxiesConfiables.toArray(new String[0])));
    }
}
