package bo.bosque.com.impexpap.dao;
import java.util.List;
import bo.bosque.com.impexpap.model.LoteProduccion;



public interface ILoteProduccion {


    /**
     * Para registrar el lote de produccion
     * @param regLoteProduccion
     * @param acc
     * @return
     */
    boolean registrarLoteProduccion( LoteProduccion  regLoteProduccion, String acc );

    /**
     * Para obtener la loteProduccion ultimo
     * @return
     */
    List<LoteProduccion> obtenerLotesProduccionNew( int idMa );

    /**
     * Para obtener los articulos
     * @return
     */
    List<LoteProduccion> obtenerArticulos();

    List<LoteProduccion> obtenerDocNumXEmpresa( int codEmpresa );

}
