package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.AnticipoDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IAnticipoDetalle {
    /**
     * procedimiento para registrar/actualizar un anticipo x empleado
     * @param ad
     * @param acc
     * @return
     */
    RespuestaSp registrarAnticipoDetalle(AnticipoDetalle ad,String acc);

    /**
     * procedimiento para listar los anticipos por empleado
     * @param ad
     * @return
     */
    List<AnticipoDetalle> obtenerAnticipoDetalle(AnticipoDetalle ad);

    /**
     * detalle anticipos sin asignar (TIGO)
     * @param ad
     * @return
     */
    List<AnticipoDetalle> anticipoNoAsignado(AnticipoDetalle ad);

    /**
     * OBRTENDRA EL ESTADO DE LOS ANTICIPOS
     * @param ad
     * @return
     */
    List<AnticipoDetalle> estadoAnticipo(AnticipoDetalle ad);
}
