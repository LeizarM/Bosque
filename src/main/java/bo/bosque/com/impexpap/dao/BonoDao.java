package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Bono;
import bo.bosque.com.impexpap.model.BonoEmpleado;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BonoDao implements IBono {

    private final SpHelper spHelper;

    public BonoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<Bono> listarBono(Bono b) {
        return this.spHelper.ejecutarListado("p_list_Bono", b,"C", Bono.class);
    }

    @Override
    public List<BonoEmpleado> listarBonoEmpleado(BonoEmpleado be) {
        return this.spHelper.ejecutarListado("p_list_BonoEmpleado", be, "A", BonoEmpleado.class);
    }

    @Override
    public RespuestaSp abmBono(Bono b,String acc) {
        return this.spHelper.ejecutarAbm("p_abm_Bono", b, acc);
    }
}
