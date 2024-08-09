package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CostoIncre;
import bo.bosque.com.impexpap.model.RegistroResmaDetalle;

import java.util.List;

public interface IRegistroResmaDetalle {


    /**
     * Metodo para registrar un registro de resma detalle
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarRegistroResmaDetalle( RegistroResmaDetalle mb, String acc );

    /**
     * Metodo para listar los registros de resma detalle
     * @return
     */
    List<RegistroResmaDetalle> lstRegistroResmaDetalle();
}
