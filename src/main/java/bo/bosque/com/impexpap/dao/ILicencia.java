package bo.bosque.com.impexpap.dao;
import java.util.List;
import bo.bosque.com.impexpap.model.Licencia;



public interface ILicencia {

    /**
     * Procedimiento para obtener las licencia de conducir de una persona
     * @param codPersona
     * @return
     */
    List<Licencia> obtenerLicencia( int codPersona );

    /**
     * Procedimiento para el abm de la licencia de conducir
     * @param lc
     * @param acc
     * @return
     */
    boolean registrarLicencia( Licencia  lc, String acc );
}
