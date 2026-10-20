package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeConfigDto;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;

@Repository
public class TcpeConfigDao implements ITcpeConfig {

    static final String SP_LIST = "p_list_tcpe_Config";

    private final SpHelper spHelper;

    public TcpeConfigDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TcpeConfigDto> listar() {
        return spHelper.ejecutarListado(SP_LIST, new LinkedHashMap<>(), "L", TcpeConfigDto.class);
    }
}
