package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.TransaccionBancariaDto;
import bo.bosque.com.impexpap.model.ChTransaccionBancaria;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a <b>tch_chTransaccionBancaria</b>. Todo pasa por procedimiento almacenado via {@link SpHelper}.
 *
 * <p>La rama 'A' de {@code p_list_ChTransaccionBancaria} se lee por posicion, como el legacy
 * ({@code ChTransaccionBancariaDAO.listadoTransaccionBancaria}): 1 codCheque, 2 nroTransaccion, 3 codBanco,
 * 4 fechaTransaccion, 5 datoBanco, 6 datoFecha (texto dd/MM/yyyy que no se usa).
 */
@Repository
public class ChTransaccionBancariaDAO implements IChTransaccionBancaria {

    private static final String SP_ABM = "p_abm_ChTransaccionBancaria";
    private static final String SP_LIST = "p_list_ChTransaccionBancaria";

    private static final RowMapper<TransaccionBancariaDto> MAPA_FILA = (rs, i) -> {
        TransaccionBancariaDto t = new TransaccionBancariaDto();
        t.setCodCheque(ArgsSp.entero(rs, 1));
        t.setNroTransaccion(rs.getString(2));
        t.setCodBanco(ArgsSp.entero(rs, 3));
        t.setFechaTransaccion(ArgsSp.fecha(rs, 4));
        t.setDatoBanco(rs.getString(5));
        return t;
    };

    private final SpHelper spHelper;

    public ChTransaccionBancariaDAO(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TransaccionBancariaDto> listarPorCheque(int codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        List<TransaccionBancariaDto> filas = spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "A", MAPA_FILA);
        int n = 1;
        for (TransaccionBancariaDto f : filas) f.setFila(n++);
        return filas;
    }

    @Override
    public RespuestaSp registrar(ChTransaccionBancaria t) {
        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "codCheque", t.getCodCheque());
        ArgsSp.poner(p, "nroTransaccion", t.getNroTransaccion());
        ArgsSp.poner(p, "codBanco", t.getCodBanco());
        ArgsSp.poner(p, "fechaTransaccion", t.getFechaTransaccion());
        ArgsSp.poner(p, "audUsuario", t.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I");
    }

    @Override
    public RespuestaSp eliminar(int codCheque, String nroTransaccion, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        ArgsSp.poner(p, "nroTransaccion", nroTransaccion);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }
}
