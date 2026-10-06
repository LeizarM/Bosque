// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\TesBaseDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.DiaRevisadoTesBaseDto;
import bo.bosque.com.impexpap.dto.TraspasoEfectivoPendienteDto;
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
public class TesBaseDao implements ITesBase {

    private final SpHelper spHelper;

    public TesBaseDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TraspasoEfectivoPendienteDto> listarPendientes() {
        return spHelper.ejecutarListado(
                "p_list_TesBase", new HashMap<String, Object>(), "B",
                TraspasoEfectivoPendienteDto.class);
    }

    @Override
    public DiaRevisadoTesBaseDto diaRevisado(long idBitTarRuti) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarRuti", idBitTarRuti);
        List<DiaRevisadoTesBaseDto> filas = spHelper.ejecutarListado(
                "p_list_TesBase", params, "H", DiaRevisadoTesBaseDto.class);
        return filas == null || filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public List<TraspasoEfectivoPendienteDto> listarDelDia(Date fecha) {
        Map<String, Object> params = new HashMap<>();
        params.put("fechaRegistro", fecha);
        return spHelper.ejecutarListado(
                "p_list_TesBase", params, "D", TraspasoEfectivoPendienteDto.class);
    }

    // Los dos van por ejecutarAbmMap y NO por ejecutarAbm. ejecutarAbm arma la
    // llamada desde la metadata del SP y exige un valor para cada parámetro
    // declarado; p_abm_TesBase tiene once, y estas ACCIONes usan dos o tres.
    // ejecutarAbmMap manda solo las claves provistas (y los tres OUTPUT que el
    // archivo SQL 56 le agregó al procedimiento para esto).

    @Override
    public RespuestaSp cerrar(long codTes, long idBitTarRuti, long audUsuario) {
        log.info("Cerrando transferencia TesBase codTes={} desde la ocurrencia {}", codTes, idBitTarRuti);
        Map<String, Object> params = new HashMap<>();
        params.put("codTes", codTes);
        params.put("idBitTarRuti", idBitTarRuti);
        params.put("audUsuarioI", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_TesBase", params, "C");
    }

    @Override
    public RespuestaSp sinPendientes(long idBitTarRuti, long audUsuario) {
        log.info("Sin pendientes de TesBase para la ocurrencia {}", idBitTarRuti);
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarRuti", idBitTarRuti);
        params.put("audUsuarioI", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_TesBase", params, "S");
    }
}
