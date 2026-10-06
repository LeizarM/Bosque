// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\CierreOperacionesDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.ChequeCierreDto;
import bo.bosque.com.impexpap.dto.ConfirmarCierreOperacionesRequest;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO del flujo especial "Cierre de Operaciones" (idATR=3, el paso doer) —
 * reemplaza el panel plMovCaja de WizardTareas.java. La lógica vive como
 * ACCION='C' del proc genérico p_abm_tac_BitTareaRuti (no en un proc con
 * nombre propio — ver
 * sql/2026-09-03_tac_tareaRutinaria_24_flujos_especiales_sobre_procs_genericos.sql
 * y sql/2026-09-03_tac_tareaRutinaria_27_cierre_verificar_cajachica_parity.sql
 * para el agregado de fecha + verificación por fila).
 * El listado de traspasos del día reutiliza ITraspasoMovCaja (CRUD estándar)
 * filtrado por fecha — no hace falta un DAO de listado aparte.
 */
@Slf4j
@Repository
public class CierreOperacionesDao implements ICierreOperaciones {

    private final SpHelper spHelper;

    public CierreOperacionesDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp confirmarTraspasos(ConfirmarCierreOperacionesRequest req, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", req.getIdBitTarea());
        params.put("fecha", req.getFecha());
        // 2026-09-10: ya no se manda traspasosXml. La verificacion de traspasos
        // dejo de ser parte de Cierre de Operaciones y paso a la tarea 289
        // ("Verificar Traspaso de Efectivo Entre Sistemas", idATR 11).
        //
        // El SP tambien dejo de mirar ese parametro (archivo SQL 50). Los dos
        // cambios van juntos: mientras el SP siguiera con la rama de "reclamo
        // en bloque", cerrar Cierre de Operaciones se llevaba puestos los
        // traspasos que el cajero acababa de verificar desde su propia tarea.
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_BitTareaRuti", params, "C");
    }

    // @audUsuario no viaja, igual que en ValeDao.lstChequeCO: el SP tiene su
    // propio DEFAULT. El PDF (subRptChequesCO) sí lo manda, con 34 fijo.
    @Override
    public List<ChequeCierreDto> cheques(Date fecha) {
        Map<String, Object> params = new HashMap<>();
        params.put("fechaBusq", fecha);
        return spHelper.ejecutarListado("p_SAP_Rpt_ImpChequesPR", params, "A", ChequeCierreDto.class);
    }
}
