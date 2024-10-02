package bo.bosque.com.impexpap.dao;
import java.util.List;
import bo.bosque.com.impexpap.model.Telefono;

public interface ITelefono {

    /**
     * Procedimiento para listar los telefonos por persona
     * @param codPersona
     * @return
     */
    List<Telefono> obtenerTelefonos(int codPersona );


    /**
     * Procedimiento para registrar el Telefono
     * @param tel
     * @param acc
     * @return
     */
    boolean registrarTelefono( Telefono tel ,String acc );

    /**
     * Procedimiento para obtener el ultimo codigo de persona registrado
     * @param audUsuario
     * @return
     */
    int obtenerUltimoCodPersona( int audUsuario );
}
