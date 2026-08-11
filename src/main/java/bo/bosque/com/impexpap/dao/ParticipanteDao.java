package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Participante;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class ParticipanteDao implements IParticipante {

    private static final String SP_ABM  = "p_abm_trs_Participante";
    private static final String SP_LIST = "p_list_trs_Participante";

    private final SpHelper spHelper;

    public ParticipanteDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarParticipante(Participante participante, String acc) {
        log.info("Registrando Participante: {}, Accion: {}", participante.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, participante, acc);
    }

    // ── U: sólo el grupo ──────────────────────────────────────────────────
    @Override
    public RespuestaSp asignarGrupo(long idParticipante, String grupoRotacion,
                                    long audUsuario) {
        log.info("Asignando grupo {} al participante {}", grupoRotacion, idParticipante);
        Map<String, Object> params = new HashMap<>();
        params.put("idParticipante", idParticipante);
        params.put("grupoRotacion", grupoRotacion);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "U");
    }

    /* Los dos de abajo van por ejecutarAbmMap por la misma razon que asignarGrupo,
       y aca pesa todavia mas: son operaciones que se DEFINEN por lo que NO tocan.
       ejecutarAbm serializaria el bean entero y mandaria activo=1, nroOrden,
       grupoRotacion y todo lo demas; con el Map viajan tres parametros y el resto
       queda NULL en el SP.
       La fecha se convierte a java.sql.Date porque el parametro del SP es DATE:
       viaja como objeto nativo al PreparedStatement, sin pasar por texto. NULL es
       un valor legitimo -- significa "usa tu default" (hoy para la baja, el proximo
       sabado para el alta) -- y setObject(n, null) lo manda sin consultar
       metadatos.
       Supuesto, el mismo que ya hace todo el proyecto: el JVM corre con la zona
       horaria de Bolivia. JacksonConfig parsea "2026-07-04" como medianoche en
       America/La_Paz y el driver formatea el java.sql.Date con la zona por defecto
       del JVM; si fueran distintas y la del JVM estuviera mas al oeste que UTC-4,
       la fecha se correria un dia.                                               */

    // ── D: sale de los sabados desde una fecha ────────────────────────────
    @Override
    public RespuestaSp sacarDeSabados(long idParticipante, java.util.Date fechaBaja,
                                      long audUsuario) {
        log.info("Sacando de los sabados al participante {} desde {}", idParticipante, fechaBaja);
        Map<String, Object> params = new HashMap<>();
        params.put("idParticipante", idParticipante);
        params.put("fechaBaja", aSqlDate(fechaBaja));
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    // ── R: vuelve a los sabados desde una fecha ───────────────────────────
    @Override
    public RespuestaSp reincorporar(long idParticipante, java.util.Date fechaAlta,
                                    long audUsuario) {
        log.info("Reincorporando a los sabados al participante {} desde {}", idParticipante, fechaAlta);
        Map<String, Object> params = new HashMap<>();
        params.put("idParticipante", idParticipante);
        params.put("fechaAlta", aSqlDate(fechaAlta));
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "R");
    }

    private static java.sql.Date aSqlDate(java.util.Date d) {
        return d == null ? null : new java.sql.Date(d.getTime());
    }

    // ── L: los participantes de un rol ────────────────────────────────────
    // @activo se manda sólo si viene 0 o 1; con -1 se omite y el SP no filtra.
    @Override
    public List<Participante> obtenerParticipantes(long idRol, int activo) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        if (activo >= 0) {
            filtro.put("activo", activo);
        }
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Participante.class);
    }

    // ── L: por id ─────────────────────────────────────────────────────────
    @Override
    public Participante obtenerParticipantePorId(long idParticipante) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idParticipante", idParticipante);
        List<Participante> r = spHelper.ejecutarListado(SP_LIST, filtro, "L", Participante.class);
        return r.isEmpty() ? null : r.get(0);
    }

    // ── T: turnos por persona (el SUM del Excel) ──────────────────────────
    @Override
    public List<Participante> obtenerTurnosPorParticipante(long idRol) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        return spHelper.ejecutarListado(SP_LIST, filtro, "T", Participante.class);
    }

    // ── K: cumpleaños que caen sábado ─────────────────────────────────────
    @Override
    public List<Participante> obtenerCumplesSabado(long idRol) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        return spHelper.ejecutarListado(SP_LIST, filtro, "K", Participante.class);
    }
}
