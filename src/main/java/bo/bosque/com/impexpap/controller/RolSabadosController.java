package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.dto.ConvocarBloqueDto;
import bo.bosque.com.impexpap.dto.GenerarRolDto;
import bo.bosque.com.impexpap.dto.MiEquipoDto;
import bo.bosque.com.impexpap.dto.PuenteVacacionDto;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST del <b>Rol de Turnos de Sábado</b> (módulo {@code trs_}).
 *
 * <p><b>Qué resuelve.</b> Quién viene a trabajar cada sábado del año. La regla por
 * defecto es una rotación A/B: los sábados impares del año trabaja el grupo A, los
 * pares el B. El sistema la aplica solo y nadie tiene que confirmar nada — lo único
 * que se carga a mano son las EXCEPCIONES.
 *
 * <p><b>Arquitectura.</b> Igual que el resto del proyecto: sin JPA, todo por Stored
 * Procedures vía {@code SpHelper}. Nueve tablas, cada una con su
 * {@code p_abm_trs_<Tabla>} y su {@code p_list_trs_<Tabla>}. Los SPs de proceso
 * ({@code trs_sp_*}) van aparte, por {@link IProcesoRol}.
 *
 * <p><b>La grilla se guarda como filas, no como matriz.</b> Cada celda ocupada del
 * Excel es una fila de {@code trs_Asignacion}. <b>Libre = que NO haya fila.</b> Por eso
 * no hay endpoint que devuelva la matriz pivoteada: el front arma la grilla agrupando
 * {@code /obtener-asignaciones} por persona y fecha.
 *
 * <p><b>Las tres capas de una celda</b> ({@code origen}), que es lo que hace que
 * regenerar no borre el trabajo de nadie:
 * <pre>
 *   G = lo generó la rotación A/B      -> REGENERAR la pisa
 *   P = la programó un jefe            -> sobrevive
 *   M = la corrigió RR.HH. a mano      -> sobrevive
 * </pre>
 *
 * <p><b>Prioridad al pintar una celda:</b>
 * {@code FERIADO 'X' > EXCUSADO 'E' > EVENTO '1' > ASUETO 'A' > TRABAJA '1'}.
 *
 * <p><b>Las cuatro formas de salirse del default</b>, cada una con su puerta:
 * <ol>
 *   <li><b>Evento</b> — el sábado de inventario. Se declara con
 *       {@code /marcar-evento} y se completa con {@code /convocar}.</li>
 *   <li><b>Programación</b> — un jefe le cambia el sábado a un dependiente
 *       ({@code /programar}).</li>
 *   <li><b>Cambio</b> — permuta o cobertura entre dos compañeros. Nace SOLICITADO y
 *       <b>no toca la grilla</b> hasta {@code /aprobar-cambio}.</li>
 *   <li><b>Corrección manual</b> — RR.HH. arregla una celda suelta
 *       ({@code /corregir-celda}).</li>
 * </ol>
 * Las cuatro se auditan juntas en {@code /obtener-intervenciones}: pocas filas
 * significa que el default está bien calibrado.
 *
 * <p><b>Convención de la casa:</b> todos los endpoints son POST con
 * {@code @RequestBody}, incluso los de lectura, y responden {@code ApiResponse<T>}.
 * La excepción es {@link #miEquipo}, que NO lleva body: su único parámetro es quién
 * llama, y eso sale del token — no del cliente. Los dos endpoints que decide un jefe
 * ({@link #programar} y {@link #anularProgramacion}) siguen la misma idea: el
 * {@code codEmpleadoProgramador} que venga en el JSON se descarta y se pisa con el del
 * token.
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/rol-sabados")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class RolSabadosController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    /**
     * Lo único que un jefe puede escribir en una celda: '1' que venga, 'L' que no venga.
     *
     * <p>Las otras seis letras son de RR.HH. — 'V' vacación, 'B' baja, 'X' feriado, 'C'
     * cobertura, 'A' asueto, 'E' excusado — y salen de tablas que el jefe no maneja. Si lo
     * dejáramos mandar cualquier letra, con un 'V' se estaría auto-otorgando vacaciones
     * desde la grilla del sábado.
     */
    private static final List<String> LETRAS_DE_JEFE = Arrays.asList("1", "L");

    // ==================== DEPENDENCIAS (9 DAOs de tabla + 1 de proceso) ====================

    /** Catálogo de letras de la grilla (1, L, V, C, B, A, X, E). */
    private final IEstadoTurno estadoTurnoDao;
    /** Cabecera anual del rol (trs_Rol). */
    private final IRol rolDao;
    /** Los ~52 sábados del período (trs_Sabado). */
    private final ISabado sabadoDao;
    /** Foto del organigrama dentro del rol (trs_Participante). */
    private final IParticipante participanteDao;
    /** Lista nominal de un evento (trs_Convocatoria). */
    private final IConvocatoria convocatoriaDao;
    /** Jefes con permiso de programar a otros (trs_Programador). */
    private final IProgramador programadorDao;
    /** El evento de programar (trs_Programacion). */
    private final IProgramacion programacionDao;
    /** Las celdas de la grilla (trs_Asignacion). */
    private final IAsignacion asignacionDao;
    /** Permutas y coberturas (trs_Cambio). */
    private final ICambio cambioDao;
    /** Los SPs de proceso que no tienen ABM propio. */
    private final IProcesoRol procesoRolDao;
    /** El cruce con las vacaciones y permisos de RR.HH. (trh_permiso). */
    private final IPermisoSabado permisoSabadoDao;
    /** Quiénes pueden corregir CUALQUIER celda (trs_Rrhh). */
    private final IRrhh rrhhDao;

    /**
     * Sólo para el PDF. {@link JasperReportExport} necesita una {@code Connection} viva
     * para pasársela a {@code JasperFillManager}, porque el SQL del reporte vive dentro
     * del {@code .jasper} y no pasa por los DAOs. Es la única razón por la que este
     * controlador conoce el {@code JdbcTemplate}; para todo lo demás siguen mandando los
     * SPs vía {@code SpHelper}.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Quién sos y qué podés tocar. Los cuatro métodos de identidad de este controlador
     * ({@code esAdmin}, {@code loginDelToken}, {@code miPermiso}, {@code exigirAdminORrhh})
     * viven ahora acá y se comparten con el módulo de Permisos de RR.HH.; abajo quedan como
     * delegaciones de una línea para no tocar las ~20 llamadas que ya funcionan.
     */
    private final AccesoModuloHelper acceso;

    public RolSabadosController(IEstadoTurno estadoTurnoDao, IRol rolDao, ISabado sabadoDao,
                                IParticipante participanteDao, IConvocatoria convocatoriaDao,
                                IProgramador programadorDao, IProgramacion programacionDao,
                                IAsignacion asignacionDao, ICambio cambioDao,
                                IProcesoRol procesoRolDao,
                                IPermisoSabado permisoSabadoDao,
                                IRrhh rrhhDao,
                                JdbcTemplate jdbcTemplate,
                                AccesoModuloHelper acceso) {
        this.estadoTurnoDao  = estadoTurnoDao;
        this.rolDao          = rolDao;
        this.sabadoDao       = sabadoDao;
        this.participanteDao = participanteDao;
        this.convocatoriaDao = convocatoriaDao;
        this.programadorDao  = programadorDao;
        this.programacionDao = programacionDao;
        this.asignacionDao   = asignacionDao;
        this.cambioDao       = cambioDao;
        this.procesoRolDao   = procesoRolDao;
        this.permisoSabadoDao = permisoSabadoDao;
        this.rrhhDao         = rrhhDao;
        this.jdbcTemplate    = jdbcTemplate;
        this.acceso          = acceso;
    }

    // ==================================================================
    // 1 · GENERACIÓN — el punto de partida
    // ==================================================================

    /**
     * Genera el rol entero de un año: cabecera, ~52 sábados, participantes y celdas.
     *
     * <p>Con {@code modo='REGENERAR'} reconcilia contra el organigrama de hoy y rehace
     * <b>sólo</b> las celdas 'G'. Hay que correrlo cada vez que entra o sale gente:
     * nada lo dispara solo.
     *
     * <p>Sin {@code codSucursal} ni {@code codEmpresa} genera un rol <b>global</b> con
     * toda la empresa. El feriado departamental igual sale bien, porque se resuelve por
     * persona contra {@code trs_Participante.codSucursal}.
     *
     * <p><b>ROLE_ADM o RR.HH.</b> Es la escritura más ancha del módulo: rehace el año de
     * las 85 personas de una. Hasta acá no tenía control propio, o sea que cualquier
     * usuario autenticado podía rehacer el rol entero — el {@code @PreAuthorize} de la
     * clase acepta los dos únicos roles que existen, así que no separa a nadie. Ver
     * {@link #exigirAdminORrhh}.
     */
    @PostMapping("/generar-rol")
    public ResponseEntity<ApiResponse<?>> generarRol(@RequestBody GenerarRolDto dto,
                                                     Authentication auth) {
        exigirAdminORrhh(auth);
        // El SP estampa este valor en trs_Rol, trs_Sabado, trs_Participante y en
        // las ~2 300 celdas. Si viniera del body, la escritura más ancha del
        // módulo sería también la peor auditada: el gate diría quién puede, y la
        // fila diría lo que el cliente quiera. Mismo criterio que firmarConElToken.
        dto.setAudUsuario(miPermiso(auth).getCodUsuario());
        return respuestaCreada(procesoRolDao.generarRol(dto));
    }

    /**
     * Rehace las celdas de UN sábado sin tocar el resto del año. Respeta las capas:
     * sólo pisa las 'G'.
     *
     * <p>ROLE_ADM o RR.HH., igual que la generación entera: es la misma decisión sobre
     * un día en vez de sobre el año, y dejarla abierta sería la puerta de al lado.
     */
    @PostMapping("/regenerar-sabado")
    public ResponseEntity<ApiResponse<?>> regenerarSabado(@RequestBody Sabado mb,
                                                          Authentication auth) {
        exigirAdminORrhh(auth);
        // Firma del token, no del body: ver la nota en {@link #generarRol}.
        procesoRolDao.regenerarSabado(mb.getIdSabado(), miPermiso(auth).getCodUsuario());
        return respuestaOk("Sábado regenerado.");
    }

    /**
     * Sincroniza los feriados del rol contra {@code trh_diaNoLaborable}, en los dos
     * sentidos: marca los que cargaron después y desmarca los que borraron.
     *
     * <p><b>Es el arreglo del caso PUENTE:</b> el feriado cae viernes, RR.HH. declara
     * además el sábado, y el rol ya estaba generado. Sin esto la grilla sigue diciendo
     * que esa gente trabaja.
     *
     * <p>Con {@code soloInformar=true} no escribe nada: reporta qué cambiaría.
     *
     * @param mb Rol con {@code idRol}; {@code observacion='INFORMAR'} para simular
     */
    @PostMapping("/refrescar-feriados")
    public ResponseEntity<ApiResponse<?>> refrescarFeriados(@RequestBody Rol mb) {
        boolean soloInformar = "INFORMAR".equalsIgnoreCase(mb.getObservacion());
        procesoRolDao.refrescarFeriados(mb.getIdRol(), soloInformar, mb.getAudUsuario());
        return respuestaOk(soloInformar ? "Simulación ejecutada; no se escribió nada."
                                        : "Feriados sincronizados con RR.HH.");
    }

    /**
     * Cruza el rol con las vacaciones, bajas y permisos de RR.HH. y corrige las
     * celdas.
     *
     * <p>La rotación A/B sabe a quién le toca, pero no sabe quién está de
     * vacaciones: sin esto, la grilla dice que alguien viene un sábado que
     * RR.HH. ya le dio libre.
     *
     * @param mb Rol con {@code idRol}; {@code observacion='INFORMAR'} para simular
     */
    @PostMapping("/refrescar-permisos")
    public ResponseEntity<ApiResponse<?>> refrescarPermisos(@RequestBody Rol mb) {
        boolean soloInformar = "INFORMAR".equalsIgnoreCase(mb.getObservacion());
        procesoRolDao.refrescarPermisos(mb.getIdRol(), 0, soloInformar,
                                        mb.getAudUsuario());
        return respuestaOk(soloInformar
                ? "Simulación ejecutada; no se escribió nada."
                : "Vacaciones y permisos aplicados a la grilla.");
    }

    /**
     * Los permisos que la grilla todavía NO refleja: alguien figura trabajando
     * un sábado que RR.HH. ya le dio libre.
     */
    @PostMapping("/obtener-desfases-permiso")
    public ResponseEntity<ApiResponse<?>> obtenerDesfasesPermiso(@RequestBody Rol mb) {
        return procesarLista(permisoSabadoDao.obtenerDesfases(mb.getIdRol()),
                             "La grilla ya refleja todas las vacaciones y permisos.");
    }

    /** Todos los permisos que caen en un sábado del rol, coincidan o no. */
    @PostMapping("/obtener-permisos-sabado")
    public ResponseEntity<ApiResponse<?>> obtenerPermisosSabado(@RequestBody Participante mb) {
        return procesarLista(
            permisoSabadoDao.obtenerPermisosDeSabado(mb.getIdRol(), mb.getCodEmpleado()),
            "Ningún permiso de RR.HH. cae en un sábado de este rol.");
    }

    /**
     * <b>Qué pasaría</b> si se declarara ese sábado como puente a cuenta de vacación:
     * una fila por persona, con los días que se le descontarían, y para quien se saltea
     * el motivo. <b>No escribe nada.</b>
     *
     * <p>Es la mitad que alimenta el mensaje de confirmación. Un alta en lote que le
     * consume vacación a cuarenta personas no se aprieta a ciegas.
     */
    @PostMapping("/simular-puente-vacacion")
    public ResponseEntity<ApiResponse<?>> simularPuenteVacacion(
            @RequestBody PuenteVacacionDto mb, Authentication auth) {
        MiEquipoDto yo = miPermiso(auth);
        return procesarLista(
            permisoSabadoDao.simularPuente(
                mb, yo.getCodEmpleado() != null ? yo.getCodEmpleado() : 0L, esAdmin(auth)),
            "Ese sábado no tiene a nadie que tenga que venir, así que no hay puente que declarar.");
    }

    /**
     * Declara el puente: da de alta el permiso de vacación de todos los que ese sábado
     * tenían que venir, y deja sus celdas en 'V'.
     *
     * <p><b>Escribe en {@code trh_permiso}, que es de RR.HH.</b> Le consume saldo de
     * vacación a cada persona, así que quién puede lo decide el SP con la identidad que
     * sale del token: ROLE_ADM o quien esté en {@code trs_Rrhh}. Un jefe programador no,
     * aunque sí pueda corregir la celda de su gente — un puente es una decisión de
     * empresa, no de equipo.
     *
     * <p>{@code codUsuarioAutorizador} sale del token y no del body: es quien queda
     * firmando el permiso de cuarenta personas.
     */
    @PostMapping("/aplicar-puente-vacacion")
    public ResponseEntity<ApiResponse<?>> aplicarPuenteVacacion(
            @RequestBody PuenteVacacionDto mb, Authentication auth) {
        MiEquipoDto yo = miPermiso(auth);
        long codEmpleado = yo.getCodEmpleado() != null ? yo.getCodEmpleado() : 0L;
        long codUsuario  = yo.getCodUsuario()  != null ? yo.getCodUsuario()  : 0L;
        return respuestaEscritura(
            permisoSabadoDao.aplicarPuente(mb, codUsuario, codEmpleado, esAdmin(auth), codUsuario));
    }

    // ==================================================================
    // 2 · ROL (cabecera)
    // ==================================================================

    /**
     * Mueve el estado del rol (BORRADOR · PUBLICADO · CERRADO), o lo crea.
     *
     * <p><b>ROLE_ADM o RR.HH.</b>, y es lo mismo que la generación por dos motivos
     * distintos, los dos verificados en el SP:
     *
     * <ul>
     *   <li><b>Con {@code idRol = 0} esto ES una generación completa.</b> El
     *       {@code 'I'} de {@code p_abm_trs_Rol} no inserta una fila: hace
     *       {@code EXEC trs_sp_generarRol @modo='CREAR'}, o sea la cabecera, los
     *       ~52 sábados, los ~85 participantes y todas sus celdas. El javadoc de
     *       antes decía que «el alta va por /generar-rol» y el código de abajo lo
     *       desmentía: dejarlo abierto era dejar {@code /generar-rol} cerrado con
     *       la llave puesta en la puerta de al lado.
     *   <li><b>Reabrir a BORRADOR es la única llave de la regeneración.</b>
     *       {@code trs_sp_generarRol} rechaza REGENERAR sobre un rol PUBLICADO y
     *       en el mensaje manda justamente acá. Quien puede regenerar tiene que
     *       poder reabrir, o el permiso no sirve para nada.
     * </ul>
     *
     * <p>{@code 'D'} borra en cascada las 6 tablas que cuelgan del rol.
     */
    @PostMapping("/registrar-rol")
    public ResponseEntity<ApiResponse<?>> registrarRol(@RequestBody Rol mb,
                                                       Authentication auth) {
        exigirAdminORrhh(auth);
        return respuestaEscritura(rolDao.registrarRol(mb, mb.getIdRol() == 0 ? "I" : "U"));
    }

    /**
     * Elimina el rol y todo lo que cuelga de él.
     *
     * <p><b>ROLE_ADM o RR.HH.</b> Es estrictamente más destructivo que regenerar
     * —siete DELETE en cascada, y un rol PUBLICADO se borra igual: el SP sólo
     * frena a los CERRADOS— así que no podía quedar más abierto que la
     * regeneración que acabamos de cerrar. Ningún cliente lo llama: no hay
     * constante en el Flutter ni método en su repositorio.
     */
    @PostMapping("/eliminar-rol")
    public ResponseEntity<ApiResponse<?>> eliminarRol(@RequestBody Rol mb,
                                                      Authentication auth) {
        exigirAdminORrhh(auth);
        return respuestaEscritura(rolDao.registrarRol(mb, "D"));
    }

    /** Cabeceras de rol con sucursal y contadores. Filtros en 0 = trae todo. */
    @PostMapping("/obtener-roles")
    public ResponseEntity<ApiResponse<?>> obtenerRoles(@RequestBody Rol mb) {
        long suc = mb.getCodSucursal() != null ? mb.getCodSucursal() : 0L;
        return procesarLista(rolDao.obtenerRoles(suc, mb.getAnio()), "No se encontraron roles.");
    }

    /** Un rol por su id. */
    @PostMapping("/obtener-rol")
    public ResponseEntity<ApiResponse<?>> obtenerRol(@RequestBody Rol mb) {
        return procesarObjeto(rolDao.obtenerRolPorId(mb.getIdRol()), "No se encontró el rol.");
    }

    /**
     * <b>El tablero de control del módulo:</b> todo lo que un humano tocó sobre el
     * horario por defecto, de las cuatro fuentes juntas.
     *
     * <p>Es lo que reemplazó a la conformidad. El rol se acepta por defecto, así que lo
     * único que hay que mirar es lo que se salió de la regla. Pocas filas = el default
     * está bien calibrado; muchas = alguna regla no representa cómo se trabaja de
     * verdad, y conviene arreglar la regla en vez de seguir corrigiendo a mano.
     */
    @PostMapping("/obtener-intervenciones")
    public ResponseEntity<ApiResponse<?>> obtenerIntervenciones(@RequestBody Rol mb) {
        return procesarLista(rolDao.obtenerIntervenciones(mb.getIdRol()),
                             "El rol no tiene intervenciones: se está cumpliendo el default.");
    }

    // ==================================================================
    // 3 · SÁBADOS
    // ==================================================================

    /**
     * Declara el EVENTO de un sábado y rehace sus celdas.
     *
     * <p>{@code alcanceEvento}:
     * <ul>
     *   <li>{@code TOTAL} — vienen todos, sin importar la rotación.</li>
     *   <li>{@code SELECTIVA} — no viene nadie por rotación; sólo los que se
     *       convoquen después con {@link #convocar}. Es el caso del inventario.</li>
     *   <li>{@code null} — vuelve a ser un sábado normal.</li>
     * </ul>
     *
     * <p>Los sábados <b>no se insertan ni se borran</b>: los genera el SP caminando de 7
     * en 7 días y un INSERT suelto rompería {@code indiceAnio}, de donde sale la rotación.
     */
    @PostMapping("/marcar-evento")
    public ResponseEntity<ApiResponse<?>> marcarEvento(@RequestBody Sabado mb) {
        return respuestaEscritura(sabadoDao.registrarSabado(mb, "U"));
    }

    /** Los sábados de un rol. */
    @PostMapping("/obtener-sabados")
    public ResponseEntity<ApiResponse<?>> obtenerSabados(@RequestBody Sabado mb) {
        return procesarLista(sabadoDao.obtenerSabados(mb.getIdRol()), "No se encontraron sábados.");
    }

    /** Un sábado por su id. */
    @PostMapping("/obtener-sabado")
    public ResponseEntity<ApiResponse<?>> obtenerSabado(@RequestBody Sabado mb) {
        return procesarObjeto(sabadoDao.obtenerSabadoPorId(mb.getIdSabado()), "No se encontró el sábado.");
    }

    /** Cobertura por sábado: el SUBTOTAL de abajo del Excel. {@code mes} 0 = todos. */
    @PostMapping("/obtener-cobertura")
    public ResponseEntity<ApiResponse<?>> obtenerCobertura(@RequestBody Sabado mb) {
        return procesarLista(sabadoDao.obtenerCobertura(mb.getIdRol(), mb.getMes()),
                             "No se encontró cobertura para ese rol.");
    }

    /**
     * Sábados cuya marca de feriado NO coincide con RR.HH., en los dos sentidos.
     * Es el control a mirar antes de correr {@link #refrescarFeriados}.
     */
    @PostMapping("/obtener-feriados-desincronizados")
    public ResponseEntity<ApiResponse<?>> obtenerFeriadosDesincronizados(@RequestBody Rol mb) {
        return procesarLista(sabadoDao.obtenerFeriadosDesincronizados(mb.getIdRol()),
                             "Los feriados del rol están sincronizados con RR.HH.");
    }

    /**
     * Los días no laborables de RR.HH., con día de semana, si caen sábado, si son
     * puente de viernes y a cuántas sucursales alcanzan.
     *
     * <p>Muestra TODOS los feriados y no sólo los que caen sábado, justamente para ver
     * el puente. {@code sucursalesAlcanzadas} importa: los feriados son POR SUCURSAL y
     * hay departamentales que aplican a una sola.
     */
    @PostMapping("/obtener-dias-no-laborables")
    public ResponseEntity<ApiResponse<?>> obtenerDiasNoLaborables(@RequestBody Sabado mb) {
        long suc = mb.getCodSucursal() != null ? mb.getCodSucursal() : 0L;
        return procesarLista(sabadoDao.obtenerDiasNoLaborables(mb.getAnio(), suc),
                             "No se encontraron días no laborables.");
    }

    // ==================================================================
    // 4 · PARTICIPANTES
    // ==================================================================

    /**
     * Alta o modificación de un participante.
     *
     * <p>Después de un alta hay que correr {@link #generarRol} con
     * {@code modo='REGENERAR'} para que se le generen las celdas; el SP lo avisa en el
     * mensaje.
     *
     * <p>Si no se manda {@code codSucursal}, el SP lo deduce del cargo vigente. Esa
     * columna es la que decide qué feriado le toca a cada uno.
     *
     * <p><b>El {@code 'U'} NO puede activar ni desactivar a nadie:</b> el SP ignora
     * {@code activo} a propósito. Para sacar o devolver a alguien de los sábados están
     * {@link #eliminarParticipante} y {@link #reincorporarParticipante}.
     */
    @PostMapping("/registrar-participante")
    public ResponseEntity<ApiResponse<?>> registrarParticipante(@RequestBody Participante mb) {
        return respuestaEscritura(
            participanteDao.registrarParticipante(mb, mb.getIdParticipante() == 0 ? "I" : "U"));
    }

    /**
     * Cambia el grupo de rotación de una persona (A o B).
     *
     * <p>Es lo que permite armar los grupos a mano en vez de aceptar el reparto
     * automático por cargo y apellido. <b>No mueve la grilla</b>: hay que correr
     * {@code /generar-rol} con {@code modo='REGENERAR'} para que las celdas se
     * rehagan con los grupos nuevos.
     *
     * <p>Y sobrevive a esa regeneración: el generador sólo asigna grupo a la
     * gente que entra por primera vez, nunca reescribe el de quien ya está.
     */
    @PostMapping("/asignar-grupo")
    public ResponseEntity<ApiResponse<?>> asignarGrupo(@RequestBody Participante mb) {
        long aud = mb.getAudUsuario() != null ? mb.getAudUsuario() : 0L;
        return respuestaEscritura(
            participanteDao.asignarGrupo(mb.getIdParticipante(),
                                         mb.getGrupoRotacion(), aud));
    }

    /**
     * <b>Saca a una persona de los sábados</b> desde una fecha, sin borrarle el
     * historial. No es una baja de la empresa: sigue trabajando, sólo que ya no
     * entra en la rotación del sábado.
     *
     * <p>Se cierra la ventana ({@code fechaBaja}) y {@code activo} NO se toca — esa
     * columna la reconcilia sólo REGENERAR contra la relación laboral, y escribirla
     * acá era lo que hacía que la baja se deshiciera sola en la próxima regeneración.
     *
     * <p><b>Los sábados que ya pasaron quedan como están</b>; de los que vienen se
     * borran las celdas generadas, pero las que programó un jefe ('P') o corrigió
     * RR.HH. a mano ('M') <b>sobreviven</b> y el mensaje de respuesta dice cuántas
     * son. Hay que leerlo: es el único aviso de que a esa persona todavía le queda
     * un sábado cargado después de la fecha en que la sacaron.
     *
     * <p>La meta de cobertura del rol NO se recalcula: es un requisito, no un
     * promedio. Si la baja deja déficit, la grilla lo tiene que mostrar.
     *
     * <p><b>ROLE_ADM o RR.HH.</b>, igual que el ABM de programadores: decidir quién hace
     * sábados y quién no es de RR.HH., no de cualquier usuario del módulo. Estaba con
     * {@code @PreAuthorize("hasRole('ROLE_ADM')")}, que deja afuera justamente al área
     * dueña de la decisión: el módulo entero se pensó para que RR.HH. no dependa de
     * Sistemas para mover su propia nómina. Ver {@link #exigirAdminORrhh}.
     *
     * @param mb Participante con {@code idParticipante} y, opcional,
     *           {@code fechaBaja} (hasta cuándo viene, inclusive; sin ella, hoy)
     */
    @PostMapping("/eliminar-participante")
    public ResponseEntity<ApiResponse<?>> eliminarParticipante(@RequestBody Participante mb,
                                                               Authentication auth) {
        exigirAdminORrhh(auth);
        return respuestaEscritura(
            participanteDao.sacarDeSabados(mb.getIdParticipante(), mb.getFechaBaja(),
                                           miPermiso(auth).getCodUsuario()));
    }

    /**
     * <b>Devuelve a una persona a los sábados</b> a media gestión: abre la ventana
     * ({@code fechaAlta}, y borra {@code fechaBaja}) y le escribe sus celdas de esa
     * fecha en adelante, una por una.
     *
     * <p>No hace falta reabrir el rol: no pasa por REGENERAR —que sobre un rol
     * PUBLICADO está bloqueado— sino por {@code trs_sp_regenerarSabado} acotado a
     * esta persona, así que no se mueven los sábados de nadie más ni se aplican de
     * refilón los cambios de grupo que hubiera pendientes.
     *
     * <p>Rebota si ya no figura en la relación laboral vigente: eso se arregla
     * corriendo REGENERAR, no desde acá.
     *
     * <p><b>ROLE_ADM o RR.HH.</b>, la contracara de {@link #eliminarParticipante}: quien
     * puede cerrar la ventana tiene que poder volver a abrirla, o una baja hecha por
     * error queda esperando a Sistemas.
     *
     * @param mb Participante con {@code idParticipante} y, opcional,
     *           {@code fechaAlta} (sin ella, el próximo sábado del rol)
     */
    @PostMapping("/reincorporar-participante")
    public ResponseEntity<ApiResponse<?>> reincorporarParticipante(@RequestBody Participante mb,
                                                                    Authentication auth) {
        exigirAdminORrhh(auth);
        return respuestaEscritura(
            participanteDao.reincorporar(mb.getIdParticipante(), mb.getFechaAlta(),
                                         miPermiso(auth).getCodUsuario()));
    }

    /** Participantes de un rol. {@code activo} -1 = todos, 0/1 = filtra. */
    @PostMapping("/obtener-participantes")
    public ResponseEntity<ApiResponse<?>> obtenerParticipantes(@RequestBody Participante mb) {
        return procesarLista(participanteDao.obtenerParticipantes(mb.getIdRol(), mb.getActivo()),
                             "No se encontraron participantes.");
    }

    /** Un participante por su id. */
    @PostMapping("/obtener-participante")
    public ResponseEntity<ApiResponse<?>> obtenerParticipante(@RequestBody Participante mb) {
        return procesarObjeto(participanteDao.obtenerParticipantePorId(mb.getIdParticipante()),
                              "No se encontró el participante.");
    }

    /** Turnos por persona contra su objetivo: el SUM de la derecha del Excel. */
    @PostMapping("/obtener-turnos-por-participante")
    public ResponseEntity<ApiResponse<?>> obtenerTurnosPorParticipante(@RequestBody Participante mb) {
        return procesarLista(participanteDao.obtenerTurnosPorParticipante(mb.getIdRol()),
                             "No se encontraron turnos.");
    }

    /**
     * Cumpleaños que caen sábado y qué pasó con el asueto.
     * {@code situacionAsueto} distingue ASUETO · ASUETO ANULADO POR EVENTO · LO
     * EXCUSARON · DECIDIÓ VENIR · FERIADO — es la lista que RR.HH. necesita para saber
     * a quién compensar.
     */
    @PostMapping("/obtener-cumples-sabado")
    public ResponseEntity<ApiResponse<?>> obtenerCumplesSabado(@RequestBody Participante mb) {
        return procesarLista(participanteDao.obtenerCumplesSabado(mb.getIdRol()),
                             "Ningún cumpleaños cae sábado en ese rol.");
    }

    // ==================================================================
    // 5 · CONVOCATORIA — la lista nominal del evento
    // ==================================================================

    /**
     * Convoca o excusa en BLOQUE: una persona, todo el subárbol de un jefe, o todos los
     * de un cargo. Es el caso real del inventario — "que venga Contabilidad entera".
     *
     * <p>Sólo funciona en sábados con evento ya declarado ({@link #marcarEvento}).
     */
    @PostMapping("/convocar")
    public ResponseEntity<ApiResponse<?>> convocar(@RequestBody ConvocarBloqueDto dto) {
        procesoRolDao.convocar(dto);
        return respuestaOk("Convocatoria aplicada y celdas rehechas.");
    }

    /**
     * Convoca o excusa a UNA persona y rehace las celdas del día.
     *
     * <p>'I' y 'U' hacen lo mismo (borran y reinsertan), porque alternar
     * CONVOCADO ↔ EXCUSADO chocaría contra el UNIQUE.
     */
    @PostMapping("/registrar-convocatoria")
    public ResponseEntity<ApiResponse<?>> registrarConvocatoria(@RequestBody Convocatoria mb) {
        return respuestaEscritura(
            convocatoriaDao.registrarConvocatoria(mb, mb.getIdConvocatoria() == 0 ? "I" : "U"));
    }

    /** Saca a la persona de la lista y la devuelve a la rotación. */
    @PostMapping("/eliminar-convocatoria")
    public ResponseEntity<ApiResponse<?>> eliminarConvocatoria(@RequestBody Convocatoria mb) {
        return respuestaEscritura(convocatoriaDao.registrarConvocatoria(mb, "D"));
    }

    /** La lista cruda de un sábado. */
    @PostMapping("/obtener-convocatorias")
    public ResponseEntity<ApiResponse<?>> obtenerConvocatorias(@RequestBody Convocatoria mb) {
        return procesarLista(convocatoriaDao.obtenerConvocatorias(mb.getIdSabado()),
                             "Ese sábado no tiene convocados ni excusados.");
    }

    /**
     * El detalle del evento contrastado contra la rotación.
     * {@code leTocabaPorRotacion} es la columna clave: un CONVOCADO con 1 ya venía solo,
     * y esa fila no agrega a nadie.
     */
    @PostMapping("/obtener-detalle-evento")
    public ResponseEntity<ApiResponse<?>> obtenerDetalleEvento(@RequestBody Convocatoria mb) {
        return procesarLista(convocatoriaDao.obtenerDetalleEvento(mb.getIdSabado()),
                             "Ese sábado no tiene evento con lista nominal.");
    }

    // ==================================================================
    // 6 · PROGRAMADORES Y PROGRAMACIÓN
    // ==================================================================

    /**
     * Alta o modificación de un programador (un jefe con permiso de programar a otros).
     *
     * <p>En el alta el SP devuelve en el mensaje CUÁNTOS dependientes le da el
     * organigrama. Si son 0, el permiso no sirve de nada — mejor enterarse ahí.
     *
     * <p><b>ROLE_ADM o RR.HH.</b> Esta es la tabla que decide quién puede programar a
     * quién: dejarla abierta a todo usuario autenticado es regalar la autoelevación — uno
     * se inserta a sí mismo con {@code alcance='SUBARBOL'} y ya programa a media empresa.
     * Ver {@link #exigirAdminORrhh}.
     */
    @PostMapping("/registrar-programador")
    public ResponseEntity<ApiResponse<?>> registrarProgramador(@RequestBody Programador mb,
                                                               Authentication auth) {
        exigirAdminORrhh(auth);
        return respuestaEscritura(
            programadorDao.registrarProgramador(mb, mb.getIdProgramador() == 0 ? "I" : "U"));
    }

    /**
     * Baja LÓGICA ({@code estado='I'}): borrarlo dejaría huérfanas sus programaciones.
     *
     * <p>ROLE_ADM o RR.HH., por lo mismo que el alta: quien puede sacar permisos también
     * puede sacárselos a otro para quedarse solo con el equipo.
     */
    @PostMapping("/eliminar-programador")
    public ResponseEntity<ApiResponse<?>> eliminarProgramador(@RequestBody Programador mb,
                                                              Authentication auth) {
        exigirAdminORrhh(auth);
        return respuestaEscritura(programadorDao.registrarProgramador(mb, "D"));
    }

    /** Los programadores, con su reemplazo y cuántos dependientes tienen hoy. */
    @PostMapping("/obtener-programadores")
    public ResponseEntity<ApiResponse<?>> obtenerProgramadores(@RequestBody Programador mb) {
        long suc = mb.getCodSucursal() != null ? mb.getCodSucursal() : 0L;
        return procesarLista(programadorDao.obtenerProgramadores(suc, mb.getEstado()),
                             "No se encontraron programadores.");
    }

    /** Un programador por su id. */
    @PostMapping("/obtener-programador")
    public ResponseEntity<ApiResponse<?>> obtenerProgramador(@RequestBody Programador mb) {
        return procesarObjeto(programadorDao.obtenerProgramadorPorId(mb.getIdProgramador()),
                              "No se encontró el programador.");
    }

    /**
     * El organigrama expandido de un jefe. {@code profundidad} 1 = reportes directos.
     * {@code codEmpleado} 0 = todos los programadores.
     */
    @PostMapping("/obtener-dependientes")
    public ResponseEntity<ApiResponse<?>> obtenerDependientes(@RequestBody Programador mb) {
        return procesarLista(programadorDao.obtenerDependientes(mb.getCodEmpleado()),
                             "Ese programador no tiene dependientes en el organigrama.");
    }

    /**
     * A quiénes le quedaría a cargo un permiso <b>que todavía no existe</b>: lo que se
     * mira antes de apretar «Dar de alta».
     *
     * <p>Sale del MISMO árbol con el que {@code trs_sp_programar} valida después
     * ({@code fn_trs_DependientePorCargo}), así que lo que se ve acá es exactamente lo
     * que el sistema va a aceptar. Si fueran dos recorridos distintos, la pantalla
     * prometería un equipo y el sistema aceptaría otro.
     *
     * <p><b>ROLE_ADM o RR.HH.</b>, igual que el alta a la que acompaña: devuelve el
     * organigrama de cualquier persona a partir de su {@code codEmpleado}, o sea quién
     * le reporta a quién en toda la empresa. Es lectura, pero no es pública.
     *
     * <p>{@code codSucursal} en 0 significa <b>todas las sucursales</b>, igual que el
     * NULL que guarda la tabla. {@code alcance} y {@code codSucursal} son dos
     * dimensiones independientes: una dice cuán hondo baja el árbol y la otra cuán
     * ancho. Un jefe cuya planta está en otra sucursal necesita las dos abiertas.
     */
    @PostMapping("/previsualizar-dependientes")
    public ResponseEntity<ApiResponse<?>> previsualizarDependientes(@RequestBody Programador mb,
                                                                    Authentication auth) {
        exigirAdminORrhh(auth);
        long suc = mb.getCodSucursal() != null ? mb.getCodSucursal() : 0L;
        return procesarLista(
            programadorDao.previsualizarDependientes(
                mb.getCodEmpleado(), suc, mb.getAlcance(), mb.getIdRol()),
            "Con ese alcance el organigrama no le da ningún dependiente.");
    }

    /**
     * <b>"Su Equipo":</b> quién sos y a quiénes podés programar. Es lo primero que pide
     * esa pestaña, y de acá sale si se muestra o no.
     *
     * <p><b>Sin body a propósito.</b> La identidad la resuelve el servidor desde el token
     * ({@code login → tb_usuario → trs_Programador}); el cliente no la afirma. Si la
     * mandara él, cambiar un número en el JSON alcanzaría para programar la gente de otro.
     *
     * <p><b>Siempre 200, nunca 204.</b> "No sos programador" ({@code esProgramador=0}, el
     * equipo vacío) es una respuesta completa y correcta, no un resultado vacío — el front
     * necesita ese dato para esconder la pestaña. Por eso no pasa por
     * {@code procesarObjeto}: manda 204, y un 204 con cuerpo lo descarta el cliente HTTP.
     */
    @PostMapping("/mi-equipo")
    public ResponseEntity<ApiResponse<?>> miEquipo(Authentication auth) {
        MiEquipoDto yo = programadorDao.obtenerMiEquipo(loginDelToken(auth));
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, yo, HttpStatus.OK.value()));
    }

    /**
     * Un jefe le cambia el sábado a un dependiente.
     *
     * <p>El SP valida que quien programa sea programador activo (o el reemplazo de uno),
     * que el dependiente cuelgue de él en el organigrama, crea la programación
     * <b>y escribe la celda</b>, todo en una transacción. Del body sólo se usan
     * {@code idSabado}, {@code codEmpleadoDependiente}, {@code codigoExcel} ('L' si decide
     * que no venga) y {@code motivo}.
     *
     * <p><b>{@code codEmpleadoProgramador} y {@code audUsuario} SE IGNORAN</b> aunque
     * vengan en el JSON: se pisan con los del token. Aceptarlos del cliente era la puerta
     * abierta — con cambiar ese número cualquiera programaba en nombre de un jefe.
     *
     * <p>Y se manda el {@code codEmpleado} del <b>logueado</b>, no el del titular, aun
     * cuando quien llama sea el reemplazo. Es a propósito: {@code trs_sp_programar} resuelve
     * el permiso igual (busca por titular O por reemplazo), pero al ver que el que aprieta
     * no es el dueño lo anota en {@code trs_Programacion.codEmpleadoEjecutor}. Mandar el
     * codEmpleado del titular haría desaparecer esa traza: nadie sabría quién decidió.
     *
     * <p>La letra se valida acá y no sólo en el SP porque este endpoint es la puerta del
     * jefe: '1' y 'L' y nada más. Ver {@code LETRAS_DE_JEFE}.
     */
    @PostMapping("/programar")
    public ResponseEntity<ApiResponse<?>> programar(@RequestBody Programacion mb, Authentication auth) {
        MiEquipoDto yo = miPermiso(auth);
        if (yo.getEsProgramador() != 1) {
            throw new SpBusinessException("Tu usuario no esta registrado como programador, "
                    + "asi que no podes programar sabados de nadie.");
        }

        String letra = mb.getCodigoExcel() != null ? mb.getCodigoExcel().trim().toUpperCase() : "";
        if (!LETRAS_DE_JEFE.contains(letra)) {
            throw new SpBusinessException("Sobre tu equipo solo podes decidir que la persona "
                    + "venga (1) o que no venga (L). Vacaciones, bajas y feriados los carga RR.HH.");
        }
        mb.setCodigoExcel(letra);

        mb.setCodEmpleadoProgramador(yo.getCodEmpleado() != null ? yo.getCodEmpleado() : 0L);
        mb.setAudUsuario(yo.getCodUsuario());
        return respuestaEscritura(programacionDao.registrarProgramacion(mb, "I"));
    }

    /**
     * Anula la programación ({@code estado='ANULADA'}) y libera sus celdas.
     *
     * <p>{@code codEmpleadoProgramador} también se pisa con el del token: el SP comprueba
     * que la programación sea TUYA antes de anularla, y esa comprobación no vale nada si el
     * dueño lo declara el mismo que pide la anulación.
     *
     * <p>Acá no se corta antes por {@code esProgramador}: quién puede anular qué lo decide
     * el SP, que además mira si la programación sigue vigente y si el sábado ya pasó.
     */
    @PostMapping("/anular-programacion")
    public ResponseEntity<ApiResponse<?>> anularProgramacion(@RequestBody Programacion mb, Authentication auth) {
        MiEquipoDto yo = miPermiso(auth);
        mb.setCodEmpleadoProgramador(yo.getCodEmpleado() != null ? yo.getCodEmpleado() : 0L);
        mb.setAudUsuario(yo.getCodUsuario());
        return respuestaEscritura(programacionDao.registrarProgramacion(mb, "D"));
    }

    /**
     * Qué decidió cada jefe y con cuánta antelación.
     * {@code controlAntelacion} marca los avisos con menos de una semana.
     */
    @PostMapping("/obtener-programaciones")
    public ResponseEntity<ApiResponse<?>> obtenerProgramaciones(@RequestBody Programacion mb) {
        return procesarLista(programacionDao.obtenerProgramaciones(mb.getIdRol(), mb.getIdSabado()),
                             "No se encontraron programaciones.");
    }

    /** Una programación por su id. */
    @PostMapping("/obtener-programacion")
    public ResponseEntity<ApiResponse<?>> obtenerProgramacion(@RequestBody Programacion mb) {
        return procesarObjeto(programacionDao.obtenerProgramacionPorId(mb.getIdProgramacion()),
                              "No se encontró la programación.");
    }

    // ==================================================================
    // 7 · ASIGNACIONES — la grilla
    // ==================================================================

    /**
     * Las celdas en plano, con nombre y fecha resueltos. <b>Es la fuente de la
     * grilla:</b> el front agrupa por {@code idParticipante} y {@code fecha}.
     *
     * <p>Ojo con lo que NO viene: si a alguien le toca LIBRE ese sábado no hay fila. El
     * libre es la ausencia de la fila, no un estado.
     */
    @PostMapping("/obtener-asignaciones")
    public ResponseEntity<ApiResponse<?>> obtenerAsignaciones(@RequestBody Asignacion mb) {
        return procesarLista(asignacionDao.obtenerAsignaciones(mb.getIdRol(), mb.getIdSabado()),
                             "No se encontraron asignaciones.");
    }

    /**
     * Corrige UNA celda a mano. Se escribe con {@code origen='M'}, y ese origen es lo
     * que hace que la corrección SOBREVIVA a una regeneración.
     *
     * <p>{@code codigoExcel='L'} o vacío libera la celda (borra la fila).
     *
     * <p><b>Quién puede.</b> El SP decide, con la identidad que le pasa ESTE método
     * desde el token: ROLE_ADM y RR.HH. corrigen la celda de cualquiera; un jefe
     * programador, sólo la de su propia gente y sólo entre '1' y 'L'. Antes acá no se
     * validaba nada: alcanzaba con estar logueado, así que cualquiera de los usuarios
     * ROLE_LIM podía cambiarle el sábado a cualquier persona y firmarlo con el
     * {@code audUsuario} que quisiera. Era la puerta de atrás de {@code /programar}.
     *
     * <p><b>{@code codEmpleadoEjecutor}, {@code esAdmin} y {@code audUsuario} SE PISAN</b>
     * aunque vengan en el JSON. Es el mismo criterio que {@code /programar}.
     */
    @PostMapping("/corregir-celda")
    public ResponseEntity<ApiResponse<?>> corregirCelda(
            @RequestBody Asignacion mb, Authentication auth) {
        firmarConElToken(mb, auth);
        return respuestaEscritura(asignacionDao.registrarAsignacion(mb, "U"));
    }

    /** Libera la celda: la persona queda LIBRE ese sábado. Mismo control que corregir. */
    @PostMapping("/liberar-celda")
    public ResponseEntity<ApiResponse<?>> liberarCelda(
            @RequestBody Asignacion mb, Authentication auth) {
        firmarConElToken(mb, auth);
        return respuestaEscritura(asignacionDao.registrarAsignacion(mb, "D"));
    }

    // ==================================================================
    // 7b · RR.HH. — quiénes pueden corregir CUALQUIER celda
    // ==================================================================

    /**
     * Suma a alguien al padrón de RR.HH.
     *
     * <p>Desde que se guarda, esa persona puede corregir la celda de cualquiera con
     * cualquier letra: es el permiso más ancho del módulo después de ROLE_ADM.
     *
     * <p><b>ROLE_ADM o RR.HH.</b>, por lo mismo que el ABM de programadores: quien puede
     * escribir en esta tabla decide quién corrige celdas. Que RR.HH. sume a otro RR.HH.
     * es la delegación que se pidió, no una autoelevación — ver {@link #exigirAdminORrhh}.
     *
     * <p>El alta reactiva si la persona ya estuvo — no crea una segunda fila.
     */
    @PostMapping("/registrar-rrhh")
    public ResponseEntity<ApiResponse<?>> registrarRrhh(@RequestBody Rrhh mb,
                                                        Authentication auth) {
        exigirAdminORrhh(auth);
        return respuestaEscritura(rrhhDao.registrarRrhh(mb, mb.getIdRrhh() == 0 ? "I" : "U"));
    }

    /**
     * Baja LÓGICA ({@code estado='I'}). Lo que esa persona ya corrigió no se toca: la
     * fila queda para poder responder quién podía corregir celdas cuando se corrigieron.
     */
    @PostMapping("/eliminar-rrhh")
    public ResponseEntity<ApiResponse<?>> eliminarRrhh(@RequestBody Rrhh mb,
                                                       Authentication auth) {
        exigirAdminORrhh(auth);
        return respuestaEscritura(rrhhDao.registrarRrhh(mb, "D"));
    }

    /**
     * El padrón, con nombre, cargo y sucursal. {@code estado} vacío trae activos y dados
     * de baja: el ABM necesita ver los dos para poder reactivar a alguien.
     *
     * <p>ROLE_ADM o RR.HH.: es la lista de quién tiene el permiso más ancho del módulo.
     */
    @PostMapping("/obtener-rrhh")
    public ResponseEntity<ApiResponse<?>> obtenerRrhh(@RequestBody Rrhh mb,
                                                      Authentication auth) {
        exigirAdminORrhh(auth);
        return procesarLista(rrhhDao.obtenerRrhh(mb.getEstado()),
                             "Todavía no hay nadie cargado como RR.HH.");
    }

    /**
     * Pone en la asignación quién la está escribiendo, sacándolo del token.
     *
     * <p>Los tres campos se pisan siempre, vengan o no en el body: el cliente no
     * afirma quién es. {@code audUsuario} también, porque si no la auditoría queda
     * falsificable — de nada sirve controlar quién escribe si después la fila dice que
     * la escribió otro.
     *
     * <p>Que alguien no tenga {@code codEmpleado} (un usuario de sistema sin ficha) no
     * se corta acá: se manda en 0 y el SP responde con su propio mensaje, que explica
     * qué hacer. Cortar acá daría un error genérico sin pista.
     */
    private void firmarConElToken(Asignacion mb, Authentication auth) {
        MiEquipoDto yo = miPermiso(auth);
        mb.setCodEmpleadoEjecutor(yo.getCodEmpleado() != null ? yo.getCodEmpleado() : 0L);
        mb.setAudUsuario(yo.getCodUsuario());
        mb.setEsAdmin(esAdmin(auth) ? 1 : 0);
    }

    /**
     * Si el token trae ROLE_ADM. Delega en {@link AccesoModuloHelper#esAdmin(Authentication)},
     * que es donde vive la implementación desde que la comparte el módulo de Permisos de RR.HH.
     */
    private boolean esAdmin(Authentication auth) {
        return acceso.esAdmin(auth);
    }

    /**
     * Corta si quien llama no es ni ROLE_ADM ni RR.HH.
     *
     * <p>Es el guardián de los DOS ABM del módulo —programadores y RR.HH.—, que antes
     * eran sólo de ROLE_ADM. RR.HH. entra ahora porque quién programa y quién corrige
     * celdas es una decisión de personal, no de Sistemas: en esta empresa los 6
     * ROLE_ADM son Sistemas, y tenerlos de intermediarios para cada alta era hacerlos
     * tramitar una decisión que no toman.
     *
     * <p><b>No es un {@code @PreAuthorize}</b> y no puede serlo: ser de RR.HH. no viaja
     * en el token, es una fila en {@code trs_Rrhh}. Por eso se resuelve contra la base,
     * con la identidad que sale del token. {@code esAdmin} va primero para que los 6
     * ROLE_ADM no paguen esa consulta.
     *
     * <p><b>Sí, esto permite delegar el propio permiso:</b> quien está en {@code trs_Rrhh}
     * puede sumar a otro a {@code trs_Rrhh}. Es exactamente lo que se pidió —RR.HH. da de
     * alta a jefes y a otros de RR.HH.— y no es autoelevación: ya tenía el permiso más
     * ancho del módulo antes de tocar el botón. Lo que ningún RR.HH. puede seguir haciendo
     * es darse ROLE_ADM, que vive en otra tabla y fuera de este módulo.
     */
    private void exigirAdminORrhh(Authentication auth) {
        acceso.exigirAdminORrhh(auth);
    }

    /**
     * La mezcla por capa y estado: cuántas celdas puso el generador, cuántas un jefe y
     * cuántas una corrección manual.
     *
     * <p>Al leerla, cuidado: 'G' quiere decir "la escribió el generador", NO "no la
     * decidió nadie" — las celdas del evento y de la convocatoria también son G, porque
     * se derivan de esas tablas y se rehacen en cada regeneración.
     */
    @PostMapping("/obtener-mezcla-por-origen")
    public ResponseEntity<ApiResponse<?>> obtenerMezclaPorOrigen(@RequestBody Asignacion mb) {
        return procesarLista(asignacionDao.obtenerMezclaPorOrigen(mb.getIdRol()),
                             "El rol no tiene celdas.");
    }

    /**
     * Cuánto resuelve el sistema solo: celdas totales, con decisión humana y generadas
     * solas. Si {@code conDecisionHumana} crece, la regla por defecto no representa cómo
     * se trabaja de verdad.
     */
    @PostMapping("/obtener-automatizacion")
    public ResponseEntity<ApiResponse<?>> obtenerAutomatizacion(@RequestBody Asignacion mb) {
        return procesarObjeto(asignacionDao.obtenerAutomatizacion(mb.getIdRol()),
                              "El rol no tiene celdas.");
    }

    /** El catálogo de letras de la grilla, para pintar la leyenda. */
    @PostMapping("/obtener-estados-turno")
    public ResponseEntity<ApiResponse<?>> obtenerEstadosTurno() {
        return procesarLista(estadoTurnoDao.obtenerEstadosTurno(), "No se encontraron estados de turno.");
    }

    /** Un estado por su letra del Excel ('1','V','C','B','X','A','L','E'). */
    @PostMapping("/obtener-estado-turno")
    public ResponseEntity<ApiResponse<?>> obtenerEstadoTurno(@RequestBody EstadoTurno mb) {
        return procesarObjeto(estadoTurnoDao.obtenerPorCodigo(mb.getCodigoExcel()),
                              "No se encontró el estado de turno.");
    }

    /**
     * Alta o modificación de un estado del catálogo. Rara vez hace falta: las ocho
     * letras salen del Excel y cambiarlas cambia el significado de la grilla entera.
     */
    @PostMapping("/registrar-estado-turno")
    public ResponseEntity<ApiResponse<?>> registrarEstadoTurno(@RequestBody EstadoTurno mb) {
        return respuestaEscritura(
            estadoTurnoDao.registrarEstadoTurno(mb, mb.getIdEstadoTurno() == 0 ? "I" : "U"));
    }

    /**
     * Retira un estado del catálogo. Si alguna celda lo usa, el SP no lo borra:
     * lo marca {@code estado='I'} para no romper la FK.
     */
    @PostMapping("/eliminar-estado-turno")
    public ResponseEntity<ApiResponse<?>> eliminarEstadoTurno(@RequestBody EstadoTurno mb) {
        return respuestaEscritura(estadoTurnoDao.registrarEstadoTurno(mb, "D"));
    }

    // ==================================================================
    // 8 · CAMBIOS — permutas y coberturas
    // ==================================================================

    /**
     * Registra la solicitud de permuta o cobertura. <b>Nace SOLICITADO y NO toca la
     * grilla</b> — para eso está {@link #aprobarCambio}.
     *
     * <p>Una {@code PERMUTA} exige {@code idSabadoReposicion} (el sábado que se
     * devuelve); una {@code COBERTURA} no.
     */
    @PostMapping("/registrar-cambio")
    public ResponseEntity<ApiResponse<?>> registrarCambio(@RequestBody Cambio mb) {
        return respuestaEscritura(cambioDao.registrarCambio(mb, mb.getIdCambio() == 0 ? "I" : "U"));
    }

    /**
     * <b>Aprueba y APLICA.</b> Es lo único que mueve la grilla, y son tres escrituras
     * que van juntas en una transacción: el titular pasa a 'C', el que cubre aparece con
     * '1' en un sábado que no le tocaba, y si hay reposición se le libera ese otro
     * sábado.
     *
     * <p>Revalida contra el estado de HOY: si el titular o el reemplazo ya no son
     * participantes activos, rebota.
     */
    @PostMapping("/aprobar-cambio")
    public ResponseEntity<ApiResponse<?>> aprobarCambio(@RequestBody Cambio mb) {
        long aprobador  = mb.getCodAprobador() != null ? mb.getCodAprobador() : 0L;
        long audUsuario = mb.getAudUsuario()   != null ? mb.getAudUsuario()   : 0L;
        return respuestaEscritura(cambioDao.aprobarCambio(mb.getIdCambio(), aprobador, audUsuario));
    }

    /**
     * Anula la solicitud. Rebota si ya está APROBADA: sus celdas ya se escribieron y
     * deshacerlas por acá dejaría la grilla mintiendo — ese caso se arregla con
     * {@link #corregirCelda}.
     */
    @PostMapping("/anular-cambio")
    public ResponseEntity<ApiResponse<?>> anularCambio(@RequestBody Cambio mb) {
        return respuestaEscritura(cambioDao.registrarCambio(mb, "D"));
    }

    /** Las permutas y coberturas con los nombres ya resueltos. */
    @PostMapping("/obtener-cambios")
    public ResponseEntity<ApiResponse<?>> obtenerCambios(@RequestBody Cambio mb) {
        return procesarLista(cambioDao.obtenerCambios(mb.getIdRol(), mb.getEstado()),
                             "No se encontraron cambios.");
    }

    /** Un cambio por su id. */
    @PostMapping("/obtener-cambio")
    public ResponseEntity<ApiResponse<?>> obtenerCambio(@RequestBody Cambio mb) {
        return procesarObjeto(cambioDao.obtenerCambioPorId(mb.getIdCambio()), "No se encontró el cambio.");
    }

    // ==================================================================
    // 9 · EL PDF — la hoja que se publica
    // ==================================================================

    /**
     * El rol de UN sábado en PDF, listo para mandar al grupo de WhatsApp.
     *
     * <p><b>Por qué existe.</b> RR.HH. venía recortando a mano una hoja del Excel y
     * mandándola al grupo. Eso se perdía la mitad de lo que la base ya sabe, así que el
     * PDF es <b>más explícito que el Excel</b>, no una copia: la fecha completa y sin
     * ambigüedad ("SÁBADO 1 DE AGOSTO DE 2026", porque en un chat un "01-ago" a los seis
     * meses no se sabe de qué año es), el total arriba de todo, la gente agrupada por
     * sucursal con su subtotal, y un aviso rojo cuando el sábado es feriado o tiene
     * evento — un sábado de inventario no es un sábado normal y el PDF tiene que gritarlo.
     *
     * <p><b>Trae dos listas, no una</b>, porque son dos lectores distintos: el trabajador
     * busca su nombre para saber si le toca, y el jefe quiere saber por qué falta el que
     * no está. Arriba los que vienen; abajo, bajo una barra oscura, los que NO vienen con
     * su motivo (vacación, cambio, permiso). Los LIBRE no aparecen en ninguna de las dos:
     * en este modelo libre es la ausencia de fila, y una lista de 40 nombres que no vienen
     * porque no les toca no le sirve a nadie.
     *
     * <p><b>Sale de un solo lugar</b>, {@code p_list_trs_Asignacion @ACCION='R'}: el SQL
     * vive dentro del {@code .jasper} y {@link JasperReportExport#exportPDFStatic} le pasa
     * la conexión. Por eso acá sólo viaja el {@code idSabado}.
     *
     * <p><b>Ojo con el .jasper.</b> {@code exportPDFStatic} NO compila el {@code .jrxml}:
     * carga el {@code .jasper} precompilado de {@code resources/reports/}. Si se toca el
     * {@code .jrxml} hay que recompilarlo y versionar los dos archivos, o esto responde 500.
     *
     * @param mb Sábado con {@code idSabado}; el resto del body se ignora
     * @return el PDF crudo ({@code application/pdf}), que el front comparte con
     *         {@code Printing.sharePdf}
     */
    @PostMapping("/reporte-sabado-pdf")
    public ResponseEntity<?> reporteSabadoPdf(@RequestBody Sabado mb) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("idSabado", mb.getIdSabado());

            byte[] reportBytes = new JasperReportExport(this.jdbcTemplate)
                    .exportPDFStatic("RptRolSabado", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("No se pudo armar el PDF del sábado {}", mb.getIdSabado(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================================================================
    // IDENTIDAD — de dónde sale el "quién soy" en los endpoints que escriben
    // ==================================================================

    /**
     * El login del que está llamando.
     *
     * <p>Se usa {@code auth.getName()} y no un claim con el {@code codEmpleado} porque el
     * login es lo ÚNICO que el filtro JWT deja hoy en el {@code SecurityContext}: el
     * principal es el {@code UserDetails} que devuelve {@code loadUserByUsername}. El
     * puente login → codEmpleado lo hace {@code tb_usuario}, del lado del SQL.
     *
     * <p>Si esto viene vacío es que el token no se validó y el request llegó igual, así
     * que se corta acá antes de escribir nada.
     */
    private String loginDelToken(Authentication auth) {
        return acceso.loginDelToken(auth);
    }

    /**
     * Quién soy y si puedo programar, resuelto en el servidor.
     *
     * <p>Trae el permiso <b>sin</b> la lista de dependientes: los endpoints que escriben
     * sólo necesitan la identidad, y de la pertenencia al equipo ya se ocupa
     * {@code trs_sp_programar} con su EXISTS contra {@code fn_trs_ProgramadorDependiente()}.
     * Cargar el equipo entero acá sería una consulta al pedo en cada click.
     */
    private MiEquipoDto miPermiso(Authentication auth) {
        return acceso.miPermiso(auth);
    }

    // ==================================================================
    // HELPERS (mismos que PagosExtranjerosController)
    // ==================================================================

    /** 201 con el id generado. */
    private ResponseEntity<ApiResponse<?>> respuestaCreada(long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, id, HttpStatus.CREATED.value()));
    }

    /** 200 con un mensaje: para los procesos que no generan un id. */
    private ResponseEntity<ApiResponse<?>> respuestaOk(String mensaje) {
        return ResponseEntity.ok(new ApiResponse<>(mensaje, null, HttpStatus.OK.value()));
    }

    /**
     * 201 con el mensaje del SP. Los ABM del módulo lo usan para explicar el siguiente
     * paso ("corra REGENERAR", "quedó sin sucursal"), así que conviene mostrarlo tal cual.
     *
     * <p>Si el SP devuelve error, {@code SpHelper} ya lanzó {@code SpBusinessException}
     * y el handler global respondió 400: acá sólo llegan los casos exitosos.
     */
    private ResponseEntity<ApiResponse<?>> respuestaEscritura(RespuestaSp res) {
        HttpStatus status = res.getError() == 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), status.value()));
    }

    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, String mensajeVacio) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    private <T> ResponseEntity<ApiResponse<?>> procesarObjeto(T obj, String mensajeVacio) {
        if (obj == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, obj, HttpStatus.OK.value()));
    }
}
