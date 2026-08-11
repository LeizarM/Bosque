package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.IntervencionRolDto;
import bo.bosque.com.impexpap.model.Rol;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class RolDao implements IRol {

    private static final String SP_ABM  = "p_abm_trs_Rol";
    private static final String SP_LIST = "p_list_trs_Rol";

    private final SpHelper spHelper;

    public RolDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarRol(Rol rol, String acc) {
        log.info("Registrando Rol: {}, Accion: {}", rol.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, rol, acc);
    }

    // ── L: cabeceras con filtros opcionales ───────────────────────────────
    // El SP filtra con (ISNULL(@x,0)=0 OR ...), así que 0 = sin filtro.
    @Override
    public List<Rol> obtenerRoles(long codSucursal, int anio) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codSucursal", codSucursal);
        filtro.put("anio", anio);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Rol.class);
    }

    // ── L: por id ─────────────────────────────────────────────────────────
    @Override
    public Rol obtenerRolPorId(long idRol) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        List<Rol> r = spHelper.ejecutarListado(SP_LIST, filtro, "L", Rol.class);
        return r.isEmpty() ? null : r.get(0);
    }

    // ── I: las intervenciones humanas sobre el default ────────────────────
    // Devuelve una forma DISTINTA de la de la tabla (UNION de 4 fuentes),
    // por eso mapea contra IntervencionRolDto y no contra Rol.
    @Override
    public List<IntervencionRolDto> obtenerIntervenciones(long idRol) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        return spHelper.ejecutarListado(SP_LIST, filtro, "I", IntervencionRolDto.class);
    }
}
