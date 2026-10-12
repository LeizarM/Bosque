// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ResponsableCierreDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CandidatoCierreDto;
import bo.bosque.com.impexpap.dto.FuenteCierreDto;
import bo.bosque.com.impexpap.dto.FuenteCierreRequest;
import bo.bosque.com.impexpap.dto.CandidatoFuenteDto;
import bo.bosque.com.impexpap.dto.SeccionCierreDto;
import bo.bosque.com.impexpap.dto.SeccionCierreRequest;
import bo.bosque.com.impexpap.dto.ResponsableCierreDto;
import bo.bosque.com.impexpap.dto.ResponsableCierreRequest;
import bo.bosque.com.impexpap.dto.SucursalCajaDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@code p_abm_tac_ResponsableCierre} y {@code p_list_tac_ResponsableCierre} (archivo SQL 81). */
@Repository
public class ResponsableCierreDao implements IResponsableCierre {

    private static final String SP_ABM = "p_abm_tac_ResponsableCierre";
    private static final String SP_LIST = "p_list_tac_ResponsableCierre";

    private final SpHelper spHelper;

    public ResponsableCierreDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<ResponsableCierreDto> listar(long idTarRuti) {
        return spHelper.ejecutarListado(SP_LIST, tarea(idTarRuti), "L", ResponsableCierreDto.class);
    }

    @Override
    public List<CandidatoCierreDto> candidatos(long idTarRuti) {
        return spHelper.ejecutarListado(SP_LIST, tarea(idTarRuti), "C", CandidatoCierreDto.class);
    }

    @Override
    public List<SucursalCajaDto> sucursales() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "S", SucursalCajaDto.class);
    }

    @Override
    public RespuestaSp agregar(ResponsableCierreRequest req, long audUsuario) {
        return spHelper.ejecutarAbmMap(SP_ABM, fila(req, audUsuario), "I");
    }

    @Override
    public RespuestaSp cambiar(ResponsableCierreRequest req, long audUsuario) {
        Map<String, Object> params = fila(req, audUsuario);
        poner(params, "idRC", req.getIdRC());
        poner(params, "estado", req.getEstado());
        return spHelper.ejecutarAbmMap(SP_ABM, params, "U");
    }

    @Override
    public RespuestaSp quitar(long idRC, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idRC", idRC);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    @Override
    public List<SeccionCierreDto> secciones(long idTarRuti) {
        return spHelper.ejecutarListado(SP_LIST, tarea(idTarRuti), "E", SeccionCierreDto.class);
    }

    @Override
    public List<FuenteCierreDto> fuentes() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "W", FuenteCierreDto.class);
    }

    @Override
    public RespuestaSp guardarSeccion(SeccionCierreRequest req, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        poner(params, "seccion", vacioANull(req.getSeccion()));
        poner(params, "nombre", req.getNombre());
        poner(params, "queRevisar", req.getQueRevisar());
        poner(params, "porSucursal", req.getPorSucursal());
        poner(params, "fuente", vacioANull(req.getFuente()));
        poner(params, "marcaPorFila", req.getMarcaPorFila());
        poner(params, "firmaSola", req.getFirmaSola());
        poner(params, "obsDesde", req.getObsDesde());
        poner(params, "diaRevisado", vacioANull(req.getDiaRevisado()));
        poner(params, "orden", req.getOrden());
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "S");
    }

    @Override
    public RespuestaSp quitarSeccion(String seccion, int estado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        poner(params, "seccion", vacioANull(seccion));
        params.put("estado", estado);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "Q");
    }

    @Override
    public List<FuenteCierreDto> fuentesTodas() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "V", FuenteCierreDto.class);
    }

    @Override
    public List<CandidatoFuenteDto> candidatasFuente() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "O", CandidatoFuenteDto.class);
    }

    @Override
    public RespuestaSp guardarFuente(FuenteCierreRequest req, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        poner(params, "fuente", vacioANull(req.getFuente()));
        poner(params, "nombre", req.getNombre());
        poner(params, "descripcion", req.getDescripcion());
        poner(params, "spMarcar", vacioANull(req.getSpMarcar()));
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "F");
    }

    @Override
    public RespuestaSp estadoFuente(String fuente, int estado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        poner(params, "fuente", vacioANull(fuente));
        params.put("estado", estado);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "A");
    }

    @Override
    public RespuestaSp borrarFuente(String fuente, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        poner(params, "fuente", vacioANull(fuente));
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "B");
    }

    private static String vacioANull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static Map<String, Object> tarea(long idTarRuti) {
        Map<String, Object> params = new HashMap<>();
        params.put("idTarRuti", idTarRuti);
        return params;
    }

    /**
     * Los null no viajan: el SP los toma como NULL por defecto. La sucursal
     * vacía es "todas", y quién es (cargo o persona) lo valida el SP.
     */
    private static Map<String, Object> fila(ResponsableCierreRequest req, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        poner(params, "idTarRuti", req.getIdTarRuti());
        poner(params, "seccion", req.getSeccion());
        poner(params, "codSucursal", req.getCodSucursal());
        poner(params, "codCargoSucursal", req.getCodCargoSucursal());
        poner(params, "codEmpleado", req.getCodEmpleado());
        poner(params, "puedeAutorrevisar", req.getPuedeAutorrevisar());
        params.put("audUsuario", audUsuario);
        return params;
    }

    private static void poner(Map<String, Object> params, String clave, Object valor) {
        if (valor != null) params.put(clave, valor);
    }
}
