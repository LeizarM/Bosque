package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Transacciones;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class TransaccionesDao implements ITransacciones {

    private final SpHelper spHelper;

    public TransaccionesDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarTransacciones(Transacciones mb, String acc) {

        log.info("Registrando Transacciones: {}, Accion: {}", mb.toString(), acc);


        return spHelper.ejecutarAbm("p_abm_tpex_Transacciones", mb, acc);
    }

    // ── L: Grilla de transacciones de una solicitud ────────────────────────
    // FIX: Usa Map para evitar el problema 0 vs NULL.
    // El SP usa IS NULL como "sin filtro"; si enviamos 0 (default de primitivos)
    // el SP lo interpreta como un filtro real y puede devolver vacío.
    // codEmpresa siempre se envía porque el SP lo necesita para poblar @tempProv.
    @Override
    public List<Transacciones> obtenerTransacciones(long idSolicitud, String cardCode,
                                                    int codBanco, String estado,
                                                    long idTipoTransaccion, long codEmpresa) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpresa", codEmpresa);                         // requerido para @tempProv
        if (idSolicitud       != 0)  filtro.put("idSolicitud",       idSolicitud);
        if (codBanco          != 0)  filtro.put("codBanco",          codBanco);
        if (idTipoTransaccion != 0)  filtro.put("idTipoTransaccion", idTipoTransaccion);
        if (cardCode          != null && !cardCode.isEmpty()) filtro.put("cardCode", cardCode);
        if (estado            != null && !estado.isEmpty())   filtro.put("estado",   estado);
        return spHelper.ejecutarListado("p_list_tpex_Transacciones", filtro, "L", Transacciones.class);
    }

    // ── R: Registro completo (formulario) ──────────────────────────────────
    @Override
    public Transacciones obtenerTransaccion(long idTransaccion, long codEmpresa) {
        Transacciones filtro = new Transacciones();
        filtro.setIdTransaccion(idTransaccion);
        filtro.setCodEmpresa(codEmpresa);
        List<Transacciones> resultado = spHelper.ejecutarListado(
                "p_list_tpex_Transacciones", filtro, "R", Transacciones.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    /**
     * [R] Carga UNA transacción por ID exacto (para load-before-update).
     * Usa el overload Map del SpHelper para enviar SOLO @idTransaccion.
     */
    @Override
    public Transacciones obtenerTransaccionPorId(long idTransaccion,  long codEmpresa) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTransaccion", idTransaccion);
        filtro.put("codEmpresa", codEmpresa);
        List<Transacciones> resultado = spHelper.ejecutarListado(
                "p_list_tpex_Transacciones", filtro, "R", Transacciones.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    // ── C: Transacciones vinculadas a una cotización ─────────────────────
    // IMPORTANTE: el SP debe implementar la acción 'C' con el bloque:
    //   IF @ACCION = 'C' BEGIN
    //       SELECT t.* ... WHERE t.idCotizacion = @idCotizacion
    //   END
    // Mientras el SP no tenga esa acción, este método siempre retorna vacío.
    @Override
    public List<Transacciones> obtenerTransaccionesPorCotizacion(long idCotizacion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCotizacion", idCotizacion);
        return spHelper.ejecutarListado("p_list_tpex_Transacciones", filtro, "C", Transacciones.class);
    }

    // ── B: Reporte entre fechas ────────────────────────────────────────────
    // FIX: Usa Map para evitar el problema 0 vs NULL.
    // Si idTipoTransaccion llega como 0 (no enviado) y se serializa el modelo
    // completo, el SP filtraría t.idTipoTransaccion = 0 en lugar de "todos".
    // codEmpresa siempre se envía porque el SP lo necesita para poblar @tempProv.
    @Override
    public List<Transacciones> obtenerTransaccionesReporte(Date fechaInicio, Date fechaFin,
                                                           String cardCode, String estado,
                                                           long idTipoTransaccion, long codEmpresa) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpresa", codEmpresa);                         // requerido para @tempProv
        if (fechaInicio       != null)        filtro.put("fechaInicio",       fechaInicio);
        if (fechaFin          != null)        filtro.put("fechaFin",          fechaFin);
        if (cardCode          != null && !cardCode.isEmpty()) filtro.put("cardCode", cardCode);
        if (estado            != null && !estado.isEmpty())   filtro.put("estado",   estado);
        if (idTipoTransaccion != 0)           filtro.put("idTipoTransaccion", idTipoTransaccion);

        log.info("Obteniendo reporte de transacciones con filtro: {}", filtro);

        return spHelper.ejecutarListado("p_list_tpex_Transacciones", filtro, "B", Transacciones.class);
    }

    // ── U-parcial: Actualizar solo rutaVoucher ─────────────────────────────
    // Usa Map para enviar ÚNICAMENTE los campos necesarios.
    // El SP recibe el resto de parámetros en NULL → no se pisan otros campos.
    @Override
    public RespuestaSp actualizarVoucher(long idTransaccion, String rutaVoucher, int audUsuario) {
        log.info("Actualizando voucher de transacción ID={}, ruta={}", idTransaccion, rutaVoucher);
        Map<String, Object> params = new HashMap<>();
        params.put("idTransaccion", idTransaccion);
        params.put("rutaVoucher",   rutaVoucher);
        params.put("audUsuario",    audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tpex_Transacciones", params, "U");
    }
}
