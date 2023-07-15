package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Propuesta;

public interface IPropuesta {
    /**
     * para registrar la propuesta
     * @param propuesta
     * @param acc
     * @return
     */
    boolean registrarPropuesta(Propuesta propuesta, String acc );

    /**
     * Metodo para obtener el ultimo id de la ultima propuesta registrada
     * @param audUsuario
     * @return
     */
    int ultimaIdPropuesta ( int audUsuario );

}
