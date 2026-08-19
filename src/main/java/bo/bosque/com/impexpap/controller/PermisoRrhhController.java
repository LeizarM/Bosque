package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IAbonoDias;
import bo.bosque.com.impexpap.dao.IPermiso;
import bo.bosque.com.impexpap.dao.IVacacionAsignada;
import bo.bosque.com.impexpap.dto.CalculoAntiguedadDto;
import bo.bosque.com.impexpap.dto.MiEquipoDto;
import bo.bosque.com.impexpap.dto.PermisoRrhhEscrituraDto;
import bo.bosque.com.impexpap.dto.PermisoRrhhFiltroDto;
import bo.bosque.com.impexpap.model.AbonoDias;
import bo.bosque.com.impexpap.model.VacacionAsignada;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.SpHelper;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.AbonoDiasDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consola administrativa de Permisos y Vacaciones de RR.HH.
 *
 * <p>Responde la pregunta que hoy obliga a abrir el ERP viejo: <i>"¿cuántos días tiene este
 * empleado y de dónde salen?"</i> — tres lecturas sobre {@code p_list_Permiso} (ACCIONes
 * {@code 'C'}, {@code 'D'} y {@code 'U'}) — y desde la Fase 2 <b>también escribe</b>: el ABM de
 * vacación asignada, el ABM de abono de días, la carga colectiva de abono y la vacación colectiva.
 *
 * <h3>Vacación asignada y vacación colectiva son OPUESTAS</h3>
 * La primera escribe {@code trh_vacacionAsignada}: son los días que la empresa te RECONOCE, el
 * <b>debe</b> del saldo. La segunda escribe {@code trh_permiso} con {@code tipoPermiso='vac'}: son
 * días GOZADOS, el <b>haber</b>. Confundirlas le suma días a la gente en vez de descontárselos, y
 * los nombres se parecen lo suficiente como para que valga la pena decirlo acá arriba.
 *
 * <h3>Las escrituras mueven plata</h3>
 * Una vacación asignada vale de 15 a 30 días pagados y un abono son días libres cobrados. Por eso
 * <b>toda la validación de negocio vive en Java</b>: los tres {@code p_abm_} son de 2016, no
 * validan nada, no devuelven id generado ni envelope de error y no tienen transacción. Está toda en
 * {@link IVacacionAsignada} e {@link IAbonoDias}; acá arriba sólo queda el gate de permisos, la
 * identidad y la traducción a HTTP.
 *
 * <h3>Rutas en kebab-case</h3>
 * Sigue el precedente más reciente ({@code /rol-sabados/simular-puente-vacacion}). El resto del
 * backend usa camelCase; la inconsistencia queda documentada acá y el módulo es consistente
 * puertas adentro.
 *
 * <h3>Todo POST con {@code @RequestBody}, nunca query params</h3>
 * {@code SecurityFilter} revisa {@code getParameterMap()} y el query string buscando patrones de
 * SQLi y devuelve 403 con su propio cuerpo ante un apóstrofo o una palabra como {@code select}.
 * Un {@code GET ?nombre=D'Angelo} daría un 403 que nadie sabría explicar.
 *
 * <h3>Los {@code @Transactional} de las cargas colectivas NO están acá</h3>
 * Están en {@code AbonoDiasDao.aplicarGrupal} y {@code PermisoDao.aplicarVacacionColectiva}, que es
 * donde está el bucle que inserta N filas; acá envolverlos sólo agregaría un segundo lugar donde
 * mirar. Dos consecuencias que hay que respetar: este controlador inyecta las <b>interfaces</b>
 * ({@link IAbonoDias}, {@link IPermiso}), porque la transacción vive en el proxy y no en el objeto,
 * y ninguna llamada de escritura se envuelve en un {@code try/catch} — el rollback de Spring
 * depende de que la excepción salga.
 *
 * <h3>Los errores no se arman acá</h3>
 * Se lanza la excepción que corresponde y {@code GlobalExceptionHandler} la convierte en el
 * envelope: {@code SpBusinessException} → 400, {@code SpConflictException} → 409,
 * {@code AccessDeniedException} → 403. Un {@code try/catch} en el controlador sería una segunda
 * forma de responder lo mismo, y las dos se irían separando con el tiempo.
 */
@RestController
@CrossOrigin("*")
@RequestMapping("/permiso-rrhh")
public class PermisoRrhhController {

    private static final Logger log = LoggerFactory.getLogger(PermisoRrhhController.class);

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    /** {@code tb_vista.codVista} de "Permisos / Vacaciones / Abono Dias". */
    private static final int VISTA_PERMISOS = 24;

    // ══════════════════════════════════════════════════════════════════════
    // Los botones del ACL, en constantes y no sueltos por ahí
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <b>LECTURA</b>, separada de toda escritura: la ficha, el desglose, los dos historiales y el
     * padrón de las cargas colectivas. codBtn 106, 5 usuarios.
     *
     * <p>Es el mismo criterio que ya se aplicó en la Fase 1 con {@code btnDetalles}/
     * {@code btnApoyoCalc}: ver el saldo de alguien y moverle 15 días pagados no son la misma
     * atribución y no pueden compartir constante, aunque hoy los tengan las mismas 5 personas.
     */
    private static final String BTN_CONSULTA = "btnDetalles";
    private static final String BTN_CALCULADORA = "btnApoyoCalc";

    /**
     * ABM de vacación asignada. <b>Botón propio, y a propósito uno que todavía no existe en
     * {@code tb_vistaBtn}.</b>
     *
     * <p>El legacy nombra {@code btnEditNewVacAsigAntesDos} (permiso.xhtml:984 y 988) y <b>ese
     * botón tampoco existe en la tabla</b>: verificado contra {@code BOSQUE-2_0}, no está ni en la
     * vista 24 ni en ninguna otra. O sea que en el ERP viejo esta ABM la ejecutan únicamente los
     * {@code adm}, por el fallback de administrador de {@code Loggin.autorizarBtn()}.
     *
     * <p>Antes esto apuntaba a {@code btnEditVacAntPenult} (codBtn 117) como parche, para que los
     * {@code lim} no vieran un botón que devolvía 403. Estaba mal: ese botón existe y lo tienen 5
     * logins, pero en el ERP viejo gobierna <i>editar el permiso ya gozado</i>
     * ({@code trh_permiso}), no la vacación asignada. Reusarlo les concedía alta, edición y baja de
     * filas que valen de 15 a 30 días PAGADOS a 5 personas que nunca tuvieron esa atribución.
     *
     * <p><b>Mientras el nombre no exista en la tabla, esto cierra en vez de abrir.</b>
     * {@link AccesoModuloHelper#tieneBoton} resuelve primero {@code esAdmin} y recién después
     * consulta el ACL: un nombre desconocido no devuelve fila, así que pasan sólo los 6
     * {@code ROLE_ADM} y todos los {@code lim} reciben 403. El {@code PermissionWidget} del front
     * hace lo mismo con lo que dibuja. Es el estado pedido —que sólo Sistemas pueda tocarlo— y
     * falla del lado seguro si el script del DBA no se corre nunca.
     *
     * <p><b>Para abrirlo a RR.HH.:</b> correr {@code sql/04_botones_vacacion_asignada.sql} del
     * proyecto de migración, que crea este botón y el de la baja en {@code tb_vistaBtn} (codVista
     * 24) con sus 134 filas de {@code tb_usuarioBtn} cada uno, y después subir el
     * {@code nivelAcceso} de quien corresponda desde la pantalla de permisos.
     */
    private static final String BTN_VACACION_ASIGNADA = "btnNuevaVacAsignada";

    /**
     * <b>La BAJA de una vacación asignada, con botón propio de verdad</b> y no un alias del alta.
     *
     * <p>Borrar no es editar: una edición se corrige, un DELETE físico se recupera pidiéndoselo a
     * Sistemas. Como los dos nombres se crean juntos en el mismo script, separarlos no cuesta nada
     * y deja la baja en manos de menos gente el día que RR.HH. lo pida, sin tocar código.
     */
    private static final String BTN_VACACION_ASIGNADA_BAJA = "btnEliminarVacAsignada";

    /** ABM individual de abono de días — codBtn 119, el que usa hoy {@code wPermiso.editarAbonDia}. */
    private static final String BTN_ABONO_DIAS = "btnEditarAbonoDia";

    /** Carga colectiva de abono — codBtn 107, el del asistente de dos pasos del legacy. */
    private static final String BTN_ABONO_GRUPAL = "btnNuevoAbonoGrupal";

    /** Vacación colectiva — codBtn 108, 5 usuarios. El otro asistente de dos pasos del legacy. */
    private static final String BTN_VACACION_GRUPAL = "btnNuevaVacGrupal";

    /**
     * "Programar permiso" — codBtn 112, <b>4 usuarios</b>.
     *
     * <p>Uno MENOS que los otros dos botones individuales, y por eso tiene constante propia:
     * compartirla con {@link #BTN_PROGRAMAR_VACACION} le concedería a una persona una atribución
     * que hoy no tiene.
     */
    private static final String BTN_PROGRAMAR_PERMISO = "btnProgramarPermiso";

    /** "Programar vacación" — codBtn 113, 5 usuarios. */
    private static final String BTN_PROGRAMAR_VACACION = "btnProgramarVacacion";

    /**
     * "Vacación pagada" — son días PAGADOS, la escritura de más riesgo del módulo.
     *
     * <p>El nombre está <b>duplicado</b> en {@code tb_vistaBtn} (codBtn 110 y 114, 5 usuarios cada
     * uno). {@code AccesoModuloHelper.tieneBoton} lo tolera con {@code anyMatch}, así que el permiso
     * efectivo es la UNIÓN de los dos padrones. <b>Pendiente de RR.HH.</b>: confirmar que sean el
     * mismo conjunto de personas.
     */
    private static final String BTN_VACACION_PAGADA = "btnNuevaVacPagada";

    /**
     * El tipo de la vacación individual. Literal acá y no importado del DAO porque el controlador
     * inyecta la interfaz; si alguna vez dejara de ser el código de {@code v_tipos},
     * {@code validarTipoPermiso} lo rechaza con un 400 explícito, no en silencio.
     */
    private static final String TIPO_VACACION = "vac";

    /**
     * Tope de cordura de la calculadora. <b>No es una regla de negocio</b> —el SP no valida nada
     * y se autocorrige—: es el rango a partir del cual la pregunta ya no tiene sentido.
     *
     * <p>Se cuenta con años de <b>366 días</b> a propósito. Con 365 el tope real serían 21.900
     * días y 60 años de CALENDARIO son 21.915 (15 bisiestos), así que un rango de exactamente 60
     * años caía en el 400 que dice "no puede superar los 60 años" — la pantalla contradiciéndose
     * a sí misma. El cliente valida lo mismo con 365,25 (calculadora_antiguedad.dart) y queda
     * dentro de este tope, que es el que manda.
     */
    private static final long MAX_RANGO_MS = 60L * 366 * 24 * 60 * 60 * 1000;

    private final IPermiso pDao;
    private final AccesoModuloHelper acceso;
    /** La INTERFAZ, no la clase: la transacción de la carga colectiva vive en el proxy. */
    private final IVacacionAsignada vacDao;
    private final IAbonoDias abonoDao;

    public PermisoRrhhController(IPermiso pDao, AccesoModuloHelper acceso,
                                 IVacacionAsignada vacDao, IAbonoDias abonoDao,
                                 JdbcTemplate jdbcTemplate, SpHelper spHelper) {
        this.pDao = pDao;
        this.acceso = acceso;
        this.vacDao = vacDao;
        this.abonoDao = abonoDao;
        this.jdbcTemplate = jdbcTemplate;
        this.spHelper = spHelper;
    }

    /** Sólo para los reportes Jasper, que necesitan la conexión cruda. */
    private final JdbcTemplate jdbcTemplate;

    private final SpHelper spHelper;

    /** El botón del ACL de los reportes de saldos (codBtn 210, 4 usuarios). */
    private static final String BTN_REPORTES = "btnReportesPYV";

    /**
     * El botón del ACL de la <b>boleta</b> (codBtn 109, 6 usuarios). Es el mismo que exige
     * {@code /vacacion/RptPermisoVacacion}: buscar una boleta y bajarla son la misma atribución,
     * y un buscador que lista lo que después no se puede abrir no sirve de nada.
     */
    private static final String BTN_BOLETA = "btnReImprimirBoleta";

    /**
     * <b>Estado de cuenta del empleado</b> en PDF — el «DETALLE COMPLETO» del sistema anterior.
     *
     * <p>Dos variantes, y la diferencia <b>no la decide Java</b>: vive dentro del {@code .jrxml},
     * en el {@code @codPermiso} de su propia consulta. El interno manda {@code 1} y muestra los
     * días abonados; el fiscal manda {@code 2}, que hace que {@code ACCION 'S'} devuelva
     * {@code diasAbonados = 0.0}, y además arma el documento con 3 subreportes en vez de 4. Es una
     * decisión de la empresa sobre qué ve una inspección, tomada hace años: al portarlo se
     * conservó tal cual y por eso el flag no es un parámetro que alguien pueda pasar mal.
     *
     * <p>Los seis {@code .jrxml} se recompilaron con JasperReports <b>6.21.2</b>, la del backend:
     * los {@code .jasper} de Bosque v2 son de la 4.7 y el formato serializado no es compatible
     * entre esas mayores. Los subreportes se resuelven por classpath desde {@code reports/}, igual
     * que los {@code subRpt*} de la ficha del trabajador, que ya funcionan así.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/reportes/estado-cuenta")
    public ResponseEntity<?> estadoDeCuenta(@RequestBody PermisoRrhhFiltroDto f,
                                            Authentication auth) {
        return estadoDeCuentaPdf(f, auth, "RptPermEmpTotal");
    }

    /**
     * <b>Qué días del rango no descuentan</b>, para que la pantalla explique la resta.
     *
     * <p>Devuelve los feriados de la sucursal del empleado y los sábados que el rol dice que no le
     * tocan. Los domingos no vienen: el cliente los deduce de la fecha y mandarlos sería llenar la
     * respuesta con lo único que no hace falta explicar.
     *
     * <p>Es la <b>misma regla</b> que aplica {@code f_CalcularDiasHabilesPermiso} al grabar. Si se
     * separan, la pantalla explicaría una resta distinta de la que se guarda.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/permisos/dias-no-habiles")
    public ResponseEntity<ApiResponse<?>> diasNoHabiles(@RequestBody PermisoRrhhFiltroDto f,
                                                        Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        validarEmpleado(f);
        return procesarLista(
                pDao.diasNoHabiles(f.getCodEmpleado(), f.getDesde(), f.getHasta()));
    }

    /**
     * <b>Quién está fuera</b> — los permisos de toda la empresa en una fecha o rango.
     *
     * <p>Es la única consulta del módulo que <b>no</b> es por empleado, y por eso no llama a
     * {@code validarEmpleado}: la pregunta es «¿quién falta hoy?», no «¿cuántos días tiene
     * Fulano?». Sin fecha ni rango el DAO rechaza con 400.
     *
     * <p>Llena el hueco que dejó el cronograma del sistema anterior, cuya pantalla y cuyos tres
     * reportes quedaron sin bean.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/permisos/quien-esta-fuera")
    public ResponseEntity<ApiResponse<?>> quienEstaFuera(@RequestBody PermisoRrhhFiltroDto f,
                                                          Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        return procesarLista(pDao.quienEstaFuera(f.getFecRango(), f.getDesde(), f.getHasta()));
    }

    /**
     * <b>Boletas entre fechas</b> — el buscador global, para cuando alguien pide una copia y no se
     * sabe de quién era. No es por empleado, así que no valida empleado.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/permisos/boletas")
    public ResponseEntity<ApiResponse<?>> boletasEntreFechas(@RequestBody PermisoRrhhFiltroDto f,
                                                              Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_BOLETA);
        return procesarLista(
                pDao.boletasEntreFechas(f.getDesde(), f.getHasta(), f.getTipoPermiso()));
    }

    /** La variante fiscal: sin días abonados. Ver {@link #estadoDeCuenta}. */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/reportes/estado-cuenta-fiscal")
    public ResponseEntity<?> estadoDeCuentaFiscal(@RequestBody PermisoRrhhFiltroDto f,
                                                  Authentication auth) {
        return estadoDeCuentaPdf(f, auth, "RptPermEmpTotalFiscal");
    }

    private ResponseEntity<?> estadoDeCuentaPdf(PermisoRrhhFiltroDto f, Authentication auth,
                                                String reporte) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_REPORTES);
        validarEmpleado(f);

        // El reporte pide la relación como parámetro y no la sabe resolver. Si el cliente no la
        // manda, se usa la vigente: la misma resolución que el resto del módulo.
        Long pedida = f.getCodRelEmplEmpr();
        long ree = (pedida != null && pedida > 0)
                ? pedida
                : AbonoDiasDao.relacionActivaDe(spHelper, f.getCodEmpleado());

        Map<String, Object> params = new HashMap<>();
        params.put("codEmp", (int) f.getCodEmpleado());
        params.put("codRee", (int) ree);
        // El logo va como InputStream, igual que en el legacy. La imagen tiene
        // onErrorType="Blank", así que si faltara el PDF sale sin logo en vez de fallar.
        params.put("logoEmpresa", getClass().getResourceAsStream("/logos/logoEmpresa.jpg"));

        byte[] pdf = new JasperReportExport(jdbcTemplate).exportPDFStatic(reporte, params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(pdf.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /**
     * Ficha de saldo del empleado — {@code p_list_Permiso @ACCION='C'}.
     *
     * <p>Devuelve <b>lista</b> (se consume con {@code postAndReturnList} del lado Flutter), no
     * objeto. 204 si el empleado existe y está bien pero el SP no tiene nada que mostrar; 400
     * con el motivo concreto si falta la relación laboral o el cargo; 409 si tiene dos relaciones
     * activas.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/saldo/ficha")
    public ResponseEntity<ApiResponse<?>> fichaSaldo(@RequestBody PermisoRrhhFiltroDto f,
                                                     Authentication auth) {
        // SUPUESTO D4 — pendiente de confirmación de RR.HH. (ver plan §5).
        // El ACL de botones que ya usa toda la casa, con la identidad sacada del token y NUNCA
        // del body. Esconder el botón en Flutter no autoriza nada: esta es la puerta real.
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        validarEmpleado(f);
        return procesarLista(pDao.fichaSaldoEmpleado(f.getCodEmpleado()));
    }

    /**
     * Desglose del saldo en 5 tramos — {@code p_list_Permiso @ACCION='D'}.
     *
     * <p>Devuelve <b>objeto</b> ({@code postAndReturnObject}), no lista: cabecera + los 5 tramos
     * + los totales. Elegir mal el helper del lado Flutter devuelve {@code null} en silencio.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/saldo/desglose")
    public ResponseEntity<ApiResponse<?>> desgloseSaldo(@RequestBody PermisoRrhhFiltroDto f,
                                                        Authentication auth) {
        // SUPUESTO D4 — pendiente de confirmación de RR.HH. (ver plan §5).
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        validarEmpleado(f);
        return procesarObjeto(pDao.desgloseSaldo(f.getCodEmpleado()));
    }

    /**
     * Calculadora de antigüedad — {@code p_list_Permiso @ACCION='U'}.
     *
     * <p>Es una <b>simulación</b>, no el saldo de nadie: se le dan dos fechas cualesquiera y
     * devuelve la frase que arma {@code f_calcDiasLaborablesExt}. La UI la muestra rotulada como
     * orientativa.
     *
     * <p><b>Las validaciones están acá porque el SP no valida nada.</b> No hace falta comprobar
     * el orden de las fechas: la función se autocorrige si vienen al revés.
     *
     * <p><b>Devuelve LISTA de una fila, no objeto</b> ({@code "data":[{"datoAntCalc": "..."}]}),
     * que es lo que dice el contrato del plan §6.3 ④ y lo que consume el cliente con
     * {@code postAndReturnList}. No es un detalle de estilo: {@code postAndReturnList} castea
     * {@code data} a {@code List} y con un objeto <b>lanza un TypeError</b> —no devuelve null en
     * silencio—, así que la pestaña entera muere con un mensaje de cast.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/herramientas/calculo-antiguedad")
    public ResponseEntity<ApiResponse<?>> calculoAntiguedad(@RequestBody PermisoRrhhFiltroDto f,
                                                            Authentication auth) {
        // SUPUESTO D4 — pendiente de confirmación de RR.HH. (ver plan §5).
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CALCULADORA);

        if (f.getDesde() == null || f.getHasta() == null) {
            throw new SpBusinessException(
                    "Debe indicar la fecha de inicio de beneficio y la fecha a calcular.");
        }
        if (Math.abs(f.getHasta().getTime() - f.getDesde().getTime()) > MAX_RANGO_MS) {
            throw new SpBusinessException(
                    "El rango entre las dos fechas no puede superar los 60 años.");
        }
        CalculoAntiguedadDto dto = pDao.calcularAntiguedad(f.getDesde(), f.getHasta());
        return procesarLista(dto == null
                ? Collections.<CalculoAntiguedadDto>emptyList()
                : Collections.singletonList(dto));
    }

    // ==================================================================
    // VACACIÓN ASIGNADA — el "debe" del saldo
    // ==================================================================

    /**
     * Historial del empleado: una fila por aniversario de su relación laboral, mezclando las
     * REALES con las SINTÉTICAS ({@code sintetico = true}, {@code codVacacionAsignada = 0}) de los
     * años que todavía no tienen registro.
     *
     * <p>De acá sale el "Nuevo" de la pantalla: <b>no hay alta libre</b>, se da de alta SOBRE uno
     * de estos aniversarios y la fecha la pone el servidor.
     *
     * <p>La relación laboral es <b>opcional</b>: si el cliente la manda (sale de
     * {@code fichaSaldo.datoRelEmplEmprVigente}) se usa, y si no, la resuelve el DAO. Antes era
     * obligatoria y el resultado era un 400 permanente — el cliente manda {@code 0} cuando todavía
     * no consultó la ficha, y el historial no tiene por qué depender de que la haya consultado.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/vacacion-asignada/historial")
    public ResponseEntity<ApiResponse<?>> historialVacacionAsignada(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        exigirEmpleado(f);
        return procesarLista(
                vacDao.historial(f.getCodEmpleado(), f.getCodRelEmplEmpr(), f.getFecha()));
    }

    /**
     * Alta y edición, <b>en el mismo endpoint y decidido por el id</b>: {@code 0} da de alta,
     * mayor a cero edita. Es lo que hace el legacy ({@code acc = "U"; if (cod == 0) acc = "I";}) y
     * el precedente vivo del repo ({@code PagosExtranjerosController.registrarAsiento}); un
     * endpoint aparte sería una segunda forma de decir lo mismo.
     *
     * <p><b>La edición sólo cambia días y motivo.</b> El SP no actualiza {@code codEmpleado} y deja
     * {@code codRelEmplEmpr} comentado a propósito —mover la fila de relación le cambiaría el saldo
     * a dos personas-relación a la vez—, así que lo que llegue en esos campos se ignora.
     *
     * <p>Responde <b>201 con la fila releída</b> en el alta y 200 en la edición. Se devuelve la
     * fila entera y no el id porque el SP no devuelve {@code SCOPE_IDENTITY}: releer es obligatorio
     * igual, y además hay un proceso automático escribiendo en la misma tabla.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/vacacion-asignada/registrar")
    public ResponseEntity<ApiResponse<?>> registrarVacacionAsignada(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_VACACION_ASIGNADA);
        long audUsuario = codUsuarioDelToken(auth);

        if (f.getCodVacacionAsignada() > 0) {
            return respuestaEscritura(vacDao.edicion(
                    f.getCodVacacionAsignada(), f.getDias(), f.getMotivo(), audUsuario), false);
        }

        VacacionAsignada nueva = new VacacionAsignada();
        nueva.setCodEmpleado(f.getCodEmpleado());
        nueva.setDiasAsignados(f.getDias());
        nueva.setMotivo(f.getMotivo());
        nueva.setFecha(f.getFecha());
        // codRelEmplEmpr NO: lo resuelve el DAO contra la base, igual que en el abono. Aceptarlo
        // del body permitía insertar la vacación en la relación de OTRA persona (el alta sólo
        // comprobaba que fuera > 0, nunca que fuera del empleado).
        return respuestaEscritura(vacDao.alta(nueva, audUsuario, f.isConfirmado()), true);
    }

    /**
     * Baja. DELETE físico y recuperable: el trigger {@code dad_vacacionAsignada} archiva la fila en
     * {@code trh_vacacionAsignadaEliminado} antes de que desaparezca.
     *
     * <p><b>El empleado es obligatorio</b> y no es un capricho del formulario: el SP borra por id
     * sin mirar de quién es la fila, así que sin este dato el DAO no puede comprobar la pertenencia
     * y cualquiera borraría la fila de cualquiera.
     *
     * <p>Devuelve la fila que se borró, para que la pantalla pueda decir exactamente qué
     * desapareció.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/vacacion-asignada/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarVacacionAsignada(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_VACACION_ASIGNADA_BAJA);
        exigirEmpleado(f);
        // Quién borra, resuelto en el servidor. Ya no es sólo para el log: el DAO lo firma en la
        // fila con una 'U' previa, así que el archivo del trigger sale con el autor del BORRADO y
        // no con el de la creación. Falla cerrado si la identidad no resuelve.
        long audUsuario = codUsuarioDelToken(auth);
        log.warn("Baja de vacacion asignada {} (empleado {}) pedida por codUsuario={}",
                 f.getCodVacacionAsignada(), f.getCodEmpleado(), audUsuario);
        return respuestaEscritura(
                vacDao.baja(f.getCodVacacionAsignada(), f.getCodEmpleado(), audUsuario), false);
    }

    // ==================================================================
    // ABONO DE DÍAS — individual
    // ==================================================================

    /**
     * Historial de abonos del empleado en su relación vigente, del más nuevo al más viejo.
     *
     * <p>Se llama {@code historial} y no {@code detalle} para que sea el mismo nombre que su
     * hermano de vacación asignada: los dos son lo mismo, el listado de una persona. El detalle de
     * UNA fila no tiene endpoint propio porque no hace falta — el alta y la edición ya devuelven la
     * fila releída.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/abono-dias/historial")
    public ResponseEntity<ApiResponse<?>> historialAbonoDias(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        exigirEmpleado(f);
        // La relación es opcional: si no viene, la resuelve el DAO. Ver el historial hermano.
        return procesarLista(abonoDao.historial(f.getCodEmpleado(), f.getCodRelEmplEmpr()));
    }

    /**
     * Alta y edición, igual que su hermana: lo decide {@code codAbonoDias}.
     *
     * <p><b>La relación laboral no se acepta del body</b> en el alta: la resuelve el DAO contra la
     * base. Mandar la equivocada le movería el saldo a otra persona-relación, y el cliente no tiene
     * por qué poder elegir eso.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/abono-dias/registrar")
    public ResponseEntity<ApiResponse<?>> registrarAbonoDias(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_ABONO_DIAS);
        long audUsuario = codUsuarioDelToken(auth);

        if (f.getCodAbonoDias() > 0) {
            return respuestaEscritura(abonoDao.edicion(
                    f.getCodAbonoDias(), f.getDias(), f.getMotivo(), audUsuario), false);
        }
        return respuestaEscritura(abonoDao.alta(cabecera(f), audUsuario, f.isConfirmado()), true);
    }

    /**
     * Baja. Lo que el legacy creía que hacía y no hacía: su {@code eliminarAbonDia()} llama a
     * {@code registrar()}, que con el id cargado resuelve a ACCION {@code 'U'} y dispara un UPDATE
     * con todo en NULL. Acá va con {@code 'D'}, y el trigger {@code dad_abonoDias} la archiva en
     * {@code trh_abonoDiasEliminado}.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/abono-dias/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarAbonoDias(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_ABONO_DIAS);
        exigirEmpleado(f);
        long audUsuario = codUsuarioDelToken(auth);
        log.warn("Baja de abono de dias {} (empleado {}) pedida por codUsuario={}",
                 f.getCodAbonoDias(), f.getCodEmpleado(), audUsuario);
        return respuestaEscritura(
                abonoDao.baja(f.getCodAbonoDias(), f.getCodEmpleado(), audUsuario), false);
    }

    // ==================================================================
    // ABONO DE DÍAS — carga colectiva
    // ==================================================================

    /**
     * <b>Qué pasaría.</b> Una fila por persona con {@code aplica} y, para quien queda afuera, el
     * {@code motivoOmision}. <b>No escribe absolutamente nada.</b>
     *
     * <p>Es el segundo paso del asistente del legacy convertido en endpoint, y el mismo papel que
     * cumple {@code trs_sp_puenteVacacion @ACCION='S'} antes del puente. Se conserva porque es la
     * única barrera que existe hoy antes de escribir N filas que son días cobrados.
     *
     * <p>Sigue pidiendo el botón del ACL aunque no escriba: la lista de quién tiene relación activa
     * y quién ya cobró ese día es información de RR.HH., no un cálculo público.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/colectivo/abono-dias/simular")
    public ResponseEntity<ApiResponse<?>> simularAbonoGrupal(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_ABONO_GRUPAL);
        return procesarLista(abonoDao.simularGrupal(cabecera(f), f.getCodEmpleados()));
    }

    /**
     * Lo aplica, <b>todo o nada</b> ({@code @Transactional} en {@code AbonoDiasDao.aplicarGrupal}).
     *
     * <p>El DAO re-simula adentro de la transacción en vez de confiar en la lista que trajo el
     * cliente: entre la pantalla de confirmación y este botón puede haber escrito otra persona —o
     * el proceso automático—, y quién entra tiene que ser el de este instante.
     *
     * <p><b>El mensaje no se copia del legacy.</b> Aquél decía "Problema(s) al registrar N
     * empleado(s)" sobre un fondo de éxitos parciales, porque insertaba sin transacción y dejaba
     * media carga escrita. Acá un fallo significa que no quedó ninguna fila, y el error tiene que
     * decir eso: copiar el texto viejo con la semántica nueva sería mentir.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/colectivo/abono-dias/aplicar")
    public ResponseEntity<ApiResponse<?>> aplicarAbonoGrupal(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_ABONO_GRUPAL);
        int cargados = abonoDao.aplicarGrupal(
                cabecera(f), f.getCodEmpleados(), codUsuarioDelToken(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                "Se registró el abono a " + cargados + " empleado(s).",
                cargados, HttpStatus.CREATED.value()));
    }

    // ==================================================================
    // VACACIÓN COLECTIVA — trh_permiso, NO trh_vacacionAsignada
    // ==================================================================

    /**
     * <b>El padrón</b> de las cargas colectivas: todos los empleados con relación activa, su cargo
     * de planilla más reciente y su sucursal, ordenados por empresa y nombre
     * ({@code p_list_Permiso @ACCION='E'}).
     *
     * <p>Sirve a los <b>dos</b> asistentes colectivos (abono y vacación), así que el gate es un O:
     * alcanza con tener cualquiera de los dos botones. Exigir los dos dejaría afuera a los 4
     * usuarios que sólo tienen el de abono; exigir uno fijo dejaría afuera a los del otro. Para eso
     * existe {@code tieneBoton} —la versión que responde en vez de cortar— y no un try/catch
     * alrededor de un throw.
     *
     * <p>Es una lista de gente de RR.HH. con su empresa y su sucursal: no es un dato público, por
     * eso pide botón aunque no escriba nada.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/colectivo/empleados")
    public ResponseEntity<ApiResponse<?>> padronColectivo(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        if (!acceso.tieneBoton(auth, VISTA_PERMISOS, BTN_ABONO_GRUPAL)
                && !acceso.tieneBoton(auth, VISTA_PERMISOS, BTN_VACACION_GRUPAL)) {
            throw new AccessDeniedException("No tiene habilitada ninguna carga colectiva.");
        }
        return procesarLista(pDao.padronColectivo(f.getCodEmpresa()));
    }

    /**
     * <b>Qué pasaría.</b> Una fila por persona con los días que le tocarían a ELLA y el motivo de
     * quien queda afuera. <b>No escribe absolutamente nada.</b>
     *
     * <p>Existe porque en la vacación colectiva el número del cabezal <b>no es el que se graba</b>:
     * los días se calculan por empleado descontando los feriados de SU sucursal, así que "5 días
     * para 40 personas" puede terminar siendo 4,5 para los de una. Confirmar sobre el número del
     * cabezal sería confirmar un número que nadie va a ver después en la tabla.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/colectivo/vacacion/simular")
    public ResponseEntity<ApiResponse<?>> simularVacacionColectiva(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_VACACION_GRUPAL);
        return procesarLista(pDao.simularVacacionColectiva(
                f.getCodEmpleados(), f.getDesde(), f.getHasta(), f.getMotivo()));
    }

    /**
     * Lo aplica, <b>todo o nada</b> ({@code @Transactional} en
     * {@code PermisoDao.aplicarVacacionColectiva}).
     *
     * <p>Escribe {@code trh_permiso} con {@code tipoPermiso='vac'}: es vacación <b>GOZADA</b>, o
     * sea que le DESCUENTA días del saldo a cada persona. No toca {@code trh_vacacionAsignada}, que
     * es lo contrario.
     *
     * <p>El autorizador de los N permisos es el usuario del token, resuelto en el servidor: es la
     * firma de quien declara que la empresa cierra esos días.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/colectivo/vacacion/aplicar")
    public ResponseEntity<ApiResponse<?>> aplicarVacacionColectiva(
            @RequestBody PermisoRrhhEscrituraDto f, Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_VACACION_GRUPAL);
        int cargados = pDao.aplicarVacacionColectiva(
                f.getCodEmpleados(), f.getDesde(), f.getHasta(), f.getMotivo(),
                codUsuarioDelToken(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                "Se registró la vacación a " + cargados + " empleado(s).",
                cargados, HttpStatus.CREATED.value()));
    }

    // ==================================================================
    // NÓMINA DE PERMISOS y las tres altas individuales
    // ==================================================================

    /**
     * <b>La grilla del kardex</b> — {@code p_list_Permiso @ACCION='Q'}, con los cuatro filtros del
     * legacy. Es el botón "Buscar Permisos" de la pantalla.
     *
     * <p>Los filtros van todos en el mismo cuerpo y se combinan con AND: {@code tipoPermiso} vacío o
     * {@code "0"} es "Todos"; {@code desde}/{@code hasta} acotan el inicio y el fin del permiso; y
     * {@code fecRango} <b>no es un rango</b>, es "quién estaba de permiso el día X". El SP les da
     * esa semántica y el DAO la respeta; acá no se traduce nada.
     *
     * <p><b>Sin columnas de deuda.</b> "Deuda En Día(s)" / "Deuda En Hr(s)" no se portaron: la
     * fórmula del SP depende de {@code trh_repper}, que tiene 0 filas, y devolvería el NEGATIVO de
     * los días. Ver {@code PermisoKardexDto}.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/permisos/kardex")
    public ResponseEntity<ApiResponse<?>> kardexPermisos(@RequestBody PermisoRrhhFiltroDto f,
                                                         Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        validarEmpleado(f);
        return procesarLista(pDao.kardex(f.getCodEmpleado(), f.getCodRelEmplEmpr(),
                f.getTipoPermiso(), f.getDesde(), f.getHasta(), f.getFecRango()));
    }

    /**
     * <b>De dónde sale un tramo del desglose</b> — los permisos concretos que suman ese número.
     *
     * <p>Un solo endpoint para las tres ACCIONes ({@code 'H'}, {@code 'J'}, {@code 'K'}): el
     * cliente manda la clave del tramo y el servidor resuelve la ACCION y las fechas con el mismo
     * {@code fecIniPenUl} que produjo el monto. Si el cliente calculara la fecha, el detalle
     * podría no sumar el total y nadie lo notaría mirando la pantalla.
     *
     * <p>Sólo tres de los cinco tramos se abren. {@code ASIGNADA_ANIO} suma
     * {@code trh_vacacionAsignada} —que tiene su propia pestaña— y {@code ACUMULADA} es
     * {@code f_calcVacacionesAnioLaboral}, una fórmula: no hay lista que mostrar. Cualquier otra
     * clave es 400.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/saldo/detalle-tramo")
    public ResponseEntity<ApiResponse<?>> detalleDeTramo(@RequestBody PermisoRrhhFiltroDto f,
                                                         Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        validarEmpleado(f);
        return procesarLista(pDao.detalleDeTramo(f.getCodEmpleado(), f.getClaveTramo()));
    }

    /**
     * El <b>otro</b> botón de la nómina, "Buscar Vac Ganadas": las vacaciones ASIGNADAS del empleado
     * entre dos fechas ({@code p_list_vacacionAsignada @ACCION='D'}).
     *
     * <p>Es una segunda consulta con su propia grilla, no un filtro de la anterior: en el legacy los
     * dos botones sólo refrescan su {@code dataTable} con los filtros que ya están en el bean, y
     * este ignora el combo de tipo y "Fecha Rango". Por eso son dos POST y no uno con un flag.
     *
     * <p>Va contra {@link IVacacionAsignada} —el dueño de {@code trh_vacacionAsignada}— y no contra
     * {@code /vacacion-asignada/historial}, que es la ACCION {@code 'B'} con filas sintéticas por
     * aniversario: otra pregunta y otra respuesta.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/permisos/vacaciones-ganadas")
    public ResponseEntity<ApiResponse<?>> vacacionesGanadas(@RequestBody PermisoRrhhFiltroDto f,
                                                            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        validarEmpleado(f);
        return procesarLista(vacDao.buscarPorRango(f.getCodEmpleado(), f.getCodRelEmplEmpr(),
                f.getDesde(), f.getHasta()));
    }

    /**
     * El combo de tipo de permiso ({@code v_tipos} grupo 13).
     *
     * <p>{@code incluirVacacionYPago} en {@code false} —el default— devuelve los 7 del modal de
     * permiso, sin {@code vac} ni {@code pva}, igual que {@code Tipos.cargarList13A()} del legacy;
     * en {@code true}, los 9 del filtro de la nómina. El default es el excluyente a propósito: si el
     * cliente se olvida del flag, el peor caso es un filtro al que le faltan dos opciones —visible—
     * y no un modal que ofrece "Vacación" a quien sólo tiene {@link #BTN_PROGRAMAR_PERMISO}.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/permisos/tipos")
    public ResponseEntity<ApiResponse<?>> tiposPermiso(@RequestBody PermisoRrhhFiltroDto f,
                                                       Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        return procesarLista(pDao.tiposPermiso(f.isIncluirVacacionYPago()));
    }

    /**
     * <b>Qué pasaría, y NO ESCRIBE NADA.</b> Es lo que alimenta en vivo "Horas de permiso", "Días de
     * permiso" / "Total días de vacación" y "Horas a reponer" mientras el usuario mueve las fechas
     * —el equivalente del {@code p:ajax event="dateSelect"} del legacy—, y de paso dice si el alta
     * va a poder guardarse ({@code entra} / {@code detalle}).
     *
     * <p>Devuelve <b>objeto</b>, no lista: es una persona.
     *
     * <p>Es el mismo {@code f_CalcularDiasHabilesPermiso} que después graba el alta, así que la
     * pantalla no puede mostrar un número y guardar otro. <b>El radio "Horario Estándar / Continuo"
     * no viaja en el cuerpo</b>: la función lo deduce de la hora de fin ({@code hasta > 17:30}
     * conmuta a jornada continua). En la pantalla el radio elige la ventana horaria que ofrece el
     * reloj, no el motor de cálculo.
     *
     * <p>Pide {@link #BTN_CONSULTA} y no el botón del alta: no escribe, y el mismo cálculo lo
     * necesita quien sólo mira.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/permisos/calcular")
    public ResponseEntity<ApiResponse<?>> calcularPermiso(@RequestBody PermisoRrhhFiltroDto f,
                                                          Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_CONSULTA);
        validarEmpleado(f);
        return procesarObjeto(pDao.previsualizarPermiso(f.getCodEmpleado(), f.getTipoPermiso(),
                f.getDesde(), f.getHasta()));
    }

    /**
     * Modal <b>"Registro de permisos"</b>: un permiso a nombre del empleado, con el tipo del combo.
     * 201 con la fila releída.
     *
     * <p>La vacación NO entra por acá aunque el tipo exista, y es una regla de ACL, no de forma:
     * este endpoint pide {@link #BTN_PROGRAMAR_PERMISO} (4 usuarios) y la vacación pide
     * {@link #BTN_PROGRAMAR_VACACION} (5). Sin este corte, un {@code tipoPermiso: "vac"} le daría a
     * los 4 una atribución que el ACL no les dio. Es el mismo criterio con el que el DAO rechaza
     * {@code pva}.
     *
     * <p>El resto de la validación —tipo contra {@code v_tipos}, rango, motivo, días > 0 y el cruce
     * con un permiso ya cargado— vive en el DAO, que es donde está la transacción: acá sólo queda el
     * gate y la identidad.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/permisos/registrar")
    public ResponseEntity<ApiResponse<?>> registrarPermiso(@RequestBody PermisoRrhhEscrituraDto f,
                                                           Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_PROGRAMAR_PERMISO);
        if (f.getTipoPermiso() != null && TIPO_VACACION.equalsIgnoreCase(f.getTipoPermiso().trim())) {
            throw new SpBusinessException(
                    "La vacación individual se registra desde la pantalla de programar vacación.");
        }
        long audUsuario = codUsuarioDelToken(auth);
        return respuestaEscritura(pDao.registrarPermiso(f.getCodEmpleado(), f.getTipoPermiso(),
                f.getDesde(), f.getHasta(), f.getMotivo(), audUsuario), true);
    }

    /**
     * Modal <b>"Registro de vacación individual"</b>. Es el mismo camino que el permiso —mismo DAO,
     * mismo cálculo, mismo INSERT— con dos diferencias: el tipo lo pone <b>el servidor</b> y el ACL
     * es otro botón.
     *
     * <p>El tipo no se lee del cuerpo a propósito. Si viniera de ahí, esta ruta sería un segundo
     * camino para dar de alta cualquier permiso salteándose {@link #BTN_PROGRAMAR_PERMISO}.
     *
     * <p>Escribe {@code trh_permiso} con {@code tipoPermiso='vac'}: son días GOZADOS, o sea que le
     * DESCUENTA saldo al empleado. No confundir con {@code /vacacion-asignada/registrar}, que es lo
     * contrario.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/vacacion/registrar")
    public ResponseEntity<ApiResponse<?>> registrarVacacion(@RequestBody PermisoRrhhEscrituraDto f,
                                                            Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_PROGRAMAR_VACACION);
        long audUsuario = codUsuarioDelToken(auth);
        return respuestaEscritura(pDao.registrarPermiso(f.getCodEmpleado(), TIPO_VACACION,
                f.getDesde(), f.getHasta(), f.getMotivo(), audUsuario), true);
    }

    /**
     * Modal <b>"Pago de vacaciones"</b> ({@code tipoPermiso='pva'}). <b>Esto paga plata.</b>
     *
     * <p>No hay rango horario ni cálculo: los días los <b>tipea el usuario</b> y el servidor fuerza
     * {@code hasta = fecha}, igual que {@code savePagoDiasVacac}. Como el SP acepta cualquier número
     * —hay una fila histórica de 247 días pagados— toda la defensa está en el DAO: piso de medio
     * día, múltiplos de 0,5, comparación contra el saldo y ventana de doble toque.
     *
     * <p>Por eso {@code confirmado} importa acá más que en ninguna otra ruta: el primer POST vuelve
     * con un <b>400 {@code confirmable}</b> que dice el saldo antes y después, y sólo el segundo
     * —con la confirmación del usuario— escribe. El doble toque del teléfono es distinto y se
     * rechaza con <b>409 aunque {@code confirmado} venga en {@code true}</b>, porque el segundo
     * toque manda exactamente el mismo cuerpo, confirmación incluida.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/vacacion/pagar")
    public ResponseEntity<ApiResponse<?>> pagarVacacion(@RequestBody PermisoRrhhEscrituraDto f,
                                                        Authentication auth) {
        acceso.exigirBoton(auth, VISTA_PERMISOS, BTN_VACACION_PAGADA);
        long audUsuario = codUsuarioDelToken(auth);
        // WARN y no INFO: es la única escritura del módulo que paga días en vez de moverlos, y son
        // ~1 por año. Queda en el log del servidor aunque nadie mire la bitácora de la tabla.
        log.warn("Pago de vacacion pedido: empleado={}, fecha={}, dias={}, confirmado={}, "
               + "codUsuario={}", f.getCodEmpleado(), f.getFecha(), f.getDias(), f.isConfirmado(),
                 audUsuario);
        return respuestaEscritura(pDao.registrarVacacionPagada(f.getCodEmpleado(), f.getFecha(),
                f.getDias(), f.getMotivo(), audUsuario, f.isConfirmado()), true);
    }

    // ==================================================================
    // Auxiliares
    // ==================================================================

    /**
     * <b>Quién firma la escritura, resuelto en el servidor.</b> Es {@code audUsuarioI}, la columna
     * NOT NULL que queda registrada en las 5 filas de bitácora que escribe el trigger por cada
     * alta. Nunca sale del body: si el cliente pudiera afirmar quién es, la auditoría no valdría
     * nada.
     *
     * <p>Falla cerrado. Si el login no resuelve a ningún {@code tb_usuario} no se escribe: el
     * INSERT moriría igual contra el NOT NULL, pero con un error de SQL crudo que en pantalla se
     * lee como "se rompió el sistema".
     */
    private long codUsuarioDelToken(Authentication auth) {
        MiEquipoDto yo = acceso.miPermiso(auth);
        Long codUsuario = yo != null ? yo.getCodUsuario() : null;
        if (codUsuario == null || codUsuario <= 0) {
            throw new SpBusinessException("No se pudo identificar su usuario para registrar la "
                    + "operación. Cierre sesión y vuelva a entrar.");
        }
        return codUsuario;
    }

    /** El cabezal del abono: lo que es igual para uno o para ochenta. */
    private AbonoDias cabecera(PermisoRrhhEscrituraDto f) {
        AbonoDias a = new AbonoDias();
        a.setCodEmpleado(f.getCodEmpleado());
        a.setDiasAbonados(f.getDias());
        a.setFecha(f.getFecha());
        a.setMotivo(f.getMotivo());
        return a;   // codRelEmplEmpr NO: lo resuelve el DAO contra la base
    }

    private void exigirEmpleado(PermisoRrhhEscrituraDto f) {
        if (f == null || f.getCodEmpleado() <= 0) {
            throw new SpBusinessException("Debe seleccionar un empleado.");
        }
    }

    /** 201 cuando se creó algo, 200 cuando se modificó o se borró. Siempre con la fila releída. */
    private <T> ResponseEntity<ApiResponse<?>> respuestaEscritura(T fila, boolean creado) {
        HttpStatus estado = creado ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, fila, estado.value()));
    }

    private void validarEmpleado(PermisoRrhhFiltroDto f) {
        if (f == null || f.getCodEmpleado() <= 0) {
            throw new SpBusinessException("Debe seleccionar un empleado.");
        }
    }

    /**
     * 200 con la lista, o 204 <b>sin cuerpo</b>.
     *
     * <p>El mensaje que se manda con el 204 no lo lee nadie —una respuesta 204 no lleva cuerpo,
     * el contenedor lo descarta, y {@code postAndReturnList} hace {@code return []} sin mirar—.
     * Va igual por consistencia con el resto del backend: todo lo que necesite explicarse viaja
     * como 400 con mensaje, no como un 204 con una nota adentro.
     */
    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("No se encontraron registros.", null,
                            HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    private <T> ResponseEntity<ApiResponse<?>> procesarObjeto(T obj) {
        if (obj == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("No se encontraron registros.", null,
                            HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, obj, HttpStatus.OK.value()));
    }
}
