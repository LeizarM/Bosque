package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeMonitorEspejoDto;
import bo.bosque.com.impexpap.dto.TcpeMonitorExamenDto;
import bo.bosque.com.impexpap.dto.TcpeMonitorLoteDto;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TcpeMonitorDao implements ITcpeMonitor {

    static final String SP_LIST = "p_list_tcpe_Monitor";

    private final SpHelper spHelper;

    public TcpeMonitorDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TcpeMonitorLoteDto> lotes() {
        return spHelper.ejecutarListado(SP_LIST, new LinkedHashMap<>(), "D", TcpeMonitorLoteDto.class);
    }

    @Override
    public List<TcpeMonitorExamenDto> listar(Long codRolExamen) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codRolExamen", codRolExamen);
        return spHelper.ejecutarListado(SP_LIST, p, "L", TcpeMonitorExamenDto.class);
    }

    @Override
    public List<TcpeMonitorEspejoDto> espejo(Long codExamen) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codExamen", codExamen);
        return spHelper.ejecutarListado(SP_LIST, p, "E", TcpeMonitorEspejoDto.class);
    }
}
