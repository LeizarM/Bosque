// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\VerificarCierreOperacionesDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO del flujo especial "Verificar Cierre de Operaciones" (idATR=5, el
 * paso supervisor) — reemplaza dlgRevArqueo en su modo idATR=5 de
 * WizardTareas.java. Las 3 acciones viven como ACCION='V' de los procs
 * genéricos p_abm_tac_ArqueoCajaSucursales, p_abm_tac_Llegada y
 * p_abm_tac_BitTareaRuti (no en procs con nombre propio — ver
 * sql/2026-09-03_tac_tareaRutinaria_24_flujos_especiales_sobre_procs_genericos.sql).
 * <p>
 * {@link #resolverCodSucursal} usa {@code jdbcTemplate} directo (no
 * {@code spHelper}, pensado para "execute p_proc") porque es un SELECT
 * plano de 2 pasos, sin proc propio — se agrega aquí y no como un nuevo SP
 * (ver memoria "no new SP names") replicando exactamente el mismo join que
 * p_list_tac_ArqueoCajaSucursales ya usa server-side para lo mismo.
 */
@Slf4j
@Repository
public class VerificarCierreOperacionesDao implements IVerificarCierreOperaciones {

    private final SpHelper spHelper;
    private final JdbcTemplate jdbcTemplate;

    public VerificarCierreOperacionesDao(SpHelper spHelper, JdbcTemplate jdbcTemplate) {
        this.spHelper = spHelper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RespuestaSp marcarArqueoRevisado(long idAC, int fueRevisado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idAC", idAC);
        params.put("audUsuario", audUsuario);
        // Quitar la marca es otra ACCION y no un parametro: un parametro nuevo
        // cambiaria la firma del SP, y las llamadas genericas (ejecutarAbm)
        // exigen todos los que declara (archivo SQL 71).
        return spHelper.ejecutarAbmMap("p_abm_tac_ArqueoCajaSucursales", params,
                fueRevisado == 0 ? "Q" : "V");
    }

    @Override
    public RespuestaSp marcarLlegadaVerificada(long idRp, int fueVerificado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idRp", idRp);
        params.put("fueVerificado", fueVerificado);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_Llegada", params, "V");
    }

    @Override
    public RespuestaSp confirmarVerificacion(long idBitTarea, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", idBitTarea);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_BitTareaRuti", params, "V");
    }

    @Override
    public int resolverCodSucursal(long idBitTarea, boolean todasSucursales) {
        if (todasSucursales) {
            return 0;
        }
        List<Long> empleados = jdbcTemplate.query(
                "SELECT codEmpleado FROM dbo.tac_bitTareaRuti WHERE idBitTarea = ?",
                (rs, rowNum) -> rs.getLong(1), idBitTarea);
        if (empleados.isEmpty()) {
            return 0;
        }
        List<Integer> sucursales = jdbcTemplate.query(
                "SELECT TOP 1 cs.codSucursal " +
                        "FROM dbo.trh_empleadoCargo ec " +
                        "JOIN dbo.tb_cargo_sucursal cs ON cs.codCargoSucursal = ec.codCargoSucursal " +
                        "WHERE ec.codEmpleado = ? " +
                        "ORDER BY ec.fechaInicio DESC, ec.codCargoSucursal DESC",
                (rs, rowNum) -> rs.getInt(1), empleados.get(0));
        return sucursales.isEmpty() ? 0 : sucursales.get(0);
    }
}
