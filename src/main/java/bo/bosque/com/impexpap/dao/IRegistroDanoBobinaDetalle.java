package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.RegistroDanoBobinaDetalle;

import java.util.List;

public interface IRegistroDanoBobinaDetalle {


    /**
     * Registra el detalle de un registro de daño de bobina
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarRegistroDanoBobinaDetalle( RegistroDanoBobinaDetalle mb, String acc );

    /**
     * Obtiene la lista de todos los detalles de registro de daño de bobina
     * @return
     */
    List<RegistroDanoBobinaDetalle> lstRegistroDanoBobinaDetalle();

}
