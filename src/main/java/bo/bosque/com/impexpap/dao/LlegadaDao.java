// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\LlegadaDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Llegada;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class LlegadaDao implements ILlegada {

    private final SpHelper spHelper;

    public LlegadaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(Llegada mb, String acc) {
        log.info("Registrando Llegada: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_Llegada", mb, acc);
    }

    @Override
    public List<Llegada> listar(Llegada filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_Llegada", filtro, "L", Llegada.class);
    }

    // FIX: usa Map (no el modelo) para que idRp==0 no viaje como filtro real —
    // el SP trata @idRp IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public Llegada obtenerPorId(long idRp) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRp", idRp);
        List<Llegada> resultado = spHelper.ejecutarListado(
                "p_list_tac_Llegada", filtro, "L", Llegada.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<Map<String, Object>> obtenerDelDia(long idBitTarea, boolean todasSucursales, Date fecha) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", idBitTarea);
        params.put("todasSucursales", todasSucursales ? 1 : 0);
        // Sin fecha no viaja: el SP usa hoy, el de SQL Server.
        if (fecha != null) params.put("fecha", fecha);
        params.put("ACCION", "B");
        return spHelper.ejecutarListadoDinamico("p_list_tac_Llegada", params);
    }
}
