package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ITcpeCrud;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Registro de los 38 DAO {@code tcpe_} por el nombre de su entidad ({@code area}, {@code rsptaOpcionH}...), para
 * el CRUD genérico {@code /examenes/catalogo/{entidad}/...}. Spring inyecta todos los {@link ITcpeCrud}; si falta
 * uno, el arranque no falla pero {@link #de} responde que la entidad no existe.
 */
@Component
public class TcpeCatalogo {

    private final Map<String, ITcpeCrud<?>> porEntidad;

    public TcpeCatalogo(List<ITcpeCrud<?>> daos) {
        Map<String, ITcpeCrud<?>> m = new TreeMap<>();
        for (ITcpeCrud<?> d : daos) {
            if (m.put(d.entidad(), d) != null) {
                throw new IllegalStateException("Hay dos DAO para la entidad tcpe_" + d.entidad());
            }
        }
        this.porEntidad = Collections.unmodifiableMap(m);
    }

    /** El DAO de la entidad, o 400 si no existe. */
    public ITcpeCrud<?> de(String entidad) {
        ITcpeCrud<?> dao = entidad == null ? null : porEntidad.get(entidad);
        if (dao == null) {
            throw new SpBusinessException("La entidad '" + entidad + "' no existe en el catálogo de exámenes.");
        }
        return dao;
    }

    /** Los DAO registrados por nombre de entidad, en orden alfabético (solo lectura). */
    public Map<String, ITcpeCrud<?>> todos() {
        return porEntidad;
    }
}
