package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Talonario;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TalonarioDao implements ITalonario {

    private final SpHelper spHelper;

    public TalonarioDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarTalonario(Talonario mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tmto_Talonario", mb, acc);
    }

    /*
     * Overload de Map en todos los listados: p_list_tmto_Talonario usa NULL
     * como "sin filtro" y el overload de modelo conserva los 0 de los
     * primitivos, con lo que filtraria por codTalonario = 0.
     * Un filtro que no se envia queda en el DEFAULT NULL del SP.
     */
    @Override
    public List<Talonario> obtenerTalonario(long codTalonario) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codTalonario", codTalonario);
        return spHelper.ejecutarListado("p_list_tmto_Talonario", filtro, "L", Talonario.class);
    }

    @Override
    public List<Talonario> listarTalonario(Long codTipoRecibo, Long codEmpresa,
                                           Long codGrupo, Integer codEstadoActual,
                                           Date fechaDesde, Date fechaHasta,
                                           Boolean incluirCerrados) {
        Map<String, Object> filtro = new HashMap<>();
        if (codTipoRecibo   != null) filtro.put("codTipoRecibo",   codTipoRecibo);
        if (codEmpresa      != null) filtro.put("codEmpresa",      codEmpresa);
        if (codGrupo        != null) filtro.put("codGrupo",        codGrupo);
        if (codEstadoActual != null) filtro.put("codEstadoActual", codEstadoActual);
        if (fechaDesde      != null) filtro.put("fechaDesde",      fechaDesde);
        if (fechaHasta      != null) filtro.put("fechaHasta",      fechaHasta);
        // Se manda solo si viene explicito: el default del SP es 1 (incluirlos).
        if (incluirCerrados != null) filtro.put("incluirCerrados", incluirCerrados);
        return spHelper.ejecutarListado("p_list_tmto_Talonario", filtro, "A", Talonario.class);
    }

    @Override
    public List<Talonario> listarDisponibles(Long codGrupo) {
        Map<String, Object> filtro = new HashMap<>();
        if (codGrupo != null) filtro.put("codGrupo", codGrupo);
        return spHelper.ejecutarListado("p_list_tmto_Talonario", filtro, "B", Talonario.class);
    }

    @Override
    public List<Talonario> buscarPorNroTalonario(String nroTalonario) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("nroTalonario", nroTalonario);
        return spHelper.ejecutarListado("p_list_tmto_Talonario", filtro, "A", Talonario.class);
    }
}
