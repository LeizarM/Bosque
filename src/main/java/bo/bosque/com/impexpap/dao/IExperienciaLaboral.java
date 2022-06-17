package bo.bosque.com.impexpap.dao;
import java.util.List;

import bo.bosque.com.impexpap.model.ExperienciaLaboral;



public interface IExperienciaLaboral {

    /**
     * Lista la experiencia laboral por empleado
     */
    List<ExperienciaLaboral> obtenerExperienciaLaboral( int codEmpleado );

    /**
     * Procedimiento para registrar de la experiencia laboral por empleado
     * @param exlab
     * @param acc
     * @return
     */
    boolean registrarExpLaboral(ExperienciaLaboral exlab, String acc);
}

