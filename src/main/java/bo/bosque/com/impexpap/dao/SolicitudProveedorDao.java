package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SolicitudProveedor;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SolicitudProveedorDao implements ISolicitudProveedor {


    private final SpHelper spHelper;

    public SolicitudProveedorDao( SpHelper spHelper ) {
        this.spHelper = spHelper;
    }


    /**
     * Procedimiento para registrar las SolicitudesProveedor de Pago
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public RespuestaSp registrarSolicitudProveedor( SolicitudProveedor mb, String acc) {

        return this.spHelper.ejecutarAbm("p_abm_tpex_SolicitudProveedor", mb, acc);

    }

    /**
     * Procedimiento para obtener las SolicitudesProveedor de Pago por Id
     *
     * @param idSolicitudProveedor
     * @return
     */
    @Override
    public List<SolicitudProveedor> obtenerSolicitudProveedor( long idSolicitudProveedor ) {


        SolicitudProveedor filtro = new SolicitudProveedor();
        filtro.setIdSolicitudProveedor( idSolicitudProveedor );

        // 2. Ejecutamos el listado.

        return spHelper.ejecutarListado(
                "p_list_tpex_SolicitudProveedor",
                filtro,
                "L",
                SolicitudProveedor.class
        );
    }

    /**
     * Para obtener los proveedores SAP por empresa
     *
     * @param codEmpresa
     * @return
     */
    @Override
    public List<SolicitudProveedor> obtenerProveedoresXEmpresa( int codEmpresa ) {

        // 1. Creamos el filtro.
        SolicitudProveedor filtro = new SolicitudProveedor();
        filtro.setCodEmpresa( codEmpresa );


        return this.spHelper.ejecutarListado(
                "p_list_tpex_SolicitudProveedor",
                filtro,
                "A",
                SolicitudProveedor.class
        );
    }
}
