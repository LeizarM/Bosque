package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeRendirExamenDto;
import bo.bosque.com.impexpap.dto.TcpeRendirOpcionDto;
import bo.bosque.com.impexpap.dto.TcpeRendirPreguntaDto;
import bo.bosque.com.impexpap.dto.TcpeRevisionItemDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TcpeRendirDao implements ITcpeRendir {

    static final String SP_LIST = "p_list_tcpe_Rendir";
    static final String SP_ABM = "p_abm_tcpe_Rendir";

    private final SpHelper spHelper;

    public TcpeRendirDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TcpeRendirExamenDto> misExamenes(long codUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codUsuario", codUsuario);
        return spHelper.ejecutarListado(SP_LIST, p, "M", TcpeRendirExamenDto.class);
    }

    @Override
    public TcpeRendirExamenDto estado(long codExamen, long codUsuario) {
        List<TcpeRendirExamenDto> filas =
                spHelper.ejecutarListado(SP_LIST, examen(codExamen, codUsuario), "E", TcpeRendirExamenDto.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public List<TcpeRendirPreguntaDto> preguntas(long codExamen, long codUsuario) {
        return spHelper.ejecutarListado(SP_LIST, examen(codExamen, codUsuario), "P", TcpeRendirPreguntaDto.class);
    }

    @Override
    public List<TcpeRendirOpcionDto> opciones(long codExamen, long codUsuario) {
        return spHelper.ejecutarListado(SP_LIST, examen(codExamen, codUsuario), "O", TcpeRendirOpcionDto.class);
    }

    @Override
    public List<TcpeRevisionItemDto> revision(long codExamen, long codUsuario) {
        return spHelper.ejecutarListado(SP_LIST, examen(codExamen, codUsuario), "F", TcpeRevisionItemDto.class);
    }

    @Override
    public RespuestaSp iniciar(long codExamen, long codUsuario, Long audUsuario) {
        Map<String, Object> p = examen(codExamen, codUsuario);
        TcpeParametros.poner(p, "audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I");
    }

    @Override
    public RespuestaSp responder(long codExamen, long codUsuario, Long codPreguntaH, List<Long> opciones,
                                 String respuestaU, Long audUsuario) {
        Map<String, Object> p = examen(codExamen, codUsuario);
        TcpeParametros.poner(p, "codPreguntaH", codPreguntaH);
        // Lista vacía = '' (deja la pregunta sin responder); null = no es una cerrada, no se manda.
        if (opciones != null) p.put("opciones", SpHelper.listaDeIds(opciones));
        TcpeParametros.poner(p, "respuestaU", respuestaU);
        TcpeParametros.poner(p, "audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "R");
    }

    @Override
    public RespuestaSp latido(long codExamen, long codUsuario, Long codPreguntaH, Boolean salioPestana) {
        Map<String, Object> p = examen(codExamen, codUsuario);
        TcpeParametros.poner(p, "codPreguntaH", codPreguntaH);
        TcpeParametros.poner(p, "salioPestana", salioPestana);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "A");
    }

    @Override
    public RespuestaSp finalizar(long codExamen, long codUsuario, Long audUsuario) {
        Map<String, Object> p = examen(codExamen, codUsuario);
        TcpeParametros.poner(p, "audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "F");
    }

    /** Sin lote ({@code codRolExamen} nulo) entrega los vencidos de todos los lotes. */
    @Override
    public long entregarVencidos(Long codRolExamen, Long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.poner(p, "codRolExamen", TcpeParametros.id(codRolExamen));
        TcpeParametros.poner(p, "audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "V").getIdGenerado();
    }

    @Override
    public long entregarVencidosDe(long codUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codUsuario", codUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "V").getIdGenerado();
    }

    private static Map<String, Object> examen(long codExamen, long codUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codExamen", codExamen);
        p.put("codUsuario", codUsuario);
        return p;
    }
}
