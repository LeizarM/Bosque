package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CostoIncre;
import bo.bosque.com.impexpap.model.RegistroDanoBobina;
import bo.bosque.com.impexpap.model.RegistroResma;

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


    /**
     * Obtiene la lista de los registros de Bobina que están entrando por empresa
     * @param codEmpresa
     * @return
     */
    List<RegistroDanoBobina> lstEntradaDeMercaderiasBob(int codEmpresa );

    /**
     * Obtiene la lista de artculos por numero de documento y empresa
     * @param codEmpresa
     * @param docNum
     * @return
     */
    List<RegistroDanoBobina> lstArticuloXEntradaBob( int codEmpresa, int docNum );

}
