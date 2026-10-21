package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.model.TcpeRsptaSemana;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code tcpe_rsptaSemana} vía {@code p_list_tcpe_RsptaSemana} y {@code p_abm_tcpe_RsptaSemana}. El alta genérica
 * se rechaza: cualquiera con el catálogo podría responder por otro; se responde solo con el usuario del token.
 */
@Repository
public class TcpeRsptaSemanaDao extends TcpeCrudDao<TcpeRsptaSemana> implements ITcpeRsptaSemana {

    static final String SP_ABM = "p_abm_tcpe_RsptaSemana";

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codRsptaSemana", "codPreguntaSemana", "codUsuario" };

    /** Columnas de datos de p_abm, sin PK, audUsuario ni audFecha. */
    private static final String[] COLUMNAS = { "codPreguntaSemana", "codUsuario", "respuesta" };

    private final SpHelper sp;

    public TcpeRsptaSemanaDao(SpHelper spHelper) {
        super(spHelper, TcpeRsptaSemana.class, "rsptaSemana", "codRsptaSemana", true, FILTROS, COLUMNAS);
        this.sp = spHelper;
    }

    @Override
    public RespuestaSp guardar(TcpeRsptaSemana modelo, long audUsuario) {
        throw new SpBusinessException("Las respuestas a una pregunta de la semana se registran desde el inicio, "
                + "con el usuario que responde.");
    }

    @Override
    public RespuestaSp responder(Long codPreguntaSemana, String respuesta, long codUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.poner(p, "codPreguntaSemana", TcpeParametros.id(codPreguntaSemana));
        p.put("codUsuario", codUsuario);
        TcpeParametros.poner(p, "respuesta", respuesta);
        p.put("audUsuario", codUsuario);
        return sp.ejecutarAbmMap(SP_ABM, p, "I");
    }
}
