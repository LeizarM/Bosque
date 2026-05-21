package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Asientos;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IAsientos {

    /**
     * Registra, actualiza o elimina un asiento contable.
     * @param asiento  Objeto con los datos del asiento
     * @param acc      Acción ('I'=Insert, 'U'=Update, 'D'=Delete)
     */
    RespuestaSp registrarAsiento(Asientos asiento, String acc);

    /**
     * [T] Lista todos los asientos de una transacción.
     * @param idTransaccion ID de la transacción
     */
    List<Asientos> obtenerAsientosPorTransaccion(long idTransaccion);

    /**
     * [V] Retorna el resumen de cuadre de los asientos de una transacción.
     * Contiene totales de débito/crédito y el estadoCuadre ("CUADRADO"/"DESCUADRADO").
     * @param idTransaccion ID de la transacción
     */
    Asientos validarCuadre(long idTransaccion);
}
