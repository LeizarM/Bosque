// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\CocheDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Coche;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class CocheDao implements ICoche {

    private final SpHelper spHelper;

    public CocheDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(Coche mb, String acc) {
        log.info("Registrando Coche: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_Coche", mb, acc);
    }

    @Override
    public List<Coche> listar(Coche filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_Coche", filtro, "L", Coche.class);
    }

    // Map armado a mano y no ejecutarListadoSinCero: ese quita TODO Number==0, y aqui
    // estado=0 (inactivo) es un filtro legitimo de la pantalla. idCoche y audUsuario son
    // primitivos: 0 significa "no vino", por eso audUsuario ni siquiera se filtra.
    @Override
    public List<Coche> listarGestion(Coche filtro) {
        Map<String, Object> params = new HashMap<>();
        if (filtro != null) {
            if (filtro.getIdCoche() > 0) params.put("idCoche", filtro.getIdCoche());
            putTexto(params, "marca", filtro.getMarca());
            putTexto(params, "clase", filtro.getClase());
            putTexto(params, "placa", filtro.getPlaca());
            putTexto(params, "color", filtro.getColor());
            if (filtro.getAnio() != null) params.put("anio", filtro.getAnio());
            if (filtro.getCodSucursal() != null) params.put("codSucursal", filtro.getCodSucursal());
            if (filtro.getEstado() != null) params.put("estado", filtro.getEstado());
        }
        return spHelper.ejecutarListado("p_list_tac_Coche", params, "S", Coche.class);
    }

    private static void putTexto(Map<String, Object> params, String clave, String valor) {
        if (valor != null && !valor.trim().isEmpty()) params.put(clave, valor.trim());
    }

    // FIX: usa Map (no el modelo) para que idCoche==0 no viaje como filtro real —
    // el SP trata @idCoche IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public Coche obtenerPorId(long idCoche) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCoche", idCoche);
        List<Coche> resultado = spHelper.ejecutarListado(
                "p_list_tac_Coche", filtro, "L", Coche.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
