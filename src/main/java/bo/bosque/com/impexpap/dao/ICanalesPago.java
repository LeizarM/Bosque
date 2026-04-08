package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CanalesPago;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ICanalesPago {

    /**
     * Registrar, actualizar o eliminar un Canal de Pago
     * @param mb   Objeto con los datos del canal
     * @param acc  Acción ('I', 'U', 'D')
     * @return RespuestaSp con error, errormsg e idGenerado
     */
    RespuestaSp registrarCanalesPago(CanalesPago mb, String acc);

    /**
     * Obtener canales de pago. Si idCanal == 0 devuelve todos.
     * @param idCanal ID a buscar (0 = todos)
     * @return Lista de CanalesPago
     */
    List<CanalesPago> obtenerCanalesPago(long idCanal);
}

