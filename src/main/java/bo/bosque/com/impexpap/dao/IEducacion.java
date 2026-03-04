package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Educacion;

import java.util.List;

public interface IEducacion {
    /**
     * PROCEDIMIENTO PARA REGISTRAR EDUCACION
     * @param ed
     * @param acc
     * @return
     */
    boolean registrarEducacion(Educacion ed , String acc);

    /**
     * OBTENDRA UNA LISTA DE EDUCACION
     * @param codEmpleado
     * @return
     */
    List<Educacion> obtenerEducacion(int codEmpleado);


}
