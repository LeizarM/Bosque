package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RegistroDanoBobinaDetalle;
import bo.bosque.com.impexpap.model.RegistroResma;

import java.util.List;

public interface IRegistroResma {


    /**
     * Registra un nuevo registro de resma
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarRegistroResma( RegistroResma mb, String acc );

    /**
     * Obtiene la lista de todos los registros de resma
     * @return
     */
    List<RegistroResma> lstRegistroResma();


}
