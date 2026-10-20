package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpePersonaExamenDto;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class TcpeGenerarExamenDao implements ITcpeGenerarExamen {

    static final String SP_ABM = "p_abm_tcpe_GenerarExamen";

    private final SpHelper spHelper;

    public TcpeGenerarExamenDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public long generar(long codRolExamen, Long codEmpleado, String password2, long audUsuario) {
        Map<String, Object> p = base(codRolExamen, password2, audUsuario);
        TcpeParametros.poner(p, "codEmpleado", TcpeParametros.id(codEmpleado));
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I").getIdGenerado();
    }

    @Override
    public long generarParaPersona(long codRolExamen, TcpePersonaExamenDto persona, String password2, long audUsuario) {
        Map<String, Object> p = base(codRolExamen, password2, audUsuario);
        TcpeParametros.poner(p, "ci", recortar(persona.getCi()));
        TcpeParametros.poner(p, "nombres", recortar(persona.getNombres()));
        TcpeParametros.poner(p, "apellidoPaterno", recortar(persona.getApellidoPaterno()));
        TcpeParametros.poner(p, "apellidoMaterno", recortar(persona.getApellidoMaterno()));
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I").getIdGenerado();
    }

    private static Map<String, Object> base(long codRolExamen, String password2, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codRolExamen", codRolExamen);
        TcpeParametros.poner(p, "password2", password2);
        p.put("audUsuario", audUsuario);
        return p;
    }

    private static String recortar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
