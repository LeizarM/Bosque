package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Producto;

import java.util.List;

public interface IProducto {

    /**
     * Para registrar el producto o familia
     * @param producto
     * @param acc
     * @return
     */
    boolean registrarProducto( Producto producto, String acc );

    /**
     * Metdodo que devolvera los preveedores del SAP registrados en el bosque
     * @return
     */
    List<Producto> listadoProveedor();

    /**
     * Metodo que devolvera la lista de familias
     * @return
     */
    List<Producto> listadoFamilia( int codFamilia );

    /**
     * Metodo que devolvera la lista de todos las familias que pertenecen a un grupo de familia del sap
     * @param idGrpFamiliaSap
     * @return
     */
    List<Producto> listadoFamiliaXGrupo( int idGrpFamiliaSap );

    /**
     * Devolvera un objecto de acuerrdo al codigo de familia
     * @param codigoFamilia
     * @return
     */
    Producto cargarDatoFamilia( int codigoFamilia );
}
