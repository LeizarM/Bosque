package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Cargo;

import java.util.List;

public interface ICargo {
    /**
     * \Obtendra los cargos por empresa
     * @param codEmpresa
     * @return
     */
    List<Cargo> obtenerCargoXEmpresa(int codEmpresa );
}
