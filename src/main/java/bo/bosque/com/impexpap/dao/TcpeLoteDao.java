package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeEmpleadoElegibleDto;
import bo.bosque.com.impexpap.dto.TcpeLoteDisponibleDto;
import bo.bosque.com.impexpap.dto.TcpeLoteDto;
import bo.bosque.com.impexpap.dto.TcpeLoteFiltroDto;
import bo.bosque.com.impexpap.dto.TcpeLoteGuardarDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TcpeLoteDao implements ITcpeLote {

    static final String SP_LIST = "p_list_tcpe_Lote";
    static final String SP_ABM = "p_abm_tcpe_Lote";

    private final SpHelper spHelper;

    public TcpeLoteDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TcpeLoteDto> listar(TcpeLoteFiltroDto f) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (f != null) {
            TcpeParametros.filtro(p, "codRolExamen", f.getCodRolExamen());
            TcpeParametros.filtro(p, "codArea", f.getCodArea());
            TcpeParametros.filtro(p, "codClasificacion", f.getCodClasificacion());
            TcpeParametros.filtro(p, "modalidad", f.getModalidad());
        }
        return spHelper.ejecutarListado(SP_LIST, p, "L", TcpeLoteDto.class);
    }

    @Override
    public List<TcpeLoteDisponibleDto> disponibles(Long codClasificacion) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codClasificacion", codClasificacion);
        return spHelper.ejecutarListado(SP_LIST, p, "D", TcpeLoteDisponibleDto.class);
    }

    @Override
    public List<TcpeEmpleadoElegibleDto> empleados(Long codRolExamen) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codRolExamen", codRolExamen);
        return spHelper.ejecutarListado(SP_LIST, p, "E", TcpeEmpleadoElegibleDto.class);
    }

    /** Los mismos datos que {@code I} (sin codRolExamen: lote nuevo) y los tres conteos juntos. */
    @Override
    public RespuestaSp validar(TcpeLoteGuardarDto lote, int cantidad, int cantPersonas, int cantEmpleados) {
        Map<String, Object> p = parametros(lote);
        conteos(p, cantidad, cantPersonas, cantEmpleados);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "V");
    }

    @Override
    public RespuestaSp validarAgregar(long codRolExamen, int cantidad, int cantPersonas, int cantEmpleados) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codRolExamen", codRolExamen);
        conteos(p, cantidad, cantPersonas, cantEmpleados);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "V");
    }

    /** Sin nota ni {@code mostrarRespuestas}, los valores por defecto los pone el SP (desde {@code f_tcpe_config}). */
    @Override
    public RespuestaSp crear(TcpeLoteGuardarDto dto, long audUsuario) {
        Map<String, Object> p = parametros(dto);
        p.put("audUsuario", audUsuario);
        p.put("codRolExamen", 0L);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I");
    }

    @Override
    public RespuestaSp actualizar(TcpeLoteGuardarDto dto, long audUsuario) {
        Map<String, Object> p = parametros(dto);
        p.put("audUsuario", audUsuario);
        TcpeParametros.poner(p, "codRolExamen", dto.getCodRolExamen());
        return spHelper.ejecutarAbmMap(SP_ABM, p, "U");
    }

    @Override
    public RespuestaSp eliminar(Long codRolExamen, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.poner(p, "codRolExamen", codRolExamen);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }

    /** En V van siempre los tres, en 0 los que no vienen. */
    private static void conteos(Map<String, Object> p, int cantidad, int cantPersonas, int cantEmpleados) {
        p.put("cantidad", cantidad);
        p.put("cantPersonas", cantPersonas);
        p.put("cantEmpleados", cantEmpleados);
    }

    /** Solo lo que declara {@code p_abm_tcpe_Lote}; lo nulo no va (en {@code U} = no se cambia). */
    private static Map<String, Object> parametros(TcpeLoteGuardarDto dto) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.poner(p, "codArea", TcpeParametros.id(dto.getCodArea()));
        TcpeParametros.poner(p, "codClasificacion", TcpeParametros.id(dto.getCodClasificacion()));
        TcpeParametros.poner(p, "titulo", dto.getTitulo());
        TcpeParametros.poner(p, "observaciones", dto.getObservaciones());
        TcpeParametros.poner(p, "modalidad", dto.getModalidad());
        TcpeParametros.poner(p, "duracionMinutos", dto.getDuracionMinutos());
        TcpeParametros.poner(p, "cantidadPreg", dto.getCantidadPreg());
        TcpeParametros.poner(p, "fecha", dto.getFecha());
        TcpeParametros.poner(p, "horaInicio", dto.getHoraInicio());
        TcpeParametros.poner(p, "horaFin", dto.getHoraFin());
        TcpeParametros.poner(p, "ubicacion", dto.getUbicacion());
        TcpeParametros.poner(p, "notaAprobacion", dto.getNotaAprobacion());
        TcpeParametros.poner(p, "mostrarRespuestas", dto.getMostrarRespuestas());
        return p;
    }
}
