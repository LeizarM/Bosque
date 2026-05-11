package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Asientos;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class AsientosDao implements IAsientos {

    private final SpHelper spHelper;

    public AsientosDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarAsiento(Asientos asiento, String acc) {
        log.info("Registrando Asiento: {}, Accion: {}", asiento.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tpex_Asientos", asiento, acc);
    }

    // ── T: Asientos de una transacción ────────────────────────────────────
    // Usa Map para enviar SOLO @idTransaccion. Si se serializa el modelo completo,
    // los primitivos en 0 (idAsiento, numero, etc.) podrían filtrar resultados.
    @Override
    public List<Asientos> obtenerAsientosPorTransaccion(long idTransaccion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTransaccion", idTransaccion);
        return spHelper.ejecutarListado("p_list_tpex_Asientos", filtro, "T", Asientos.class);
    }

    // ── V: Resumen de cuadre de una transacción ───────────────────────────
    @Override
    public Asientos validarCuadre(long idTransaccion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTransaccion", idTransaccion);
        List<Asientos> resultado = spHelper.ejecutarListado("p_list_tpex_Asientos", filtro, "V", Asientos.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
