package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RegistroDanoBobina;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RegistroDanoBobinaDao  implements IRegistroDanoBobina {


    /**
     * Registrar un registro de daño de bobina
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarRegistroDanoBobina(RegistroDanoBobina mb, String acc) {
        return false;
    }

    /**
     * Listara el registro de daños de bobinas
     *
     * @return
     */
    @Override
    public List<RegistroDanoBobina> lstRegistroDanoBobina() {
        return null;
    }
}
