package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeAccesoDto;
import bo.bosque.com.impexpap.dto.TcpeRegistroPostulanteDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TcpeAccesoDao implements ITcpeAcceso {

    static final String SP_LIST = "p_list_tcpe_Acceso";
    static final String SP_ABM = "p_abm_tcpe_Acceso";

    private final SpHelper spHelper;

    public TcpeAccesoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public TcpeAccesoDto porLogin(String login) {
        if (login == null || login.trim().isEmpty()) return null;
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("login", login.trim());
        List<TcpeAccesoDto> filas = spHelper.ejecutarListado(SP_LIST, p, "L", TcpeAccesoDto.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public Long codUsuarioDeEmpleado(long codEmpleado) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codEmpleado", codEmpleado);
        List<TcpeAccesoDto> filas = spHelper.ejecutarListado(SP_LIST, p, "E", TcpeAccesoDto.class);
        return filas.isEmpty() ? null : TcpeParametros.id(filas.get(0).getCodUsuario());
    }

    @Override
    public List<TcpeAccesoDto> sinHash() {
        return spHelper.ejecutarListado(SP_LIST, new LinkedHashMap<>(), "M", TcpeAccesoDto.class);
    }

    @Override
    public RespuestaSp guardarHash(long codUsuario, String hash, Long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codUsuario", codUsuario);
        p.put("password2", hash);
        TcpeParametros.poner(p, "audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "P");
    }

    @Override
    public RespuestaSp registrar(long codUsuario, TcpeRegistroPostulanteDto d) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codUsuario", codUsuario);
        TcpeParametros.poner(p, "ci", recortar(d.getCi()));
        TcpeParametros.poner(p, "nombres", recortar(d.getNombres()));
        TcpeParametros.poner(p, "apellidoPaterno", recortar(d.getApellidoPaterno()));
        TcpeParametros.poner(p, "apellidoMaterno", recortar(d.getApellidoMaterno()));
        return spHelper.ejecutarAbmMap(SP_ABM, p, "R");
    }

    private static String recortar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
