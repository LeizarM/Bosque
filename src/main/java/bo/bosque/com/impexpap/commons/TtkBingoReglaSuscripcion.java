package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.security.ReglaSuscripcion;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tópicos del bingo en vivo y quién los escucha: /topic/bingo/{codBingo}/encargado solo con la pantalla
 * Panel; /topic/bingo/{codBingo}/publico con cualquiera de las cuatro pantallas del bingo.
 */
@Component
public class TtkBingoReglaSuscripcion implements ReglaSuscripcion {

    static final String PREFIJO = "/topic/bingo/";

    /** Únicos destinos válidos dentro del prefijo; cualquier otro se rechaza sin consultar el menú. */
    private static final Pattern DESTINO = Pattern.compile("^/topic/bingo/(\\d{1,18})/(publico|encargado)$");

    private final AccesoPantalla pantallas;

    public TtkBingoReglaSuscripcion(AccesoPantalla pantallas) {
        this.pantallas = pantallas;
    }

    public static String topicoPublico(long codBingo) {
        return PREFIJO + codBingo + "/publico";
    }

    public static String topicoEncargado(long codBingo) {
        return PREFIJO + codBingo + "/encargado";
    }

    @Override
    public boolean aplica(String destino) {
        return destino != null && destino.startsWith(PREFIJO);
    }

    @Override
    public boolean permite(Authentication auth, String destino) {
        Matcher m = DESTINO.matcher(destino == null ? "" : destino);
        if (!m.matches()) return false;
        return "encargado".equals(m.group(2))
                ? pantallas.tiene(auth, TtkPantallas.PANEL)
                : pantallas.tieneAlguna(auth, TtkPantallas.publico());
    }
}
