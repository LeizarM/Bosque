package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CostoIncre;
import bo.bosque.com.impexpap.model.RegistroDanoBobina;

import java.util.List;

public interface IRegistroDanoBobina {

    /**
     * Registrar un registro de daño de bobina
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarRegistroDanoBobina( RegistroDanoBobina mb, String acc );

    /**
     * Listara el registro de daños de bobinas
     * @return
     */
    List<RegistroDanoBobina> lstRegistroDanoBobina();
}
