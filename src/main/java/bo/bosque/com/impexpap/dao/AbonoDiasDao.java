package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.config.SpConfirmableException;
import bo.bosque.com.impexpap.config.SpConflictException;
import bo.bosque.com.impexpap.model.AbonoDias;
import bo.bosque.com.impexpap.utils.Fmt;
import bo.bosque.com.impexpap.utils.SpEscritura;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Escrituras y lecturas de {@code trh_abonoDias}, individuales y en lote.
 *
 * <h3>Mismo estilo legacy que {@link VacacionAsignadaDao}</h3>
 * {@code p_abm_AbonoDias} es de 2016 y no declara {@code @error/@errormsg/@idGenerado}, así que
 * {@code SpHelper.ejecutarAbmMap} no se puede usar. Se escribe con {@code jdbcTemplate.update} y
 * un SQL por ACCION, con los parámetros por nombre — el orden de este SP <b>no</b> es el del otro
 * ({@code @fecha} va antes que {@code @motivo}).
 *
 * <h3>Las dos consultas de lote</h3>
 * Resolver la relación laboral activa de un lote de empleados ({@code @ACCION='R'}) y ver quién ya
 * tiene un abono en esa fecha ({@code @ACCION='F'}) son ACCIONes de {@code p_list_AbonoDias}: acá
 * no se arma SQL. Las dos reciben el lote en {@code @codEmpleados}, separado por comas.
 */
@Slf4j
@Repository
public class AbonoDiasDao implements IAbonoDias {

    private static final String SP_LIST = "p_list_AbonoDias";

    private static final String SQL_ALTA =
            "execute p_abm_AbonoDias "
          + "@codEmpleado=?, @diasAbonados=?, @fecha=?, @motivo=?, "
          + "@codRelEmplEmpr=?, @audUsuarioI=?, @ACCION=?";

    /**
     * La fila COMPLETA, y no sólo lo editable: el UPDATE de este SP pisa {@code codEmpleado} y
     * {@code codRelEmplEmpr}, y {@code codEmpleado} es NOT NULL. Una 'U' parcial dejaría la
     * columna en NULL y reventaría el UPDATE entero.
     */
    private static final String SQL_EDICION =
            "execute p_abm_AbonoDias "
          + "@codAbonoDias=?, @codEmpleado=?, @diasAbonados=?, @fecha=?, @motivo=?, "
          + "@codRelEmplEmpr=?, @audUsuarioI=?, @ACCION=?";

    private static final String SQL_BAJA =
            "execute p_abm_AbonoDias @codAbonoDias=?, @ACCION=?";

    /** Largo de {@code trh_abonoDias.motivo}. Uno más y el SP muere por truncamiento. */
    static final int MOTIVO_MAX = 50;

    /**
     * Topes de cordura, <b>no reglas de negocio</b>. Medido sobre las 184 filas de la tabla: de
     * 0,5 a 22 días, promedio 2,57. Por encima de {@link #DIAS_INUSUAL} se pide confirmación y por
     * encima de {@link #DIAS_TOPE} se rechaza.
     */
    static final double DIAS_INUSUAL = 22;
    static final double DIAS_TOPE    = 60;

    /** Tope del lote. SQL Server admite 2.100 parámetros por sentencia; el padrón activo son ~85. */
    static final int MAX_LOTE = 400;

    private final SpHelper spHelper;
    private final JdbcTemplate jdbcTemplate;

    public AbonoDiasDao(SpHelper spHelper, JdbcTemplate jdbcTemplate) {
        this.spHelper = spHelper;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ══════════════════════════════════════════════════════════════════════
    // LECTURAS
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<AbonoDias> historial(long codEmpleado, Long codRelEmplEmpr) {
        // La relación puede no venir: el cliente todavía no consultó la ficha, o directamente no
        // tiene por qué saberla. Se resuelve acá, con la MISMA consulta que usa el alta, en vez de
        // devolver un 400 que obliga a la pantalla a pedir la ficha primero.
        long codRel = (codRelEmplEmpr != null && codRelEmplEmpr > 0)
                ? codRelEmplEmpr
                : relacionActivaDe(spHelper, codEmpleado);

        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);
        filtro.put("codRelEmplEmpr", codRel);

        List<AbonoDias> filas = spHelper.ejecutarListado(SP_LIST, filtro, "L", AbonoDias.class);
        // La ACCION 'L' no ordena; la grilla los muestra del más nuevo al más viejo.
        filas.sort((x, y) -> {
            Date fx = x.getFecha();
            Date fy = y.getFecha();
            if (fx == null && fy != null) return 1;
            if (fx != null && fy == null) return -1;
            int porFecha = (fx == null) ? 0 : fy.compareTo(fx);
            return porFecha != 0 ? porFecha
                    : Long.compare(y.getCodAbonoDias(), x.getCodAbonoDias());
        });
        filas.forEach(this::decorar);
        return filas;
    }

    @Override
    public AbonoDias porId(long codAbonoDias) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codAbonoDias", codAbonoDias);

        List<AbonoDias> filas = spHelper.ejecutarListado(SP_LIST, filtro, "L", AbonoDias.class);
        if (filas.isEmpty()) return null;
        return decorar(filas.get(0));
    }

    // ══════════════════════════════════════════════════════════════════════
    // ESCRITURAS INDIVIDUALES
    // ══════════════════════════════════════════════════════════════════════

    /** ACCION 'I'. El SP no valida nada: las reglas de abajo son la única defensa. */
    @Override
    public AbonoDias alta(AbonoDias a, long audUsuarioI, boolean confirmado) {
        if (a == null || a.getCodEmpleado() <= 0) {
            throw new SpBusinessException("No se seleccionó al empleado.");
        }
        validarDias(a.getDiasAbonados(), confirmado);
        final String motivo = validarMotivo(a.getMotivo());
        final Date fecha = validarFecha(a.getFecha());
        SpEscritura.exigirUsuarioAuditor(audUsuarioI);

        // La relación NO se toma del body: la equivocada le movería el saldo a otra
        // persona-relación, y todas las lecturas del módulo filtran justamente por ella.
        final long codEmpleado = a.getCodEmpleado();
        final long codRel = relacionActivaDe(spHelper, codEmpleado);

        validarSinDuplicado(codEmpleado, codRel, fecha, a.getDiasAbonados(), motivo, confirmado);

        insertar(codEmpleado, codRel, a.getDiasAbonados(), fecha, motivo, audUsuarioI);
        log.info("Alta de abono de dias: empleado={}, relacion={}, fecha={}, dias={}, usuario={}",
                 codEmpleado, codRel, fecha, a.getDiasAbonados(), audUsuarioI);

        AbonoDias guardado = ultimoEn(codEmpleado, codRel, fecha);
        if (guardado == null) {
            throw new SpBusinessException(
                    "El abono no aparece en la base después de guardarlo. Vuelva a consultar el "
                  + "historial antes de reintentar.");
        }
        return guardado;
    }

    /** ACCION 'U'. Sólo días y motivo; empleado, relación y fecha se conservan tal cual estaban. */
    @Override
    public AbonoDias edicion(long codAbonoDias, double diasAbonados, String motivo, long audUsuarioI) {
        if (codAbonoDias <= 0) {
            throw new SpBusinessException("No se indicó qué abono editar.");
        }
        AbonoDias actual = porId(codAbonoDias);
        if (actual == null) {
            throw new SpBusinessException("No se encontró el registro; puede que ya lo hayan borrado.");
        }
        validarDias(diasAbonados, true);   // en la edición no hay confirmación previa que pedir
        final String motivoOk = validarMotivo(motivo);
        SpEscritura.exigirUsuarioAuditor(audUsuarioI);

        actualizar(codAbonoDias, actual.getCodEmpleado(), diasAbonados, actual.getFecha(),
                   motivoOk, actual.getCodRelEmplEmpr(), audUsuarioI);

        log.info("Edicion de abono de dias {}: dias {} -> {}, usuario={}",
                 codAbonoDias, actual.getDiasAbonados(), diasAbonados, audUsuarioI);
        return porId(codAbonoDias);
    }

    /** ACCION 'D'. Lo que el legacy creía que hacía y no hacía (llamaba a la 'U'). */
    @Override
    public AbonoDias baja(long codAbonoDias, long codEmpleado, long audUsuarioI) {
        if (codAbonoDias <= 0) {
            throw new SpBusinessException("No se indicó qué abono dar de baja.");
        }
        AbonoDias actual = porId(codAbonoDias);
        if (actual == null) {
            throw new SpBusinessException("No se encontró el registro; puede que ya lo hayan borrado.");
        }
        // El SP borra por id sin mirar de quién es la fila: la pertenencia se comprueba acá.
        if (codEmpleado > 0 && actual.getCodEmpleado() != codEmpleado) {
            throw new SpBusinessException(
                    "Ese registro no es del empleado que está viendo; refresque la pantalla.");
        }
        SpEscritura.exigirUsuarioAuditor(audUsuarioI);

        firmarAntesDeBorrar(actual, audUsuarioI);

        ejecutar(SQL_BAJA, "dar de baja el abono de días", ps -> {
            ps.setLong(1, codAbonoDias);
            ps.setString(2, "D");
        });

        log.info("Baja de abono de dias {}: empleado={}, fecha={}, dias={}, usuario={}",
                 codAbonoDias, actual.getCodEmpleado(), actual.getFecha(), actual.getDiasAbonados(),
                 audUsuarioI);
        return actual;
    }

    /**
     * <b>Quién borró, escrito en la fila ANTES de que la fila desaparezca.</b>
     *
     * <p>La ACCION {@code 'D'} del SP no recibe {@code @audUsuarioI}: hace un DELETE pelado. El
     * trigger {@code dad_abonoDias} archiva la fila en {@code trh_abonoDiasEliminado}, pero la
     * archiva con el {@code audUsuarioI} que tenía — o sea, el de quien la <b>creó</b>. La bitácora
     * decía que la fila la borró la misma persona que la había cargado, aunque hubiera sido otra.
     *
     * <p>La solución no toca ningún SP: se ejecuta la {@code 'U'} con <b>los mismos valores</b>
     * (nada cambia de verdad) y el {@code audUsuarioI} del token. Eso deja la firma del que borra
     * en la fila, y el archivo del trigger sale ya con el autor correcto.
     *
     * <p>Un UPDATE que no cambia nada no dispara filas de {@code tb_bitacora}: el trigger
     * {@code dau_} sólo escribe cuando el valor viejo y el nuevo difieren. O sea que esto no
     * ensucia la auditoría, sólo firma.
     */
    private void firmarAntesDeBorrar(AbonoDias actual, long audUsuarioI) {
        actualizar(actual.getCodAbonoDias(), actual.getCodEmpleado(), actual.getDiasAbonados(),
                   actual.getFecha(), actual.getMotivo(), actual.getCodRelEmplEmpr(), audUsuarioI);
    }

    /**
     * La {@code 'U'} pelada, con la fila COMPLETA.
     *
     * <p>Completa y no parcial porque el UPDATE de este SP pisa {@code codEmpleado} y
     * {@code codRelEmplEmpr}, y {@code codEmpleado} es NOT NULL: una 'U' que no los mande revienta
     * el UPDATE entero.
     */
    private void actualizar(long codAbonoDias, long codEmpleado, double dias, Date fecha,
                            String motivo, Long codRel, long audUsuarioI) {
        // ejecutarUpdate y NO ejecutar: dau_abonoDias es un INSTEAD OF UPDATE con SET NOCOUNT ON,
        // así que el rowcount llega en 0 aunque la fila se haya guardado. Ver SpEscritura.
        SpEscritura.ejecutarUpdate(jdbcTemplate, SQL_EDICION, "editar el abono de días", ps -> {
            ps.setLong(1, codAbonoDias);
            ps.setLong(2, codEmpleado);
            ps.setDouble(3, dias);
            ps.setDate(4, new java.sql.Date(fecha.getTime()));
            ps.setString(5, motivo);
            if (codRel == null) ps.setNull(6, java.sql.Types.BIGINT); else ps.setLong(6, codRel);
            ps.setLong(7, audUsuarioI);
            ps.setString(8, "U");
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // CARGA COLECTIVA
    // ══════════════════════════════════════════════════════════════════════

    /**
     * El dry-run. Valida el cabezal igual que un alta —y con los mismos mensajes— y después dice,
     * persona por persona, si entra o por qué no.
     *
     * <p>Dos consultas para todo el lote, no dos por persona.
     */
    @Override
    public List<AbonoDias> simularGrupal(AbonoDias cabecera, List<Long> codEmpleados) {
        if (cabecera == null) {
            throw new SpBusinessException("Faltan los datos del abono a cargar.");
        }
        validarDias(cabecera.getDiasAbonados(), true);   // el grupal no tiene paso de confirmación por monto
        final String motivo = validarMotivo(cabecera.getMotivo());
        final Date fecha = validarFecha(cabecera.getFecha());

        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(
                codEmpleados == null ? new ArrayList<Long>() : codEmpleados));
        ids.removeIf(id -> id == null || id <= 0);
        if (ids.isEmpty()) {
            throw new SpBusinessException("Se debe seleccionar por lo menos a un empleado.");
        }
        if (ids.size() > MAX_LOTE) {
            throw new SpBusinessException(
                    "No se pueden cargar más de " + MAX_LOTE + " empleados de una vez.");
        }

        Map<Long, Object[]> relaciones = relacionesActivas(spHelper, ids); // id → {codRel, activas, nombre}
        Map<Long, String> yaTienen = abonosEnFecha(ids, fecha);       // id → detalle del abono existente

        List<AbonoDias> previa = new ArrayList<>(ids.size());
        for (Long id : ids) {
            AbonoDias fila = new AbonoDias();
            fila.setCodEmpleado(id);
            fila.setDiasAbonados(cabecera.getDiasAbonados());
            fila.setFecha(fecha);
            fila.setMotivo(motivo);

            Object[] rel = relaciones.get(id);
            if (rel == null) {
                fila.setDatoEmpleado("Empleado " + id);
                marcarOmitido(fila, "No figura en el padrón de empleados.");
            } else {
                fila.setDatoEmpleado((String) rel[2]);
                long activas = (Long) rel[1];
                if (activas == 0) {
                    marcarOmitido(fila, "No tiene relación laboral activa.");
                } else if (activas > 1) {
                    marcarOmitido(fila, "Tiene " + activas + " relaciones laborales activas; "
                            + "corrija en el ERP antes de abonarle días.");
                } else if (yaTienen.containsKey(id)) {
                    fila.setCodRelEmplEmpr((Long) rel[0]);
                    marcarOmitido(fila, "Ya tiene un abono cargado ese día (" + yaTienen.get(id) + ").");
                } else {
                    fila.setCodRelEmplEmpr((Long) rel[0]);
                    fila.setAplica(true);
                    fila.setMotivoOmision("");
                }
            }
            decorar(fila);
            previa.add(fila);
        }

        previa.sort(Comparator.comparing(AbonoDias::getDatoEmpleado,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return previa;
    }

    /**
     * La carga, TODO O NADA.
     *
     * <p>{@code @Transactional} acá y no en el controlador porque es acá donde está el bucle, y el
     * precedente de un DAO transaccional ya existe ({@link CombustibleControlDao}). El precedente
     * vivo del módulo —{@code trs_sp_puenteVacacion}— hace su {@code BEGIN TRANSACTION} dentro del
     * SP; con estos SP de 2016, que no se pueden tocar, la única transacción disponible es ésta.
     *
     * <p><b>Nadie puede envolver esta llamada en un try/catch que se trague la excepción:</b> el
     * rollback de Spring depende de que la excepción salga. Y el controlador tiene que inyectar
     * {@link IAbonoDias}, no esta clase: la transacción vive en el proxy, no en el objeto.
     *
     * <p>Se re-simula antes de escribir en vez de confiar en lo que trajo el cliente: entre la
     * pantalla de confirmación y el botón puede haber pasado cualquier cosa —incluido el job que
     * escribe solo— y la lista de quién entra tiene que ser la de este instante.
     */
    @Override
    @Transactional
    public int aplicarGrupal(AbonoDias cabecera, List<Long> codEmpleados, long audUsuarioI) {
        SpEscritura.exigirUsuarioAuditor(audUsuarioI);

        List<AbonoDias> previa = simularGrupal(cabecera, codEmpleados);
        List<AbonoDias> entran = new ArrayList<>();
        for (AbonoDias f : previa) {
            if (f.isAplica()) entran.add(f);
        }
        if (entran.isEmpty()) {
            throw new SpBusinessException(
                    "No queda nadie a quien cargarle el abono: " + resumenOmisiones(previa));
        }

        for (AbonoDias f : entran) {
            insertar(f.getCodEmpleado(), f.getCodRelEmplEmpr(), f.getDiasAbonados(),
                     f.getFecha(), f.getMotivo(), audUsuarioI);
        }

        log.info("Abono grupal aplicado: {} empleados, fecha={}, dias={}, usuario={}",
                 entran.size(), cabecera.getFecha(), cabecera.getDiasAbonados(), audUsuarioI);
        return entran.size();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Reglas — package-private y estáticas para poder probarlas sin base
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <b>Cero NO es válido acá</b>, a diferencia de la vacación asignada: el legacy valida
     * {@code > 0} estricto ("No la cantidad de dia es menor o igual a cero") y el mínimo real de la
     * tabla es 0,5.
     */
    static void validarDias(double dias, boolean confirmado) {
        if (dias <= 0) {
            throw new SpBusinessException("Se debe asignar un número de días mayor a 0.");
        }
        if (dias > DIAS_TOPE) {
            throw new SpBusinessException(
                    "No se pueden abonar más de " + (int) DIAS_TOPE + " días en un solo registro.");
        }
        if (dias > DIAS_INUSUAL && !confirmado) {
            // 400 CONFIRMABLE y no 409: esto SÍ se resuelve reintentando —con la confirmación
            // puesta—, que es justo lo que SpConflictException documenta que no debe pasar. El
            // marcador "confirmable" del cuerpo es lo que le permite al cliente distinguirlo de un
            // 400 de regla de negocio sin adivinar por el texto.
            throw new SpConfirmableException(
                    "Va a abonar " + Fmt.dias(dias) + ", y lo habitual son hasta "
                  + Fmt.dias(DIAS_INUSUAL) + ". Confirme si es correcto.");
        }
    }

    /**
     * Obligatorio y de 2 a 50 caracteres — el largo de la columna.
     *
     * <p><b>No se trunca en silencio</b> (que es lo que hace el SP al declarar
     * {@code @motivo VARCHAR(50)}) ni se copia el regex del legacy, que además de acotar los
     * caracteres se olvidó de la ñ: "Compensación por el feriado del niño" lo rechazaba.
     */
    static String validarMotivo(String motivo) {
        String limpio = motivo == null ? "" : motivo.trim();
        if (limpio.length() < 2) {
            throw new SpBusinessException("El motivo del abono de días es obligatorio.");
        }
        if (limpio.length() > MOTIVO_MAX) {
            throw new SpBusinessException(
                    "El motivo no puede pasar de " + MOTIVO_MAX + " caracteres (tiene "
                  + limpio.length() + "). Acórtelo: la columna no da para más.");
        }
        return limpio;
    }

    /**
     * Obligatoria y no más de un año en el futuro.
     *
     * <p>El pasado se permite entero: las regularizaciones son el caso más frecuente de la tabla.
     * Los límites del calendario del legacy (de hoy−3 días a hoy−3+1 año) eran del formulario y no
     * se revalidaban al guardar; se dejan como pista para la UI, no como regla del servidor, para
     * no rechazar una regularización legítima.
     */
    static Date validarFecha(Date fecha) {
        if (fecha == null) {
            throw new SpBusinessException("Falta la fecha del abono.");
        }
        Calendar tope = Calendar.getInstance();
        tope.add(Calendar.YEAR, 1);
        if (fecha.after(tope.getTime())) {
            throw new SpBusinessException(
                    "La fecha del abono no puede estar a más de un año en el futuro.");
        }
        return fecha;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Auxiliares
    // ══════════════════════════════════════════════════════════════════════

    /**
     * El INSERT pelado, sin validar: lo comparten el alta individual —que ya validó— y el bucle de
     * la carga grupal, que corre dentro de la transacción.
     */
    private void insertar(long codEmpleado, Long codRel, double dias, Date fecha, String motivo,
                          long audUsuarioI) {
        ejecutar(SQL_ALTA, "registrar el abono de días", ps -> {
            ps.setLong(1, codEmpleado);
            ps.setDouble(2, dias);
            ps.setDate(3, new java.sql.Date(fecha.getTime()));
            ps.setString(4, motivo);
            if (codRel == null) ps.setNull(5, java.sql.Types.BIGINT); else ps.setLong(5, codRel);
            ps.setLong(6, audUsuarioI);
            ps.setString(7, "I");
        });
    }

    /**
     * Duplicado. El idéntico exacto se rechaza siempre: en 184 filas históricas no hay ni uno, así
     * que es el doble toque del teléfono con red intermitente. Mismo día con otro monto u otro
     * motivo sí pasó de verdad (un empleado con 1 día de compensación y 20,5 de regularización el
     * mismo día), así que eso sólo avisa.
     */
    private void validarSinDuplicado(long codEmpleado, long codRel, Date fecha, double dias,
                                     String motivo, boolean confirmado) {
        List<AbonoDias> delDia = new ArrayList<>();
        for (AbonoDias existente : historial(codEmpleado, codRel)) {
            if (SpEscritura.mismoDia(existente.getFecha(), fecha)) delDia.add(existente);
        }
        if (delDia.isEmpty()) return;

        for (AbonoDias e : delDia) {
            if (Double.compare(e.getDiasAbonados(), dias) == 0
                    && motivo.equalsIgnoreCase(e.getMotivo() == null ? "" : e.getMotivo().trim())) {
                throw new SpConflictException(
                        "Ese abono ya está cargado (registro " + e.getCodAbonoDias() + ", "
                      + Fmt.dias(e.getDiasAbonados()) + ", mismo motivo y misma fecha). "
                      + "No se cargó de nuevo.");
            }
        }
        if (!confirmado) {
            AbonoDias e = delDia.get(0);
            // 400 confirmable: se resuelve reintentando con la confirmación puesta.
            throw new SpConfirmableException(
                    "El empleado ya tiene " + delDia.size() + " abono(s) en esa fecha (el primero, "
                  + Fmt.dias(e.getDiasAbonados()) + ": " + e.getMotivo() + "). "
                  + "Confirme si además corresponde éste.");
        }
    }

    /**
     * La fila recién insertada. Sin id generado, se busca por (empleado, relación, fecha) y se toma
     * la de id más alto.
     */
    private AbonoDias ultimoEn(long codEmpleado, long codRel, Date fecha) {
        AbonoDias ultimo = null;
        for (AbonoDias f : historial(codEmpleado, codRel)) {
            if (!SpEscritura.mismoDia(f.getFecha(), fecha)) continue;
            if (ultimo == null || f.getCodAbonoDias() > ultimo.getCodAbonoDias()) ultimo = f;
        }
        return ultimo;
    }

    /**
     * Relación laboral activa y nombre de cada empleado del lote, en una sola consulta.
     *
     * <p>Sale de {@code p_list_AbonoDias @ACCION='R'}, que recibe el lote en {@code @codEmpleados}
     * como lista separada por comas. {@code p_list_Permiso @ACCION='E'} se le parece, pero
     * encadena {@code INNER JOIN} a cargo y sucursal: alguien sin cargo desaparecería y el motivo
     * de omisión diría algo falso.
     *
     * <p><b>Estática y con el {@code SpHelper} por parámetro</b> para que
     * {@link VacacionAsignadaDao} use ESTA y no una copia: el alta de vacación asignada también
     * tiene que resolver la relación en el servidor, y dos consultas "casi iguales" para la misma
     * pregunta es exactamente cómo se empiezan a contradecir.
     *
     * @return id → {@code {codRelEmplEmpr, cuántas activas, nombre}}; sin entrada si el empleado no
     *         existe
     */
    static Map<Long, Object[]> relacionesActivas(SpHelper spHelper, List<Long> ids) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleados", SpHelper.listaDeIds(ids));
        filtro.put("ACCION", "R");

        Map<Long, Object[]> resp = new HashMap<>();
        for (Map<String, Object> f : spHelper.ejecutarListadoDinamico("p_list_AbonoDias", filtro)) {
            Number cod = (Number) f.get("codEmpleado");
            Number rel = (Number) f.get("codRelEmplEmpr");
            Number activas = (Number) f.get("activas");
            String nombre = (String) f.get("datoEmpleado");
            resp.put(cod.longValue(), new Object[] {
                    rel == null ? null : rel.longValue(),
                    activas == null ? 0L : activas.longValue(),
                    nombre == null ? "" : nombre.trim() });
        }
        return resp;
    }

    /**
     * Quién del lote ya tiene un abono en esa fecha. Misma excepción documentada que
     * {@link #relacionesActivas}: {@code p_list_AbonoDias} filtra por un empleado a la vez y acá
     * serían 85 viajes.
     *
     * @return id → descripción corta del abono que ya existe
     */
    private Map<Long, String> abonosEnFecha(List<Long> ids, Date fecha) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleados", SpHelper.listaDeIds(ids));
        filtro.put("fecha", new java.sql.Date(fecha.getTime()));
        filtro.put("ACCION", "F");

        Map<Long, String> resp = new HashMap<>();
        for (Map<String, Object> f : spHelper.ejecutarListadoDinamico("p_list_AbonoDias", filtro)) {
            long cod = ((Number) f.get("codEmpleado")).longValue();
            double dias = ((Number) f.get("diasAbonados")).doubleValue();
            String motivo = (String) f.get("motivo");
            resp.putIfAbsent(cod, Fmt.dias(dias) + (motivo == null || motivo.trim().isEmpty()
                    ? "" : ": " + motivo.trim()));
        }
        return resp;
    }

    /**
     * La relación activa de UNA persona, con los mismos criterios que el lote.
     *
     * <p><b>Es la única resolución de relación laboral del módulo</b> — la usan el alta y el
     * historial de abono, y también los de vacación asignada. Los mensajes son genéricos a
     * propósito: nombran lo que falta ("no tiene relación laboral activa"), no la operación que se
     * estaba intentando, porque el mismo hecho vale para las cuatro.
     */
    public static long relacionActivaDe(SpHelper spHelper, long codEmpleado) {
        Object[] rel = relacionesActivas(spHelper, java.util.Collections.singletonList(codEmpleado))
                .get(codEmpleado);
        if (rel == null) {
            throw new SpBusinessException("No existe un empleado con el código " + codEmpleado + ".");
        }
        long activas = (Long) rel[1];
        if (activas == 0) {
            throw new SpBusinessException(
                    "El empleado no tiene una relación laboral activa. "
                  + "Registre la relación laboral en el ERP.");
        }
        if (activas > 1) {
            throw new SpConflictException(
                    "El empleado tiene más de una relación laboral activa (" + activas
                  + "); corrija en el ERP antes de registrarle movimientos.");
        }
        return (Long) rel[0];
    }

    private static void marcarOmitido(AbonoDias fila, String motivo) {
        fila.setAplica(false);
        fila.setMotivoOmision(motivo);
    }

    /** Para el mensaje de "no queda nadie": qué pasó, agrupado, sin listar 85 nombres. */
    private static String resumenOmisiones(List<AbonoDias> previa) {
        Map<String, Integer> porMotivo = new java.util.LinkedHashMap<>();
        for (AbonoDias f : previa) {
            porMotivo.merge(f.getMotivoOmision(), 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : porMotivo.entrySet()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(e.getValue()).append(" — ").append(e.getKey());
        }
        return sb.toString();
    }

    private AbonoDias decorar(AbonoDias a) {
        a.setDiasAbonadosTxt(Fmt.dias(a.getDiasAbonados()));
        return a;
    }

    private void ejecutar(String sql, String queHacia, PreparedStatementSetter setter) {
        SpEscritura.ejecutar(jdbcTemplate, sql, queHacia, setter);
    }
}
