package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TransaccionParticipantes;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class TransaccionParticipantesDao implements ITransaccionParticipantes {

    private final SpHelper spHelper;

    public TransaccionParticipantesDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarParticipante(TransaccionParticipantes participante, String acc) {
        log.info("Registrando Participante: {}, Accion: {}", participante.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tpex_TransaccionParticipantes", participante, acc);
    }

    // ── T: Participantes de una transacción ───────────────────────────────
    // Usa Map para enviar SOLO @idTransaccion. Si se serializa el modelo completo,
    // los primitivos en 0 (idParticipante, porcentaje, etc.) podrían filtrar resultados.
    @Override
    public List<TransaccionParticipantes> obtenerParticipantesPorTransaccion(long idTransaccion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTransaccion", idTransaccion);
        return spHelper.ejecutarListado("p_list_tpex_TransaccionParticipantes", filtro, "T", TransaccionParticipantes.class);
    }

    // ── V: Resumen de cuadre del split de una transacción ─────────────────
    @Override
    public TransaccionParticipantes validarCuadre(long idTransaccion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTransaccion", idTransaccion);
        List<TransaccionParticipantes> resultado = spHelper.ejecutarListado("p_list_tpex_TransaccionParticipantes", filtro, "V", TransaccionParticipantes.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
