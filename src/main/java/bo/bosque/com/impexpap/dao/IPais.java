package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Pais;

import java.util.List;

public interface IPais {

    /**
     * Obtendra los paises registrados
     * @return
     */
    List<Pais> obtenerPais();
}
