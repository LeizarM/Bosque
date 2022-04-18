package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Formacion;

import java.util.List;

public interface IFormacion {

    /**
     * procedimiento que obtendra la formacion de un empleado
     */
    List<Formacion> obtenerFormacion( int codEmpleado );

}
