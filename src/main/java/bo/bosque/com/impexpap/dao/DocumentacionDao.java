// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\DocumentacionDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Documentacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class DocumentacionDao implements IDocumentacion {

    private final SpHelper spHelper;

    public DocumentacionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(Documentacion mb, String acc) {
        log.info("Registrando Documentacion: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_Documentacion", mb, acc);
    }

    @Override
    public List<Documentacion> listar(Documentacion filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_Documentacion", filtro, "L", Documentacion.class);
    }

    // FIX: usa Map (no el modelo) para que idDoc==0 no viaje como filtro real —
    // el SP trata @idDoc IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public Documentacion obtenerPorId(long idDoc) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idDoc", idDoc);
        List<Documentacion> resultado = spHelper.ejecutarListado(
                "p_list_tac_Documentacion", filtro, "L", Documentacion.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
