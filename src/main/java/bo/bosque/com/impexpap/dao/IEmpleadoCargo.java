package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.EmpleadoCargo;


public interface IEmpleadoCargo {

    /**
     * Procedimiento para el registro de informacion
     * @param empCar
     * @param acc
     * @return
     */
    boolean registrarEmpleadoCargo (EmpleadoCargo empCar, String acc);
}
