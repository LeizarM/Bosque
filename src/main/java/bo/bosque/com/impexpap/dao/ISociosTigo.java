package bo.bosque.com.impexpap.dao;
import bo.bosque.com.impexpap.model.FacturaTigo;
import bo.bosque.com.impexpap.model.SociosTigo;
import java.util.List;

public interface ISociosTigo {
    /**
     * Procecimiento para obtener los socios TIGO
     * @param
     * @return
     */
    List<SociosTigo> obtenerSociosTIGO( );
    /**
     * Procedimiento que registra los socios TIGO
     * @param st
     * @return
     */
    boolean registrarSociosTigo (SociosTigo st, String acc);
    /**
     * Procecimiento para obtener los socios TIGO
     * @param
     * @return
     */
    List<SociosTigo> obtenerListaGruposTigo(String periodoCobrado );
    /**
     * Procecimiento para obtener los socios TIGO
     * @param
     * @return
     */
    List<SociosTigo> obtenerNumerosSinAsignar(String periodoCobrado );

}
