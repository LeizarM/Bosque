package bo.bosque.com.impexpap.dao;
import java.util.List;
import bo.bosque.com.impexpap.model.ArticuloPropuesto;

public interface IArticuloPropuesto {

    /**
     * Para registrar el Articulo propuesto
     * @param articuloPropuesto
     * @param acc
     * @return
     */
    boolean registrarArticuloPropuesto( ArticuloPropuesto articuloPropuesto, String acc );


    /**
     * Metodo para listar los articulos por familia
     * @param codCad
     * @return
     */
    List<ArticuloPropuesto> listarArticulosXFamilia( String codCad );

    /**
     * Para listar los articulos que se van a cambiar de precios por cada propuesta
     * @param idPropuesta
     * @return
     */
    List<ArticuloPropuesto> listarArticulosPropuesta (  int idPropuesta );
}
