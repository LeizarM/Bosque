package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.PermisoSabadoDto;
import bo.bosque.com.impexpap.dto.PuenteVacacionDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class PermisoSabadoDao implements IPermisoSabado {

    private static final String SP_LIST   = "p_list_trs_PermisoSabado";
    private static final String SP_PUENTE = "trs_sp_puenteVacacion";

    private final SpHelper spHelper;

    public PermisoSabadoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<PermisoSabadoDto> obtenerPermisosDeSabado(long idRol, long codEmpleado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        filtro.put("codEmpleado", codEmpleado);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", PermisoSabadoDto.class);
    }

    @Override
    public List<PermisoSabadoDto> obtenerDesfases(long idRol) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        return spHelper.ejecutarListado(SP_LIST, filtro, "D", PermisoSabadoDto.class);
    }

    // ══════════════════════════════════════════════════════════════════════
    // PUENTE A CUENTA DE VACACIÓN
    // ══════════════════════════════════════════════════════════════════════

    /*
     * Las dos acciones van por helpers DISTINTOS, y no es capricho: 'S' devuelve un
     * resultset y nada en los OUTPUT; 'A' devuelve OUTPUT y ningún resultset.
     * ejecutarAbmMap lee lo segundo y ejecutarListado lo primero. Un SP que devolviera
     * las dos cosas a la vez rompería a ejecutarAbmMap, que toma el primer resultset
     * que aparezca como si fuera el de sus variables de salida.
     */

    @Override
    public List<PuenteVacacionDto> simularPuente(PuenteVacacionDto p,
                                                 long codEmpleadoEjecutor, boolean esAdmin) {
        return spHelper.ejecutarListado(
                SP_PUENTE, parametros(p, 0L, codEmpleadoEjecutor, esAdmin, 0L),
                "S", PuenteVacacionDto.class);
    }

    @Override
    public RespuestaSp aplicarPuente(PuenteVacacionDto p, long codUsuarioAutorizador,
                                     long codEmpleadoEjecutor, boolean esAdmin,
                                     long audUsuario) {
        log.info("Aplicando puente a cuenta de vacacion: sabado={}, {} - {}",
                 p.getIdSabado(), p.getHoraDesde(), p.getHoraHasta());
        return spHelper.ejecutarAbmMap(
                SP_PUENTE,
                parametros(p, codUsuarioAutorizador, codEmpleadoEjecutor, esAdmin, audUsuario),
                "A");
    }

    /**
     * Los parámetros del SP, en un Map explícito.
     *
     * <p>Map y no el DTO entero: el DTO lleva además las columnas que devuelve la
     * simulación (accion, dias, totales…), y ninguna de ésas es parámetro del SP.
     * Mandarlas haría fallar el EXEC, que arma la llamada con las claves tal cual.
     *
     * <p>Las horas y el motivo vacíos se OMITEN para que el SP aplique sus propios
     * defaults, en vez de recibir una cadena vacía que rebotaría como hora inválida.
     */
    private Map<String, Object> parametros(PuenteVacacionDto p, long codUsuarioAutorizador,
                                           long codEmpleadoEjecutor, boolean esAdmin,
                                           long audUsuario) {
        Map<String, Object> m = new HashMap<>();
        m.put("idSabado", p.getIdSabado());
        m.put("codEmpleadoEjecutor", codEmpleadoEjecutor);
        m.put("esAdmin", esAdmin ? 1 : 0);
        if (codUsuarioAutorizador > 0) m.put("codUsuarioAutorizador", codUsuarioAutorizador);
        if (audUsuario > 0)            m.put("audUsuario", audUsuario);
        if (noVacio(p.getHoraDesde())) m.put("horaDesde", p.getHoraDesde().trim());
        if (noVacio(p.getHoraHasta())) m.put("horaHasta", p.getHoraHasta().trim());
        if (noVacio(p.getMotivo()))    m.put("motivo", p.getMotivo().trim());
        return m;
    }

    private boolean noVacio(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
