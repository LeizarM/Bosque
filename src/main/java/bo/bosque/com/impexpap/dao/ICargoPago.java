package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CargoPago;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ICargoPago {

    /**
     * Registrar, actualizar o eliminar un Cargo de Pago
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrarCargoPago(CargoPago mb, String acc);

    /**
     * Obtener cargos de pago por ID. Si idCargo == 0 devuelve todos.
     * @param idCargo ID a buscar (0 = todos)
     */
    List<CargoPago> obtenerCargoPago(long idCargo);

    /**
     * Obtener cargos de pago asociados a una cotización
     * @param idCotizacion ID de la cotización
     */
    List<CargoPago> obtenerCargoPorCotizacion(long idCotizacion);

    /**
     * Obtener cargos de pago asociados a una transacción
     * @param idTransaccion ID de la transacción
     */
    List<CargoPago> obtenerCargoPorTransaccion(long idTransaccion);
}

