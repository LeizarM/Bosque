package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TransaccionParticipantes;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ITransaccionParticipantes {

    /**
     * Registra, actualiza o elimina un participante del split de una transacción.
     * @param participante  Objeto con los datos del participante
     * @param acc           Acción ('I'=Insert, 'U'=Update, 'D'=Delete)
     */
    RespuestaSp registrarParticipante(TransaccionParticipantes participante, String acc);

    /**
     * [T] Lista todos los participantes de una transacción.
     * @param idTransaccion ID de la transacción
     */
    List<TransaccionParticipantes> obtenerParticipantesPorTransaccion(long idTransaccion);

    /**
     * [V] Retorna el resumen de cuadre del split de una transacción.
     * Compara Σ montoUs vs montoConvertido y suma de porcentajes;
     * estadoCuadre = "CUADRADO" / "DESCUADRADO".
     * @param idTransaccion ID de la transacción
     */
    TransaccionParticipantes validarCuadre(long idTransaccion);
}
