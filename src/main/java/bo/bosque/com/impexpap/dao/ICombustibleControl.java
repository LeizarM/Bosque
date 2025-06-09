package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.CombustibleControl;

import java.util.List;

public interface ICombustibleControl {


    boolean registrarCombustibleControl( CombustibleControl mb, String acc );


    List<CombustibleControl> listarCoches();


    List<CombustibleControl> listarCochesKilometraje( int idCoche );

    List<CombustibleControl> esConsumoBajo( float kilometraje, int idCoche );
}
