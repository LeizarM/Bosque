package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ArticuloPrecioDisponible;


import java.util.List;

public interface IArticuloPrecioDisponible {



    /**
     * Para obtener los articulos de IPX y ESPP
     * @return
     */
    List<ArticuloPrecioDisponible> obtenerArticulosIPXyESPP( int codCiudad );

}
