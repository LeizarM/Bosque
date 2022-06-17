package bo.bosque.com.impexpap.dao;
import java.util.List;
import bo.bosque.com.impexpap.model.Formacion;



public interface IFormacion {

    /**
     * procedimiento que obtendra la formacion de un empleado
     * @param codEmpleado
     * @return
     */
    List<Formacion> obtenerFormacion( int codEmpleado );

    /**
     * Procecimiento para el abm
     * @param fr
     * @param acc
     * @return
     */
    boolean registrarFormacion( Formacion fr, String acc );

}
