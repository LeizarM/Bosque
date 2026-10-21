package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaVigenteDto;
import bo.bosque.com.impexpap.dto.TcpeRsptaSemanaDetalleDto;
import bo.bosque.com.impexpap.model.TcpePreguntaSemana;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

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

    /** Parámetros de p_list (PK y activo); NULL = sin filtro. */
    private static final String[] FILTROS = { "codPreguntaSemana", "activo" };

    /** Columnas de datos de p_abm, sin PK, audUsuario ni audFecha. */
    private static final String[] COLUMNAS = { "titulo", "pregunta", "fechaInicio", "fechaFin", "activo" };

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
        return sp.ejecutarListado(SP_LIST, p, "V", TcpePreguntaSemanaVigenteDto.class);
    }

    @Override
    public List<TcpeRsptaSemanaDetalleDto> respuestas(Long codPreguntaSemana) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codPreguntaSemana", codPreguntaSemana);
        return sp.ejecutarListado(SP_LIST, p, "R", TcpeRsptaSemanaDetalleDto.class);
    }

    @Override
    public RespuestaSp activar(Long codPreguntaSemana, Boolean activo, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.poner(p, "codPreguntaSemana", TcpeParametros.id(codPreguntaSemana));
        TcpeParametros.poner(p, "activo", activo);
        p.put("audUsuario", audUsuario);
        return sp.ejecutarAbmMap(SP_ABM, p, "A");
    }
}
