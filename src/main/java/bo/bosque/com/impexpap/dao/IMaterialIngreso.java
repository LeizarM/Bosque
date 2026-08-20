package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.MaterialIngreso;

public interface IMaterialIngreso {

    /**
     * Metodo para registrar el material de entrada
     * @param regMatIng
     * @param acc
     * @return
     */
    boolean registrarMaterialIngreso( MaterialIngreso regMatIng, String acc );

    /**
     * Para obtener el material de ingreso de un lote
     * @param idLp
     * @return
     */
    java.util.List<MaterialIngreso> obtenerMaterialIngresoXLote( int idLp );
}
