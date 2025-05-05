package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ControCombustibleMaquinaMontacarga;

import java.util.List;

public interface IControCombustibleMaquinaMontacarga {

    boolean registrarControlCombustible( ControCombustibleMaquinaMontacarga mb, String acc );

    List<ControCombustibleMaquinaMontacarga> lstAlmacenes();

    List<ControCombustibleMaquinaMontacarga> lstBidonesXMaquina( int idMaquina );
}
