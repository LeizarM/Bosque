package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DetalleSolicitud;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DetalleSolicitudDao implements IDetalleSolicitud {



    private final SpHelper spHelper;

    public DetalleSolicitudDao( SpHelper spHelper ) {
        this.spHelper = spHelper;
    }




    /**
     * Procedimiento para registrar las Detalle de Solicitudes Proveedor
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public RespuestaSp registrarDetalleSolicitud(DetalleSolicitud mb, String acc) {

        return this.spHelper.ejecutarAbm("p_abm_tpex_DetalleSolicitud", mb, acc);

    }

    /**
     * Procedimiento para obtener las DetalleSolicitud de Pago por Id
     *
     * @param idDetalle
     * @return
     */
    @Override
    public List<DetalleSolicitud> obtenerDetalleSolicitud( long idDetalle ) {

        // Map: el modelo tiene campos (numeroCuota, montoTotalDocumento) que el SP no recibe.
        Map<String, Object> filtro = new HashMap<>();
        filtro.put( "idDetalle", idDetalle );

        return spHelper.ejecutarListado(
                "p_list_tpex_DetalleSolicitud",
                filtro,
                "L",
                DetalleSolicitud.class
        );


    }

    /**
     * Procedimiento para obtener las ordenes de compra por empresa y facturas proveedor
     *
     * @param codEmpresa
     * @return
     */
    @Override
    public List<DetalleSolicitud> obtenerFacProvYOrdCompraXEmpresa( int codEmpresa ) {
        // Map: el modelo tiene campos (numeroCuota, montoTotalDocumento) que el SP no recibe.
        Map<String, Object> filtro = new HashMap<>();
        filtro.put( "codEmpresa", codEmpresa );

        return spHelper.ejecutarListado(
                "p_list_tpex_DetalleSolicitud",
                filtro,
                "A",
                DetalleSolicitud.class
        );
    }
}
