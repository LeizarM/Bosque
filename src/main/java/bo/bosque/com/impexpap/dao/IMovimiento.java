package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ControCombustibleMaquinaMontacarga;
import bo.bosque.com.impexpap.model.Movimiento;

import java.util.Date;
import java.util.List;

public interface IMovimiento {

    boolean registrarMovimiento( Movimiento mb, String acc );

    List<Movimiento> listarMovimientosXFechas( Date fechaInicio, Date fechaFin , int codSucursal, int idTipo );

    List<Movimiento> lstSaldosActuales();

    /**
     * Listara los bidones pendientes antes de registrar una compra de combustible
     * @param sucursalDestino
     * @return
     */
    List<Movimiento> obtenerBidonesXSucursal(int sucursalDestino );

    /**
     * Listara el detalle de un bidón según el id de movimiento
     * @param idMovimiento
     * @return
     */
    List<Movimiento> obtenerDetalleBidon( long idMovimiento );
}
