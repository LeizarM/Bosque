// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ArqueoCajaSucursalesDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ArqueoCajaSucursales;
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
public class ArqueoCajaSucursalesDao implements IArqueoCajaSucursales {

    private final SpHelper spHelper;

    public ArqueoCajaSucursalesDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(ArqueoCajaSucursales mb, String acc) {
        log.info("Registrando ArqueoCajaSucursales: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_ArqueoCajaSucursales", mb, acc);
    }

    @Override
    public List<ArqueoCajaSucursales> listar(ArqueoCajaSucursales filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_ArqueoCajaSucursales", filtro, "L", ArqueoCajaSucursales.class);
    }

    // FIX: usa Map (no el modelo) para que idAC==0 no viaje como filtro real —
    // el SP trata @idAC IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public ArqueoCajaSucursales obtenerPorId(long idAC) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idAC", idAC);
        List<ArqueoCajaSucursales> resultado = spHelper.ejecutarListado(
                "p_list_tac_ArqueoCajaSucursales", filtro, "L", ArqueoCajaSucursales.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    // Mismo motivo que obtenerPorId: Map y no el modelo, para que solo viaje
    // @idBitTarea. La rama 'L' ordena por idAC: el último es el más nuevo.
    @Override
    public ArqueoCajaSucursales obtenerPorOcurrencia(long idBitTarea) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idBitTarea", idBitTarea);
        List<ArqueoCajaSucursales> resultado = spHelper.ejecutarListado(
                "p_list_tac_ArqueoCajaSucursales", filtro, "L", ArqueoCajaSucursales.class);
        return resultado.isEmpty() ? null : resultado.get(resultado.size() - 1);
    }

    @Override
    public List<Map<String, Object>> obtenerTipoCambio() {
        Map<String, Object> params = new HashMap<>();
        params.put("ACCION", "T");
        return spHelper.ejecutarListadoDinamico("p_list_tac_ArqueoCajaSucursales", params);
    }

    @Override
    public List<Map<String, Object>> obtenerAnterior(long idBitTarea) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", idBitTarea);
        params.put("ACCION", "H");
        return spHelper.ejecutarListadoDinamico("p_list_tac_ArqueoCajaSucursales", params);
    }

    @Override
    public List<Map<String, Object>> obtenerDelDia(long idBitTarea, boolean todasSucursales, Date fecha) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", idBitTarea);
        params.put("todasSucursales", todasSucursales ? 1 : 0);
        // Sin fecha no viaja: el SP usa hoy, el de SQL Server.
        if (fecha != null) params.put("fecha", fecha);
        params.put("ACCION", "B");
        return spHelper.ejecutarListadoDinamico("p_list_tac_ArqueoCajaSucursales", params);
    }
}
