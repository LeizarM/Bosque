package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Monedas;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MonedasDao implements IMonedas {

    private final SpHelper spHelper;

    public MonedasDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarMonedas(Monedas mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tpex_Monedas", mb, acc);
    }

    @Override
    public List<Monedas> obtenerMonedas(long idMoneda) {
        Monedas filtro = new Monedas();
        filtro.setIdMoneda(idMoneda);

        System.out.println("filtro monedas "+filtro.toString());


        return spHelper.ejecutarListado("p_list_tpex_Monedas", filtro, "L", Monedas.class);
    }
}

