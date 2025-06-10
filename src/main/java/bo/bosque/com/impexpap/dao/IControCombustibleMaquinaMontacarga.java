package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ControCombustibleMaquinaMontacarga;

import java.util.Date;
import java.util.List;

public interface IControCombustibleMaquinaMontacarga {

    boolean registrarControlCombustible( ControCombustibleMaquinaMontacarga mb, String acc );

    List<ControCombustibleMaquinaMontacarga> lstAlmacenes();

    List<ControCombustibleMaquinaMontacarga> lstRptMovBidonesXTipoTransaccion( Date fechaInicio, Date fechaFin, int codSucursal );

    List<ControCombustibleMaquinaMontacarga> saldoActualCombustinbleXSucursal();

    List<ControCombustibleMaquinaMontacarga> lstUltimosMovBidones( );

    List<ControCombustibleMaquinaMontacarga> ontenerBidonesXSucursal( int codSucursalMaqVehiDestino );

    List<ControCombustibleMaquinaMontacarga> obtenerDetalleBidon( long idCM );

}
