package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.MaterialSalida;

public interface IMaterialSalida {

    /**
     * Metodo para registrar material ingreso
     * @param regMatIng
     * @param acc
     * @return
     */
    boolean registrarMaterialSalida(MaterialSalida regMatSal, String acc );
}
