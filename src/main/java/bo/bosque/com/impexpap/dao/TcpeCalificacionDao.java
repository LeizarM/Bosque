package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class TcpeCalificacionDao implements ITcpeCalificacion {

    static final String SP_ABM = "p_abm_tcpe_Calificacion";

    private final SpHelper spHelper;

    public TcpeCalificacionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp marcar(long codExamen, Long codRespuestaH, Boolean esCorrecta, long audUsuario) {
        Map<String, Object> p = examen(codExamen);
        TcpeParametros.poner(p, "codRespuestaH", codRespuestaH);
        TcpeParametros.poner(p, "esCorrecta", esCorrecta);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "P");
    }

    @Override
    public RespuestaSp cerrar(long codExamen, long audUsuario) {
        Map<String, Object> p = examen(codExamen);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "C");
    }

    @Override
    public RespuestaSp nota(long codExamen, Double resultado, long audUsuario) {
        Map<String, Object> p = examen(codExamen);
        TcpeParametros.poner(p, "resultado", resultado);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "N");
    }

    private static Map<String, Object> examen(long codExamen) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codExamen", codExamen);
        return p;
    }
}
