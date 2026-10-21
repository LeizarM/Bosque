package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaBancoDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaEstadisticaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaOpcionDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaVigenteDto;
import bo.bosque.com.impexpap.dto.TcpeRsptaSemanaDetalleDto;
import bo.bosque.com.impexpap.model.TcpePreguntaSemana;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code tcpe_preguntaSemana} vía {@code p_list_tcpe_PreguntaSemana} y {@code p_abm_tcpe_PreguntaSemana}: el CRUD
 * genérico y las acciones de proceso del contrato 15.3.
 */
@Repository
public class TcpePreguntaSemanaDao extends TcpeCrudDao<TcpePreguntaSemana> implements ITcpePreguntaSemana {

    static final String SP_LIST = "p_list_tcpe_PreguntaSemana";
    static final String SP_ABM = "p_abm_tcpe_PreguntaSemana";

    /** Parámetros de p_list (PK, FK, activo); NULL = sin filtro. */
    private static final String[] FILTROS = { "codPreguntaSemana", "codPregunta", "activo" };

    /** Columnas de datos de p_abm, sin PK, audUsuario ni audFecha. */
    private static final String[] COLUMNAS = { "codPregunta", "titulo", "fechaInicio", "fechaFin", "mostrarRespuesta", "activo" };

    private final SpHelper sp;

    public TcpePreguntaSemanaDao(SpHelper spHelper) {
        super(spHelper, TcpePreguntaSemana.class, "preguntaSemana", "codPreguntaSemana", true, FILTROS, COLUMNAS);
        this.sp = spHelper;
    }

    @Override
    public List<TcpePreguntaSemanaDto> gestion(Boolean activo) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "activo", activo);
        return sp.ejecutarListado(SP_LIST, p, "L", TcpePreguntaSemanaDto.class);
    }

    @Override
    public List<TcpePreguntaSemanaVigenteDto> vigentes(long codUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codUsuario", codUsuario);
        List<TcpePreguntaSemanaVigenteDto> vigentes = sp.ejecutarListado(SP_LIST, p, "V", TcpePreguntaSemanaVigenteDto.class);
        if (vigentes.isEmpty()) return Collections.emptyList();

        // Sin @codPreguntaSemana, O trae las opciones de todas las vigentes: se reparten en el orden del SP.
        Map<Long, List<TcpePreguntaSemanaOpcionDto>> porPregunta = new LinkedHashMap<>();
        for (TcpePreguntaSemanaOpcionDto o : sp.ejecutarListado(SP_LIST, p, "O", TcpePreguntaSemanaOpcionDto.class)) {
            porPregunta.computeIfAbsent(o.getCodPreguntaSemana(), k -> new ArrayList<>()).add(o);
        }
        for (TcpePreguntaSemanaVigenteDto v : vigentes) {
            List<TcpePreguntaSemanaOpcionDto> opciones = porPregunta.get(v.getCodPreguntaSemana());
            v.setOpciones(opciones == null ? new ArrayList<>() : opciones);
        }
        return vigentes;
    }

    @Override
    public List<TcpePreguntaSemanaEstadisticaDto> estadistica(Long codPreguntaSemana) {
        return sp.ejecutarListado(SP_LIST, dePregunta(codPreguntaSemana), "E", TcpePreguntaSemanaEstadisticaDto.class);
    }

    @Override
    public List<TcpeRsptaSemanaDetalleDto> respuestas(Long codPreguntaSemana) {
        return sp.ejecutarListado(SP_LIST, dePregunta(codPreguntaSemana), "R", TcpeRsptaSemanaDetalleDto.class);
    }

    @Override
    public List<TcpePreguntaSemanaBancoDto> banco(Long codClasificacion, String texto) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codClasificacion", codClasificacion);
        TcpeParametros.filtro(p, "texto", texto);
        return sp.ejecutarListado(SP_LIST, p, "B", TcpePreguntaSemanaBancoDto.class);
    }

    @Override
    public RespuestaSp activar(Long codPreguntaSemana, Boolean activo, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.poner(p, "codPreguntaSemana", TcpeParametros.id(codPreguntaSemana));
        TcpeParametros.poner(p, "activo", activo);
        p.put("audUsuario", audUsuario);
        return sp.ejecutarAbmMap(SP_ABM, p, "A");
    }

    private static Map<String, Object> dePregunta(Long codPreguntaSemana) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codPreguntaSemana", codPreguntaSemana);
        return p;
    }
}
