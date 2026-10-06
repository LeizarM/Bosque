package bo.bosque.com.impexpap.dao;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.AccionChequeDto;
import bo.bosque.com.impexpap.model.ChAccion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import bo.bosque.com.impexpap.utils.Tipos;

/**
 * Acceso a <b>tch_accion</b>. Todo pasa por procedimiento almacenado via {@link SpHelper}.
 *
 * <p>Los listados no repiten el cuerpo de la consulta en Java: llaman a las ramas de
 * {@code p_list_Accion} que ya usaba el legacy. El {@code Map} de parametros se arma a mano porque
 * {@code p_list_Accion} usa {@code @x IS NULL} como "sin filtro" y un {@code 0} seria un filtro real.
 */
@Repository
public class ChAccionDAO implements IChAccion {

    private static final String SP_ABM = "p_abm_Accion";
    private static final String SP_LIST = "p_list_Accion";

    private final SpHelper spHelper;

    public ChAccionDAO(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // ------------------------------ Lecturas ------------------------------

    @Override
    public List<AccionChequeDto> listarPorCheque(int codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        List<AccionChequeDto> filas = spHelper.ejecutarListado(SP_LIST, p, "L", AccionChequeDto.class);

        Map<String, String> nombres = new HashMap<>();
        for (Tipos t : new Tipos().lstEstadoAccionCheque()) nombres.put(t.getCodTipos(), t.getNombre());

        int n = 1;
        for (AccionChequeDto a : filas) {
            a.setNro(n++);
            String nombre = a.getEstado() == null ? null : nombres.get(a.getEstado().trim());
            a.setDescripcion(nombre == null ? "" : nombre);
        }
        return filas;
    }

    @Override
    public ChAccion obtener(int codAccion) {
        if (codAccion <= 0) return null;
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codAccion", codAccion);
        List<ChAccion> filas = spHelper.ejecutarListado(SP_LIST, p, "L", ChAccion.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public int contarPorCheque(int codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        return primerEntero(spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "F", (rs, i) -> rs.getInt(1)));
    }

    @Override
    public int traspasosDelCheque(int codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        return primerEntero(spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "G", (rs, i) -> rs.getInt(1)));
    }

    @Override
    public boolean devolucionesIgualAEntregas(int codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        return primerEntero(spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "H", (rs, i) -> rs.getInt(1))) == 1;
    }

    @Override
    public int pendientesDeTraspaso(long codSucursal) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codAccion", (int) codSucursal);   // la sucursal va en @codAccion
        return primerEntero(spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "C", (rs, i) -> rs.getInt(1)));
    }

    // ------------------------------ Escrituras ------------------------------

    @Override
    public RespuestaSp registrar(ChAccion a) {
        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "codCheque", a.getCodCheque());
        ArgsSp.poner(p, "fecha", a.getFecha());
        ArgsSp.poner(p, "estado", a.getEstado());
        ArgsSp.poner(p, "codEmpleado", a.getCodEmpleado());
        ArgsSp.poner(p, "nroSAP", a.getNroSAP());
        ArgsSp.poner(p, "observacion", a.getObservacion());
        ArgsSp.poner(p, "audUsuario", a.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I");
    }

    @Override
    public RespuestaSp eliminar(int codAccion, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codAccion", codAccion);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }

    @Override
    public RespuestaSp copiar(int codAccionOrigen, int codChequeDestino, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codAccion", codAccionOrigen);
        p.put("codCheque", codChequeDestino);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "A");
    }

    @Override
    public RespuestaSp traspasar(long codSucursal, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codAccion", (int) codSucursal);   // la sucursal va en @codAccion
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_LIST, p, "E");
    }

    private static int primerEntero(List<Integer> filas) {
        return filas.isEmpty() || filas.get(0) == null ? 0 : filas.get(0);
    }
}
