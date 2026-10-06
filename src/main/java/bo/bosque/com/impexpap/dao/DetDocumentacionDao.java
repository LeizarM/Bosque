// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\DetDocumentacionDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DetDocumentacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class DetDocumentacionDao implements IDetDocumentacion {

    private final SpHelper spHelper;

    public DetDocumentacionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(DetDocumentacion mb, String acc) {
        log.info("Registrando DetDocumentacion: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_DetDocumentacion", mb, acc);
    }

    @Override
    public List<DetDocumentacion> listar(DetDocumentacion filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_DetDocumentacion", filtro, "L", DetDocumentacion.class);
    }

    // FIX: usa Map (no el modelo) para que idDetDoc==0 no viaje como filtro real —
    // el SP trata @idDetDoc IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public DetDocumentacion obtenerPorId(long idDetDoc) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idDetDoc", idDetDoc);
        List<DetDocumentacion> resultado = spHelper.ejecutarListado(
                "p_list_tac_DetDocumentacion", filtro, "L", DetDocumentacion.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
