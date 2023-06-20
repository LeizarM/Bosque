package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CostoIncre;

import java.util.List;

public interface ICostoIncre {

    /**
     * Para registrar el costo incremento o costo de flete de transporte
     * @param costoIncre
     * @param acc
     * @return
     */
    boolean registrarCostoIncre( CostoIncre costoIncre, String acc );

    /**
     * Listara costo de flete de transporte
     * @return
     */
    List<CostoIncre> costoTransporteCiudad();
}
