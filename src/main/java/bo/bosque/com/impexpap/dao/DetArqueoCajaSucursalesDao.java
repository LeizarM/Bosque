// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\DetArqueoCajaSucursalesDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DetArqueoCajaSucursales;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class DetArqueoCajaSucursalesDao implements IDetArqueoCajaSucursales {

    private final SpHelper spHelper;

    public DetArqueoCajaSucursalesDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(DetArqueoCajaSucursales mb, String acc) {
        log.info("Registrando DetArqueoCajaSucursales: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_DetArqueoCajaSucursales", mb, acc);
    }

    @Override
    public List<DetArqueoCajaSucursales> listar(DetArqueoCajaSucursales filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_DetArqueoCajaSucursales", filtro, "L", DetArqueoCajaSucursales.class);
    }

    // FIX: usa Map (no el modelo) para que idDetAS==0 no viaje como filtro real —
    // el SP trata @idDetAS IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public DetArqueoCajaSucursales obtenerPorId(long idDetAS) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idDetAS", idDetAS);
        List<DetArqueoCajaSucursales> resultado = spHelper.ejecutarListado(
                "p_list_tac_DetArqueoCajaSucursales", filtro, "L", DetArqueoCajaSucursales.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
