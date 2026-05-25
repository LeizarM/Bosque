package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.AnticipoDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AnticipoDetalleDao implements IAnticipoDetalle {
    private final SpHelper  spHelper;

    public AnticipoDetalleDao (SpHelper spHelper){this.spHelper = spHelper;}

    /**
     * procedimiento para registrar/actualizar un anticipo detallado (anticipo x empleado)
     * @param ad
     * @param acc
     * @return
     */
    @Override
    public RespuestaSp registrarAnticipoDetalle(AnticipoDetalle ad,String acc){
        return this.spHelper.ejecutarAbm("p_abm_AnticipoDetalle",ad,acc);
    }

    /**
     * procedimiento para obtener el listado de los anticipos detallados por empleado.
     * @param ad
     * @return
     */
    @Override
    public List<AnticipoDetalle>obtenerAnticipoDetalle(AnticipoDetalle ad){
        return this.spHelper.ejecutarListado("p_list_AnticipoDetalle",ad,"A",AnticipoDetalle.class);
    }

    /**
     * ANTICIPO DETALLADO SIN ASIGNAR
     * @param ad
     * @return
     */
    @Override
    public List<AnticipoDetalle>anticipoNoAsignado(AnticipoDetalle ad){
        return this.spHelper.ejecutarListado("p_list_AnticipoDetalle",ad,"B",AnticipoDetalle.class);
    }

    /**
     * ESTADO DE UN ANTICIPO EN ESPECIFICO
     * @param ad
     * @return
     */
    @Override
    public List<AnticipoDetalle>estadoAnticipo(AnticipoDetalle ad){
        return this.spHelper.ejecutarListado("p_list_AnticipoDetalle",ad,"C",AnticipoDetalle.class);
    }

}
