package bo.bosque.com.impexpap.dao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.PostergacionDto;
import bo.bosque.com.impexpap.model.ChPostergacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a <b>tch_chPostergacion</b>. Todo pasa por procedimiento almacenado via {@link SpHelper}.
 *
 * <p>El {@code Map} de parametros se arma a mano: {@code p_list_ChPostergacion} usa {@code @x IS NULL} como "sin filtro" y un
 * {@code 0} seria un filtro real.
 */
@Repository
public class ChPostergacionDAO implements IChPostergacion {

    private static final String SP_ABM = "p_abm_ChPostergacion";
    private static final String SP_LIST = "p_list_ChPostergacion";

    private final SpHelper spHelper;

    public ChPostergacionDAO(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<PostergacionDto> listarPorCheque(int codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        List<PostergacionDto> filas = new ArrayList<>(spHelper.ejecutarListado(SP_LIST, p, "L", PostergacionDto.class));
        // El procedimiento no ordena (tabla sin indices): el codigo es identity, o sea el orden en que se registraron.
        filas.sort(Comparator.comparing(PostergacionDto::getCodPostergacion, Comparator.nullsLast(Comparator.<Integer>naturalOrder())));
        int n = 1;
        for (PostergacionDto f : filas) f.setFila(n++);
        return filas;
    }

    @Override
    public ChPostergacion obtener(int codPostergacion) {
        if (codPostergacion <= 0) return null;
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codPostergacion", codPostergacion);
        List<ChPostergacion> filas = spHelper.ejecutarListado(SP_LIST, p, "L", ChPostergacion.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public RespuestaSp registrar(ChPostergacion x) {
        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "codCheque", x.getCodCheque());
        ArgsSp.poner(p, "fecha", x.getFecha());
        ArgsSp.poner(p, "observacion", x.getObservacion());
        ArgsSp.poner(p, "audUsuario", x.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I");
    }

    @Override
    public RespuestaSp eliminar(int codPostergacion, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codPostergacion", codPostergacion);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }
}
