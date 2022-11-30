package bo.bosque.com.impexpap.dao;

import java.util.List;
import bo.bosque.com.impexpap.model.GaranteReferencia;



public interface IGaranteReferencia {

    /**
     * Metodo para listar los garantes o referencias de un empleado
     * @param codEmpleado
     * @return
     */
    List<GaranteReferencia> obtenerGaranteReferencia (int codEmpleado);
}
