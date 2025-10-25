package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Cargo;

import java.util.List;

public interface ICargo {
    /**
     * Obtendra los cargos por empresa
     * @param codEmpresa
     * @return
     */
    List<Cargo> obtenerCargoXEmpresa( int codEmpresa );

    /**
     * Obtendra los cargos por empresa pero de forma mas informativa
     * @param codEmpresa
     * @return
     */
    List<Cargo> obtenerCargoXEmpresaNew ( int codEmpresa );

    /**
     * Registar / Actualizar un cargo
     * @param cargo
     * @return
     */
    boolean registrarCargo( Cargo cargo, String acc );
}
