package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SolicitudPago;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SolicitudPagoDao implements ISolicitudPago{


    // 1. Variable final, imposible que sea null en tiempo de ejecución
    private final SpHelper spHelper;

    public SolicitudPagoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Procedimiento para registrar, actualizar o eliminar las Solicitudes de Pago
     *
     * @param mb Objeto con los datos
     * @param acc Acción ('I', 'U', 'D')
     * @return RespuestaSp con el error, errormsg y el idGenerado
     */
    @Override
    public RespuestaSp registrarSolicitudPago(SolicitudPago mb, String acc) {
        // Ejecutamos el SP de ABM. El Helper inyectará automáticamente el @ACCION
        return this.spHelper.ejecutarAbm("p_abm_tpex_SolicitudPago", mb, acc);
    }

    /**
     * Procedimiento para obtener las Solicitudes de Pago por Id
     *
     * @param idSolicitud El ID a buscar
     * @return Lista de solicitudes (mapeada automáticamente al modelo)
     */
    @Override
    public List<SolicitudPago> obtenerSolicitudPago(long idSolicitud) {
        // 1. Instanciamos un objeto "filtro" y le seteamos solo el parámetro de búsqueda
        SolicitudPago filtro = new SolicitudPago();
        filtro.setIdSolicitud(idSolicitud);

        // 2. Ejecutamos el listado.

        return spHelper.ejecutarListado(
                "p_list_tpex_SolicitudPago",
                filtro,
                "L",
                SolicitudPago.class
        );
    }
}
