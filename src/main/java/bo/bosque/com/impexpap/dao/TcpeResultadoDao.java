package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeExamenCabeceraDto;
import bo.bosque.com.impexpap.dto.TcpeHistorialDto;
import bo.bosque.com.impexpap.dto.TcpeInformeDto;
import bo.bosque.com.impexpap.dto.TcpeRankingPreguntaDto;
import bo.bosque.com.impexpap.dto.TcpeResultadoExamenDto;
import bo.bosque.com.impexpap.dto.TcpeRevisionDto;
import bo.bosque.com.impexpap.dto.TcpeSolucionarioDto;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TcpeResultadoDao implements ITcpeResultado {

    static final String SP_LIST = "p_list_tcpe_Resultado";

    private final SpHelper spHelper;

    public TcpeResultadoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TcpeResultadoExamenDto> listar(Long codRolExamen) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codRolExamen", codRolExamen);
        return spHelper.ejecutarListado(SP_LIST, p, "L", TcpeResultadoExamenDto.class);
    }

    @Override
    public List<TcpeRevisionDto> revision(Long codExamen) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codExamen", codExamen);
        return spHelper.ejecutarListado(SP_LIST, p, "R", TcpeRevisionDto.class);
    }

    @Override
    public List<TcpeSolucionarioDto> solucionario(Long codExamen, Long codRolExamen) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codExamen", codExamen);
        TcpeParametros.filtro(p, "codRolExamen", codRolExamen);
        return spHelper.ejecutarListado(SP_LIST, p, "S", TcpeSolucionarioDto.class);
    }

    @Override
    public List<TcpeExamenCabeceraDto> cabeceras(Long codRolExamen, Long codExamen) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codRolExamen", codRolExamen);
        TcpeParametros.filtro(p, "codExamen", codExamen);
        return spHelper.ejecutarListado(SP_LIST, p, "E", TcpeExamenCabeceraDto.class);
    }

    @Override
    public List<TcpeHistorialDto> historial(String ci) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "ci", ci);
        return spHelper.ejecutarListado(SP_LIST, p, "H", TcpeHistorialDto.class);
    }

    @Override
    public List<TcpeInformeDto> informe(Long codClasificacion, LocalDate fechaDesde, LocalDate fechaHasta) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codClasificacion", codClasificacion);
        TcpeParametros.filtro(p, "fechaDesde", fechaDesde);
        TcpeParametros.filtro(p, "fechaHasta", fechaHasta);
        return spHelper.ejecutarListado(SP_LIST, p, "C", TcpeInformeDto.class);
    }

    @Override
    public List<TcpeRankingPreguntaDto> ranking(Long codClasificacion, Long codRolExamen, LocalDate fechaDesde,
                                                LocalDate fechaHasta) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codClasificacion", codClasificacion);
        TcpeParametros.filtro(p, "codRolExamen", codRolExamen);
        TcpeParametros.filtro(p, "fechaDesde", fechaDesde);
        TcpeParametros.filtro(p, "fechaHasta", fechaHasta);
        return spHelper.ejecutarListado(SP_LIST, p, "P", TcpeRankingPreguntaDto.class);
    }
}
