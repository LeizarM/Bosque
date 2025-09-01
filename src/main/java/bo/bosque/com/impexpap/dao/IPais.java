package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Pais;

import java.util.List;

public interface IPais {

    /**
     * Obtendra los paises registrados
     * @return
     */
    List<Pais> obtenerPais();
    /**
     * Procedimiento para registrar pais
     * @param
     * @param
     * @return
     */
    boolean registrarPais(Pais pais, String acc);
}
