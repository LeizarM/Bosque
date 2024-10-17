package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.GrupoFamiliaSap;

public interface IGrupoFamiliaSap {

    /**
     * Para registrar el grupo de familia SAP
     * @param grupoFamiliaSap
     * @param acc
     * @return
     */
    boolean registrarGrupoFamiliaSap ( GrupoFamiliaSap grupoFamiliaSap, String acc );


}
