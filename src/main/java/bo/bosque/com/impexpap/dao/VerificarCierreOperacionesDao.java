// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\VerificarCierreOperacionesDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
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
 * {@link #resolverCodSucursal} es la 'S' de p_list_tac_BitTareaRuti (archivo SQL 112):
 * antes eran dos SELECT armados aquí.
 */
@Slf4j
@Repository
public class VerificarCierreOperacionesDao implements IVerificarCierreOperaciones {

    private final SpHelper spHelper;

    public VerificarCierreOperacionesDao(SpHelper spHelper) {
        this.spHelper = spHelper;
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
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", idBitTarea);
        params.put("ACCION", "S");
        List<Map<String, Object>> filas;
        try {
            filas = spHelper.ejecutarListadoDinamico("p_list_tac_BitTareaRuti", params);
        } catch (RuntimeException e) {
            log.error("p_list_tac_BitTareaRuti S falló (¿falta el SQL 112?): {}", e.getMessage());
            throw new IllegalStateException("Falta correr el SQL 112 (sucursal de la ocurrencia) en la base.", e);
        }
        // La 'S' siempre devuelve una fila (0 = sin resolver): sin filas, falta el SQL 112.
        // No se cae a 0: 0 es "todas las sucursales" y el PDF mostraría las de otros.
        if (filas.isEmpty()) {
            throw new IllegalStateException("Falta correr el SQL 112 (sucursal de la ocurrencia) en la base.");
        }
        Object codSucursal = filas.get(0).get("codSucursal");
        return codSucursal instanceof Number ? ((Number) codSucursal).intValue() : 0;
    }
}
