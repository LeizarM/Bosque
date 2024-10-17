package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ArticuloPrecioDisponible;


import java.util.List;

public interface IArticuloPrecioDisponible {



    /**
     * Para obtener los articulos de IPX y ESPP
     * @return
     */
    List<ArticuloPrecioDisponible> obtenerArticulosIPXyESPP( int codCiudad );

    /**
     * Para obtener los almacenes por item y su disponibilidad
     * @param codArticulo
     * @param codCiudad
     * @return
     */
    List<ArticuloPrecioDisponible> obtenerAlmacenXItem( String codArticulo, int codCiudad  );

}
