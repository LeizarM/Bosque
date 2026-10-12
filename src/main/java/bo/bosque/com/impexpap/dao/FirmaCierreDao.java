// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\FirmaCierreDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CeldaCierreDto;
import bo.bosque.com.impexpap.dto.CeldaTableroDto;
import bo.bosque.com.impexpap.dto.FilaSeccionDto;
import bo.bosque.com.impexpap.dto.FilaSeccionRequest;
import bo.bosque.com.impexpap.dto.FirmarSeccionRequest;
import bo.bosque.com.impexpap.dto.ResumenCierrePersonaDto;
import bo.bosque.com.impexpap.utils.ListadoConEstado;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Firmas y tablero de Verificar Cierre ({@code p_abm_tac_FirmaCierre} y
 * {@code p_list_tac_FirmaCierre}, archivo SQL 81). Sin validaciones propias:
 * el SP decide, y su mensaje llega a la app tal cual.
 */
@Repository
public class FirmaCierreDao implements IFirmaCierre {

    private static final String SP_ABM = "p_abm_tac_FirmaCierre";
    private static final String SP_LIST = "p_list_tac_FirmaCierre";

    private final SpHelper spHelper;

    public FirmaCierreDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<CeldaCierreDto> misCeldas(long idBitTarea) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", idBitTarea);
        return spHelper.ejecutarListado(SP_LIST, params, "M", CeldaCierreDto.class);
    }

    @Override
    public RespuestaSp firmar(FirmarSeccionRequest req, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", req.getIdBitTarea());
        params.put("seccion", req.getSeccion());
        if (req.getCodSucursal() != null) params.put("codSucursal", req.getCodSucursal());
        params.put("obs", req.getObs());
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "F");
    }

    @Override
    public RespuestaSp anular(long idFC, String obs, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idFC", idFC);
        params.put("obs", obs);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "X");
    }

    @Override
    public RespuestaSp marcarFila(long idBitTarea, String seccion, long idFila, int marca, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", idBitTarea);
        params.put("seccion", seccion);
        params.put("idFila", idFila);
        params.put("marca", marca);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "M");
    }

    @Override
    public ListadoConEstado<FilaSeccionDto> filasSeccion(FilaSeccionRequest req, long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idBitTarea", req.getIdBitTarea());
        params.put("seccion", req.getSeccion());
        if (req.getCodSucursal() != null) params.put("codSucursal", req.getCodSucursal());
        // Otro día: el SP solo lo acepta si es anterior al de la celda.
        if (req.getFecha() != null) params.put("fechaIni", req.getFecha());
        params.put("audUsuario", audUsuario);
        params.put("ACCION", "G");
        return spHelper.ejecutarListadoConEstado(SP_LIST, params, FilaSeccionDto.class);
    }

    @Override
    public RespuestaSp marcarFilaSeccion(FilaSeccionRequest req, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", req.getIdBitTarea());
        params.put("seccion", req.getSeccion());
        if (req.getCodSucursal() != null) params.put("codSucursal", req.getCodSucursal());
        if (req.getClave() != null) params.put("clave", req.getClave());
        if (req.getMarca() != null) params.put("marca", req.getMarca());
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "G");
    }

    @Override
    public List<CeldaTableroDto> tablero(Date fechaIni, Date fechaFin, long audUsuario) {
        return spHelper.ejecutarListado(SP_LIST, rango(fechaIni, fechaFin, audUsuario), "K", CeldaTableroDto.class);
    }

    @Override
    public List<ResumenCierrePersonaDto> resumen(Date fechaIni, Date fechaFin, long audUsuario) {
        return spHelper.ejecutarListado(SP_LIST, rango(fechaIni, fechaFin, audUsuario), "P", ResumenCierrePersonaDto.class);
    }

    /** Las fechas nulas no viajan: el SP pone los últimos 7 días. */
    private static Map<String, Object> rango(Date fechaIni, Date fechaFin, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        if (fechaIni != null) params.put("fechaIni", fechaIni);
        if (fechaFin != null) params.put("fechaFin", fechaFin);
        params.put("audUsuario", audUsuario);
        return params;
    }
}
