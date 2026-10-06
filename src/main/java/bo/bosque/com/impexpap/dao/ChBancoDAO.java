package bo.bosque.com.impexpap.dao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.BancoDto;
import bo.bosque.com.impexpap.model.ChBanco;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a <b>tch_banco</b>. Todo pasa por procedimiento almacenado via {@link SpHelper}.
 *
 * <p>Los listados de {@code /banco/bancosX} y {@code /banco/bancosPlanilla} conservan su forma
 * ({@link BancoDto}, con {@code fila}); las columnas se leen por posicion (1 y 2) como siempre.
 */
@Repository
public class ChBancoDAO implements IChBanco {

    private static final String SP_ABM = "p_abm_Banco";
    private static final String SP_LIST = "p_list_Banco";

    private final SpHelper spHelper;

    public ChBancoDAO(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<BancoDto> listBancos() {
        return numerar(spHelper.ejecutarListadoPorPosicion(SP_LIST, new LinkedHashMap<String, Object>(), "L", MAPA));
    }

    @Override
    public List<BancoDto> listBancosPlanilla() {
        return numerar(spHelper.ejecutarListadoPorPosicion(SP_LIST, new LinkedHashMap<String, Object>(), "A", MAPA));
    }

    @Override
    public ChBanco obtener(int codBanco) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codBanco", codBanco);
        List<ChBanco> filas = spHelper.ejecutarListado(SP_LIST, p, "L", ChBanco.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public RespuestaSp registrar(ChBanco banco) {
        boolean alta = banco.getCodBanco() == null || banco.getCodBanco() == 0;
        Map<String, Object> p = new LinkedHashMap<>();
        if (!alta) p.put("codBanco", banco.getCodBanco());
        ArgsSp.poner(p, "nombre", banco.getNombre());
        ArgsSp.poner(p, "audUsuario", banco.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, alta ? "I" : "U");
    }

    @Override
    public RespuestaSp eliminar(int codBanco, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codBanco", codBanco);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }

    private static final org.springframework.jdbc.core.RowMapper<BancoDto> MAPA =
            (rs, i) -> new BancoDto(rs.getInt(1), rs.getString(2), 0, 0);

    /** Numera las filas 1..n (el campo {@code fila} que ya devolvia el endpoint). */
    private static List<BancoDto> numerar(List<BancoDto> filas) {
        List<BancoDto> salida = new ArrayList<>(filas.size());
        int n = 1;
        for (BancoDto b : filas) {
            b.setFila(n++);
            salida.add(b);
        }
        return salida;
    }
}
