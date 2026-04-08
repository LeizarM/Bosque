package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ConfigComisionesBanco;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ConfigComisionesBancoDao implements IConfigComisionesBanco {

    private final SpHelper spHelper;

    public ConfigComisionesBancoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarConfigComisionesBanco(ConfigComisionesBanco mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tpex_ConfigComisionesBanco", mb, acc);
    }

    @Override
    public List<ConfigComisionesBanco> obtenerConfigComisionesBanco(long idConfig) {
        ConfigComisionesBanco filtro = new ConfigComisionesBanco();
        filtro.setIdConfig(idConfig);
        return spHelper.ejecutarListado("p_list_tpex_ConfigComisionesBanco", filtro, "L", ConfigComisionesBanco.class);
    }

    @Override
    public List<ConfigComisionesBanco> obtenerConfigPorBanco(int codBanco) {
        ConfigComisionesBanco filtro = new ConfigComisionesBanco();
        filtro.setCodBanco(codBanco);
        return spHelper.ejecutarListado("p_list_tpex_ConfigComisionesBanco", filtro, "B", ConfigComisionesBanco.class);
    }
}

