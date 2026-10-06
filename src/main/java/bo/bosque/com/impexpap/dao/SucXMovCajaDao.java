// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\SucXMovCajaDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SucXMovCaja;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class SucXMovCajaDao implements ISucXMovCaja {

    private final SpHelper spHelper;

    public SucXMovCajaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(SucXMovCaja mb, String acc) {
        log.info("Registrando SucXMovCaja: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_SucXMovCaja", mb, acc);
    }

    @Override
    public List<SucXMovCaja> listar(SucXMovCaja filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_SucXMovCaja", filtro, "L", SucXMovCaja.class);
    }

    // FIX: usa Map (no el modelo) para que idSxMC==0 no viaje como filtro real —
    // el SP trata @idSxMC IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public SucXMovCaja obtenerPorId(long idSxMC) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idSxMC", idSxMC);
        List<SucXMovCaja> resultado = spHelper.ejecutarListado(
                "p_list_tac_SucXMovCaja", filtro, "L", SucXMovCaja.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<Map<String, Object>> listarSaldoSapPorOcurrencia(long idBitTarea) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", idBitTarea);
        params.put("ACCION", "A");
        return spHelper.ejecutarListadoDinamico("p_list_tac_SucXMovCaja", params);
    }
}
