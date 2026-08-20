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

    /**
     * Para obtener el material de salida de un lote
     * @param idLp
     * @return
     */
    java.util.List<MaterialSalida> obtenerMaterialSalidaXLote( int idLp );
}
