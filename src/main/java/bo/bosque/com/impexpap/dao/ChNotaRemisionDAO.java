package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.NotaRemisionDto;
import bo.bosque.com.impexpap.model.ChNotaRemision;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a <b>tch_notaRemision</b>. Todo pasa por procedimiento almacenado via {@link SpHelper}.
 *
 * <p>El {@code Map} de parametros se arma a mano: {@code p_list_NotaRemision} usa {@code @x IS NULL} como "sin filtro" y un
 * {@code 0} seria un filtro real.
 */
@Repository
public class ChNotaRemisionDAO implements IChNotaRemision {

    private static final String SP_ABM = "p_abm_NotaRemision";
    private static final String SP_LIST = "p_list_NotaRemision";

    private final SpHelper spHelper;

    public ChNotaRemisionDAO(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<NotaRemisionDto> listarPorCheque(int codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        List<NotaRemisionDto> filas = spHelper.ejecutarListado(SP_LIST, p, "L", NotaRemisionDto.class);
        int n = 1;
        for (NotaRemisionDto f : filas) f.setFila(n++);
        return filas;
    }

    @Override
    public RespuestaSp registrar(ChNotaRemision nota) {
        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "codCheque", nota.getCodCheque());
        ArgsSp.poner(p, "notaRemision", nota.getNotaRemision());
        ArgsSp.poner(p, "nroFactura", nota.getNroFactura());
        ArgsSp.poner(p, "fechaFactura", nota.getFechaFactura());
        ArgsSp.poner(p, "audUsuario", nota.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I");
    }

    @Override
    public RespuestaSp eliminar(int codCheque, String notaRemision, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        ArgsSp.poner(p, "notaRemision", notaRemision);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }
}
