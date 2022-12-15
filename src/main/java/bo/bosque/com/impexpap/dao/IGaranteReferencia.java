package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.model.Dependiente;
import bo.bosque.com.impexpap.model.GaranteReferencia;



public interface IGaranteReferencia {

    /**
     * Metodo para listar los garantes o referencias de un empleado
     * @param codEmpleado
     * @return
     */
    List<GaranteReferencia> obtenerGaranteReferencia (int codEmpleado);

    /**
     * Para el abm de garante referencia
     * @param garRef
     * @param acc
     * @return true si lo hizo correctamente
     */
    boolean registrarGaranteReferencia(GaranteReferencia garRef, String acc );
}
