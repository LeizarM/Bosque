package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.MiEquipoDto;
import bo.bosque.com.impexpap.dto.ProgramadorDependienteDto;
import bo.bosque.com.impexpap.model.Programador;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class ProgramadorDao implements IProgramador {

    private static final String SP_ABM  = "p_abm_trs_Programador";
    private static final String SP_LIST = "p_list_trs_Programador";

    private final SpHelper spHelper;

    public ProgramadorDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarProgramador(Programador programador, String acc) {
        log.info("Registrando Programador: {}, Accion: {}", programador.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, programador, acc);
    }

    // ── L: los jefes con permiso ──────────────────────────────────────────
    // @estado es char(1) y el SP filtra con (@estado IS NULL OR ...): sólo se
    // manda si viene con contenido, para no perder las filas inactivas.
    @Override
    public List<Programador> obtenerProgramadores(long codSucursal, String estado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codSucursal", codSucursal);
        if (estado != null && !estado.trim().isEmpty()) {
            filtro.put("estado", estado);
        }
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Programador.class);
    }

    // ── L: por id ─────────────────────────────────────────────────────────
    @Override
    public Programador obtenerProgramadorPorId(long idProgramador) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idProgramador", idProgramador);
        List<Programador> r = spHelper.ejecutarListado(SP_LIST, filtro, "L", Programador.class);
        return r.isEmpty() ? null : r.get(0);
    }

    // ── D: el organigrama expandido ───────────────────────────────────────
    // Otra forma (una fila por dependiente): mapea contra el DTO, no contra Programador.
    @Override
    public List<ProgramadorDependienteDto> obtenerDependientes(long codEmpleado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);
        return spHelper.ejecutarListado(SP_LIST, filtro, "D", ProgramadorDependienteDto.class);
    }

    // ── P: el mismo organigrama, pero ANTES de guardar la fila ────────────
    @Override
    public List<ProgramadorDependienteDto> previsualizarDependientes(
            long codEmpleado, long codSucursal, String alcance, long idRol) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);
        filtro.put("codSucursal", codSucursal);
        // Se normaliza acá y no se confía en el default del SP: cualquier cosa
        // que no sea SUBARBOL es DIRECTOS, el mismo criterio que usa el árbol.
        filtro.put("alcance", "SUBARBOL".equals(alcance) ? "SUBARBOL" : "DIRECTOS");
        filtro.put("idRol", idRol);
        return spHelper.ejecutarListado(SP_LIST, filtro, "P", ProgramadorDependienteDto.class);
    }

    // ══════════════════════════════════════════════════════════════════════
    // "SU EQUIPO" — la identidad la resuelve el servidor, no el cliente
    // ══════════════════════════════════════════════════════════════════════

    // ── E: quién sos y si podés programar ─────────────────────────────────
    // El login vacío se manda igual (como cadena vacía, nunca NULL): el driver no sabe
    // qué tipo darle a un parámetro NULL, y el SP ya trata '' como "no resuelve".
    @Override
    public MiEquipoDto resolverPermiso(String login) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("login", login != null ? login.trim() : "");

        List<MiEquipoDto> r = spHelper.ejecutarListado(SP_LIST, filtro, "E", MiEquipoDto.class);
        if (!r.isEmpty()) return r.get(0);

        // Nunca null: /mi-equipo responde 200 siempre y "no sos programador" es una
        // respuesta legítima. Si devolviéramos null, el front tendría que distinguir
        // entre "no tenés permiso" y "se cayó la consulta", que no es lo mismo.
        log.warn("La accion E de {} no devolvio ninguna fila para el login '{}'", SP_LIST, login);
        MiEquipoDto sinPermiso = new MiEquipoDto();
        sinPermiso.setEsProgramador(0);
        sinPermiso.setEsReemplazo(0);
        return sinPermiso;
    }

    // ── D: los dependientes de UN permiso ─────────────────────────────────
    // Por idProgramador y no por codEmpleado: fn_trs_ProgramadorDependiente() camina el
    // organigrama desde el JEFE y no conoce codEmpleadoReemplazo. Filtrando por el id del
    // permiso, el suplente ve exactamente el equipo del titular — que es el que el SP de
    // escritura le va a dejar tocar.
    @Override
    public List<ProgramadorDependienteDto> obtenerDependientesPorProgramador(long idProgramador) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idProgramador", idProgramador);
        return spHelper.ejecutarListado(SP_LIST, filtro, "D", ProgramadorDependienteDto.class);
    }

    // ── El permiso + la gente, que es lo que pinta la pestaña ─────────────
    // Toma UN SOLO permiso, el primero que devuelve el ORDER BY de la acción E (titular
    // antes que reemplazo), y no fusiona los demás si alguien tuviera varios. Es a
    // propósito: trs_sp_programar también resuelve un único idProgramador y valida con un
    // EXISTS contra ESE. Una lista fusionada mostraría gente que el SP después rechaza,
    // y no hay peor pantalla que una que ofrece lo que el servidor no deja hacer.
    @Override
    public MiEquipoDto obtenerMiEquipo(String login) {
        MiEquipoDto yo = resolverPermiso(login);
        if (yo.getEsProgramador() == 1 && yo.getIdProgramador() != null) {
            yo.setEquipo(obtenerDependientesPorProgramador(yo.getIdProgramador()));
        }
        return yo;
    }
}
