// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\TarRuXCargoDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TarRuXCargo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class TarRuXCargoDao implements ITarRuXCargo {

    private final SpHelper spHelper;

    public TarRuXCargoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(TarRuXCargo mb, String acc) {
        log.info("Registrando TarRuXCargo: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_TarRuXCargo", mb, acc);
    }

    @Override
    public List<TarRuXCargo> listar(TarRuXCargo filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_TarRuXCargo", filtro, "L", TarRuXCargo.class);
    }

    // FIX: usa Map (no el modelo) para que idTarXCargo==0 no viaje como filtro real —
    // el SP trata @idTarXCargo IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public TarRuXCargo obtenerPorId(long idTarXCargo) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTarXCargo", idTarXCargo);
        List<TarRuXCargo> resultado = spHelper.ejecutarListado(
                "p_list_tac_TarRuXCargo", filtro, "L", TarRuXCargo.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<Map<String, Object>> listarPorCargoConDetalle(long codCargo) {
        Map<String, Object> params = new HashMap<>();
        params.put("codCargo", codCargo);
        params.put("ACCION", "C");
        return spHelper.ejecutarListadoDinamico("p_list_tac_TarRuXCargo", params);
    }

    @Override
    public List<Map<String, Object>> listarTraspasosPendientes(LocalDate desde) {
        Map<String, Object> params = new HashMap<>();
        // ejecutarListadoDinamico arma el EXEC solo con las claves que se le
        // pasan, asi que omitir @fechaInicio deja el DEFAULT del proc (90 dias)
        // en vez de mandar un NULL explicito.
        if (desde != null) {
            params.put("fechaInicio", java.sql.Date.valueOf(desde));
        }
        params.put("ACCION", "T");
        return spHelper.ejecutarListadoDinamico("p_list_tac_TarRuXCargo", params);
    }

    @Override
    public RespuestaSp traspasarTarea(long idTarRuti, long codCargoDestino, long audUsuario) {
        log.info("Traspasando tarea rutinaria {} al cargo {}", idTarRuti, codCargoDestino);
        Map<String, Object> params = new HashMap<>();
        params.put("idTarRuti", idTarRuti);
        params.put("codCargo", codCargoDestino);
        params.put("audUsuario", audUsuario);
        // ejecutarAbmMap y no ejecutarAbm: el binding por metadatos de
        // ejecutarAbm exige un valor para TODOS los parametros declarados del
        // proc, y aca se mandan solo tres a proposito para que el resto tome
        // su DEFAULT (mismo motivo por el que BitTareaRutiDao.registrar tuvo
        // que migrar a ejecutarAbmMap tras el archivo 27).
        RespuestaSp r = spHelper.ejecutarAbmMap("p_abm_tac_TarRuXCargo", params, "T");

        // Los bloques del proc son IF sueltos, sin ELSE final: un @ACCION que
        // no conoce NO falla — sale por abajo con @error=0 y @errormsg=''. O
        // sea que si este backend se despliega sin haber corrido el archivo
        // SQL 39, la pantalla diría "tarea copiada" sin haber copiado nada.
        // La ACCION 'T' siempre deja mensaje (copiada / reactivada / ya la
        // tenía), así que un mensaje vacío solo puede significar eso.
        if (r != null && r.getError() == 0
                && (r.getErrormsg() == null || r.getErrormsg().trim().isEmpty())) {
            log.error("p_abm_tac_TarRuXCargo no reconoce ACCION='T': falta correr el script SQL 39.");
            return new RespuestaSp(39,
                    "El traspaso todavía no está habilitado en la base de datos "
                            + "(falta correr el script 39). No se copió nada.", 0);
        }
        return r;
    }
}
