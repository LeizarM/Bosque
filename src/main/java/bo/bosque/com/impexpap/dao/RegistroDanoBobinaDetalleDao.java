package bo.bosque.com.impexpap.dao;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RegistroDanoBobinaDetalleDao implements  IRegistroDanoBobinaDetalle {




    /**
     * Registra el detalle de un registro de daño de bobina
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarRegistroDanoBobinaDetalle(bo.bosque.com.impexpap.model.RegistroDanoBobinaDetalle mb, String acc) {
        return false;
    }

    /**
     * Obtiene la lista de todos los detalles de registro de daño de bobina
     *
     * @return
     */
    @Override
    public List<bo.bosque.com.impexpap.model.RegistroDanoBobinaDetalle> lstRegistroDanoBobinaDetalle() {
        return null;
    }
}
