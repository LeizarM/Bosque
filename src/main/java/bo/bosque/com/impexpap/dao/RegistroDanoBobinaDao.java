package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RegistroDanoBobina;
import bo.bosque.com.impexpap.model.RegistroResma;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RegistroDanoBobinaDao  implements IRegistroDanoBobina {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    /**
     * Obtiene la lista de los registros de Bobina que están entrando por empresa
     *
     * @param codEmpresa
     * @return
     */
    @Override
    public List<RegistroDanoBobina> lstEntradaDeMercaderiasBob(int codEmpresa) {
        return null;
    }

    /**
     * Obtiene la lista de artculos por numero de documento y empresa
     *
     * @param codEmpresa
     * @param docNum
     * @return List
     */
    @Override
    public List<RegistroDanoBobina> lstArticuloXEntradaBob(int codEmpresa, int docNum) {
        return null;
    }
}
