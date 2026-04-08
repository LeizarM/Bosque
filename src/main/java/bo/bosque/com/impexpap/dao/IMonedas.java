package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Monedas;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IMonedas {

    /**
     * Registrar, actualizar o eliminar una Moneda
     * @param mb   Objeto con los datos de la moneda
     * @param acc  Acción ('I', 'U', 'D')
     * @return RespuestaSp con error, errormsg e idGenerado
     */
    RespuestaSp registrarMonedas(Monedas mb, String acc);

    /**
     * Obtener monedas. Si idMoneda == 0 devuelve todas.
     * @param idMoneda ID a buscar (0 = todas)
     * @return Lista de Monedas
     */
    List<Monedas> obtenerMonedas(long idMoneda);
}

