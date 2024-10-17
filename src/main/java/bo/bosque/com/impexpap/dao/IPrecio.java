package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Precio;
import java.util.List;

public interface IPrecio {

    /**
     * Para registrar el precio
     * @param precio
     * @param acc
     * @return
     */
    boolean registrarPrecio( Precio precio, String acc );

    /**
     * Para Listar las precios en toneladas actuales para poder calcular
     * @param codigoFamilia
     * @return
     */
    List<Precio> listPrecioToneladasActuales( int codigoFamilia );
}
