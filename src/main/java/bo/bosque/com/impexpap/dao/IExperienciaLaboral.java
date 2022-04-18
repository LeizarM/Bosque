package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ExperienciaLaboral;

import java.util.List;

public interface IExperienciaLaboral {

    /**
     * Lista la experiencia laboral por empleado
     */
    List<ExperienciaLaboral> obtenerExperienciaLaboral( int codEmpleado );
}
