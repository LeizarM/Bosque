package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.config.SpConfirmableException;
import bo.bosque.com.impexpap.config.SpConflictException;
import bo.bosque.com.impexpap.dto.CalculoAntiguedadDto;
import bo.bosque.com.impexpap.dto.DesgloseSaldoCrudoDto;
import bo.bosque.com.impexpap.dto.DesgloseSaldoDto;
import bo.bosque.com.impexpap.dto.EmpleadoColectivoDto;
import bo.bosque.com.impexpap.dto.FichaSaldoDto;
import bo.bosque.com.impexpap.dto.DiaNoHabilDto;
import bo.bosque.com.impexpap.dto.PermisoKardexDto;
import bo.bosque.com.impexpap.dto.TipoPermisoDto;
import bo.bosque.com.impexpap.dto.TramoSaldoDto;
import bo.bosque.com.impexpap.model.Permiso;
import bo.bosque.com.impexpap.utils.Fmt;
import bo.bosque.com.impexpap.utils.SpEscritura;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Lecturas de {@code p_list_Permiso} —el saldo de vacación del empleado, visto por RR.HH.— y la
 * <b>vacación colectiva</b>, que escribe {@code trh_permiso} vía {@code p_abm_Permiso}.
 *
 * <p><b>Siempre el overload {@code Map} de {@link SpHelper#ejecutarListado}</b>, nunca el de
 * modelo. Estos SP viejos filtran con {@code (@x IS NULL OR @x = col)}: el overload de modelo
 * conserva los {@code 0} de los primitivos y un {@code 0} de más devuelve la grilla vacía. El
 * único que usa el overload de modelo es {@link #diasDisponibles}, que es de producción y ya
 * andaba así.
 *
 * <h3>La escritura vive acá y no en un DAO nuevo</h3>
 * El padrón es otra ACCION del mismo {@code p_list_Permiso}, la simulación es una lectura y la
 * aplicación es un método. Un cuarto DAO con su interfaz sería dos archivos más para mantener
 * sincronizados, y el controlador ya inyecta {@link IPermiso} — o sea que el
 * {@code @Transactional} de {@link #aplicarVacacionColectiva} funciona por el proxy que ya existe,
 * sin tocar el cableado.
 */
@Repository
public class PermisoDao implements IPermiso{

    private static final Logger log = LoggerFactory.getLogger(PermisoDao.class);

    private static final String SP = "p_list_Permiso";

    /**
     * El alta de un permiso. {@code @codPermiso} va en 0 en cada iteración: es lo que le dice al SP
     * que esto es un alta y no la edición de la fila anterior.
     */
    private static final String SQL_ALTA_PERMISO =
            "execute p_abm_Permiso "
          + "@codPermiso=?, @codEmpleado=?, @codUsuarioAutorizador=?, @tipoPermiso=?, "
          + "@desde=?, @hasta=?, @motivo=?, @cantidadDias=?, @codRelEmplEmpr=?, "
          + "@audUsuarioI=?, @ACCION=?";

    /**
     * Vacación <b>GOZADA</b>. Es el valor que ya tienen 7.880 de las filas de {@code trh_permiso} y
     * el que lee {@code fn_trs_PermisoVigente}; el puente de sábado escribe exactamente el mismo.
     */
    private static final String TIPO_VACACION = "vac";

    /**
     * Vacación <b>PAGADA</b>: días que se compran en vez de tomarse. 32 filas en diez años, la
     * última el 30/07/2026. Se escribe sólo desde {@link #registrarVacacionPagada}, que es el único
     * camino con las defensas que esto necesita.
     */
    static final String TIPO_PAGO_VACACION = "pva";

    /** El combo de tipo de permiso. Nueve códigos, es una vista de catálogo sin SP de listado. */
    private static final String SQL_TIPOS =
            "SELECT codigo, descripcion FROM v_tipos WHERE grupo = 13 ORDER BY descripcion";

    /**
     * Los pagos de vacación de esa persona en ese día, con la auditoría. La ACCION {@code 'Q'} no
     * devuelve {@code audFechaI}, y sin ella no se puede distinguir el doble toque de una carga
     * deliberada. Misma excepción consciente que {@link #diasPorEmpleado}.
     */
    private static final String SQL_PVA_DEL_DIA =
            "SELECT codPermiso, cantidadDias, motivo, audFechaI"
          + "  FROM trh_permiso"
          + " WHERE codEmpleado = ? AND tipoPermiso = '" + TIPO_PAGO_VACACION + "'"
          + "   AND CONVERT(date, desde) = CONVERT(date, ?)"
          + " ORDER BY codPermiso DESC";

    /**
     * Los tipos que el legacy marca como "hay que reponer estas horas"
     * ({@code WizardPermiso.verificarTipoPermiso}). Es un cartel de pantalla: no se persiste.
     */
    private static final List<String> TIPOS_CON_REPOSICION = Arrays.asList("otro", "pcr");

    /**
     * Topes de cordura del PAGO de vacación, <b>no reglas de negocio</b>: el SP acepta cualquier
     * número y el legacy tampoco mira nada. Medido en las 32 filas reales de {@code pva}: todo vive
     * entre 0 y 30 días salvo UNA de 247, que es exactamente el error que esto ataja. Por encima de
     * {@link #DIAS_PVA_INUSUAL} se pide confirmación y por encima de {@link #DIAS_PVA_TOPE} se
     * rechaza. Mismos números que la vacación asignada, a propósito: es el mismo tipo de error.
     */
    static final double DIAS_PVA_INUSUAL = 30;
    static final double DIAS_PVA_TOPE    = 60;

    /**
     * Ventana del doble toque, igual que en la vacación asignada. Un pago IDÉNTICO (misma persona,
     * mismo día, mismos días, mismo motivo) cargado hace menos de esto no es una decisión: es el
     * botón apretado dos veces. Se rechaza <b>aunque venga confirmado</b>, porque el segundo toque
     * manda el mismo cuerpo con la confirmación adentro.
     */
    static final long VENTANA_DOBLE_TOQUE_MS = 2L * 60 * 1000;

    /** Largo de {@code trh_permiso.motivo}. Uno más y el INSERT muere por truncamiento. */
    static final int MOTIVO_MAX = 100;

    /**
     * Mínimo que hace que valga la pena grabar un permiso, y el mismo umbral del legacy. Por debajo
     * de esto el rango cayó entero en feriados o en domingo para esa persona.
     */
    static final double DIAS_MINIMO = 0.5;

    /** Tope del lote. SQL Server admite 2.100 parámetros por sentencia; el padrón activo son ~85. */
    static final int MAX_LOTE = 400;

    private final SpHelper spHelper;
    /**
     * Sólo para las dos consultas de alcance/diagnóstico de más abajo, que no tienen SP. Ver el
     * comentario de {@link #completarAlcanceD0}. La inyección es por constructor, como
     * {@code spHelper}: antes este campo era un {@code @Autowired} suelto y ni siquiera se usaba.
     */
    private final JdbcTemplate jdbcTemplate;

    public PermisoDao (SpHelper spHelper, JdbcTemplate jdbcTemplate){
        this.spHelper = spHelper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * DIAS TOTALES DISPONIBLES DE VACACION EMPLEADO
     *
     * <p><b>NO TOCAR el modelo {@link Permiso}.</b> Acá se usa el overload de MODELO, que
     * serializa los 15 campos a {@code @campo=?}. Los 15 son parámetros declarados por el SP;
     * uno de más y el {@code EXEC} falla con "too many arguments", tirando abajo este endpoint
     * de producción. Todo lo que necesite campos nuevos va en un DTO, como los de abajo.
     */
    @Override
    public List<Permiso> diasDisponibles(Permiso filtro) {
        return spHelper.ejecutarListado(
                SP,
                filtro,
                "H1",
                Permiso.class
        );
    }

    /**
     * ACCION 'C' — ficha resumen del empleado.
     *
     * @param codEmpleado único filtro real de esta acción.
     *
     * <p>NOTA 1: la columna {@code totalDias} del SP llega siempre {@code 0.0} (es un literal en
     *         el SELECT); se calcula acá — {@code diasNoUsados + diasAbonados} — igual que hacía
     *         {@code PermisoManagedBean} en el legacy, para que el número no dependa del cliente.
     * <p>NOTA 2: el join a {@code tb_relEmplEmpr ... esActivo=1} no tiene {@code TOP(1)}. Si el
     *         SP devuelve más de una fila, el empleado tiene 2+ relaciones activas → 409, nunca
     *         elegir una en silencio.
     * <p>NOTA 3 — <b>SUPUESTO D0 — pendiente de confirmación de RR.HH. (ver plan §5):</b> todas
     *         las sumas del SP están filtradas por la relación laboral ACTIVA. Medido hoy: el
     *         78 % de los permisos de empleados con relación activa vive en otra relación y NO
     *         entra en este número. Por eso {@link #completarAlcanceD0} agrega los tres campos
     *         que permiten rotularlo en pantalla.
     */
    @Override
    public List<FichaSaldoDto> fichaSaldoEmpleado(long codEmpleado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);

        List<FichaSaldoDto> r = spHelper.ejecutarListado(SP, filtro, "C", FichaSaldoDto.class);

        if (r.size() > 1) {
            throw new SpConflictException(conflictoRelacionesActivas(r.size()));
        }
        if (r.isEmpty()) {
            explicarVacio(codEmpleado);   // 400 con el motivo, o vuelve y responde 204
            return r;
        }

        FichaSaldoDto d = r.get(0);
        d.setTotalDias(d.getDiasNoUsados() + d.getDiasAbonados());
        d.setDiasNoUsadosTxt(Fmt.dias(d.getDiasNoUsados()));
        d.setDiasAbonadosTxt(Fmt.dias(d.getDiasAbonados()));
        d.setTotalDiasTxt(Fmt.dias(d.getTotalDias()));
        completarAlcanceD0(d);
        return r;
    }

    /**
     * ACCION 'D' — el saldo desglosado en 5 tramos.
     *
     * <p>El SP devuelve los tramos APLANADOS (5 columnas de etiqueta + 5 de monto, emparejadas
     * sólo por el orden del SELECT); acá se vuelven a juntar de a pares y se calculan los
     * totales. Las etiquetas se copian <b>literales</b>: las arma el propio SP con {@code UPDATE}s
     * y el criterio de aceptación #4 las compara carácter por carácter contra el modal del ERP
     * legacy — traen un espacio antes del paréntesis de cierre, dos espacios después de
     * "cumplido" en la segunda y espacio final en la tercera y la cuarta. <b>Nada de
     * {@code trim()}.</b>
     *
     * <p><b>SUPUESTO D1 — pendiente de confirmación de RR.HH. (ver plan §5):</b> el tramo 3
     * (ACUMULADA, las duodécimas del año en curso) va con signo {@code INFO} y <b>no entra en el
     * total</b>, exactamente como en el legacy ({@code PermisoManagedBean:724-726}). Se muestran
     * los dos números rotulados en vez de elegir cuál es "el" saldo.
     *
     * <p><b>SUPUESTO D2 — pendiente de confirmación de RR.HH. (ver plan §5):</b>
     * {@code desgloseAplicable = datoAnios >= 2}. Con menos, el SP cae en el {@code ELSE} del
     * tramo 2 y suma toda la historia de la relación en vez del año, y la etiqueta anuncia un
     * "-1º año cumplido" que no existe. Hoy le pasa a 57 de 85 activos. La bandera viaja y la UI
     * decide: no se esconden tramos del lado del servidor.
     *
     * @return {@code null} si el SP no devolvió filas y no había nada que explicar (→ 204)
     */
    @Override
    public DesgloseSaldoDto desgloseSaldo(long codEmpleado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);

        List<DesgloseSaldoCrudoDto> r = spHelper.ejecutarListado(SP, filtro, "D", DesgloseSaldoCrudoDto.class);

        if (r.size() > 1) {
            throw new SpConflictException(conflictoRelacionesActivas(r.size()));
        }
        if (r.isEmpty()) {
            explicarVacio(codEmpleado);
            return null;
        }

        DesgloseSaldoCrudoDto c = r.get(0);
        DesgloseSaldoDto resp = new DesgloseSaldoDto();

        resp.setCodEmpleado(c.getCodEmpleado());
        resp.setDatoEmpleado(c.getDatoEmpleado());
        resp.setDatoEmpresa(c.getDatoEmpresa());
        resp.setDatoCargo(c.getDatoCargo());
        resp.setDatoAntiguedad(c.getDatoAntiguedad());
        resp.setDatoFecIniBenef(c.getDatoFecIniBenef());
        resp.setDatoAnios(c.getDatoAnios());
        resp.setFecIniPenUl(c.getFecIniPenUl());
        resp.setFecIniUlt(c.getFecIniUlt());
        resp.setCodRelEmplEmpr(c.getCodRelEmplEmpr());

        // El literal exacto que inserta el SP cuando el empleado no tiene codRelBeneficios.
        resp.setTieneFechaBeneficio(!"Sin Asignar".equals(c.getDatoFecIniBenef()));
        resp.setDesgloseAplicable(c.getDatoAnios() >= 2);

        resp.setTramos(new ArrayList<>(Arrays.asList(
                TramoSaldoDto.de(TramoSaldoDto.SALDO_PENULTIMO, c.getDatoDetSaldoPenUltAnio(),
                        c.getMontoSaldoPenUltAnio(), TramoSaldoDto.DEBE),
                TramoSaldoDto.de(TramoSaldoDto.ASIGNADA_ANIO,   c.getDatoVacAsignadaSist(),
                        c.getMontoVacAsignadaSist(),  TramoSaldoDto.DEBE),
                TramoSaldoDto.de(TramoSaldoDto.ACUMULADA,       c.getDatoVacAcumulad(),
                        c.getMontoVacAcumulad(),      TramoSaldoDto.INFO),
                TramoSaldoDto.de(TramoSaldoDto.UTILIZADA,       c.getDatoVacUtilizada(),
                        c.getMontoVacUtilizada(),     TramoSaldoDto.HABER),
                TramoSaldoDto.de(TramoSaldoDto.PROGRAMADA,      c.getDatoVacProgramada(),
                        c.getMontoVacProgramada(),    TramoSaldoDto.HABER))));

        // Los tres tramos que se pueden abrir: son sumas de filas de trh_permiso y el detalle
        // sale de las ACCIONes 'H', 'J' y 'K'. Los otros dos no se abren y por eso no lo dicen:
        // ASIGNADA_ANIO suma trh_vacacionAsignada (está en su propia pestaña) y ACUMULADA es
        // f_calcVacacionesAnioLaboral, una fórmula, no una lista de permisos.
        for (TramoSaldoDto t : resp.getTramos()) {
            t.setTieneDetalle(TramoSaldoDto.SALDO_PENULTIMO.equals(t.getClave())
                    || TramoSaldoDto.UTILIZADA.equals(t.getClave())
                    || TramoSaldoDto.PROGRAMADA.equals(t.getClave()));
        }

        double debe  = c.getMontoSaldoPenUltAnio() + c.getMontoVacAsignadaSist();  // el tramo 3 NO entra
        double haber = c.getMontoVacUtilizada()    + c.getMontoVacProgramada();
        resp.setMontoTotalDebe(debe);
        resp.setMontoTotalHaber(haber);
        resp.setMontoTotal(debe - haber);
        resp.setMontoTotalTxt(Fmt.dias(debe - haber));
        resp.setEtiquetaTotal(DesgloseSaldoDto.ETIQUETA_TOTAL);

        return resp;
    }

    /**
     * ACCION 'U' — calculadora de antigüedad.
     *
     * <p>El cuerpo entero de esa ACCION es
     * {@code select dbo.f_calcDiasLaborablesExt(@desde, @hasta) as datoAntCalc}: una fila, una
     * columna, prosa. <b>La frase se devuelve literal y no se parsea</b> — el criterio #7 la
     * compara contra el modal {@code apyCalcAntModal} del legacy.
     *
     * <p>El SP no valida nada (ni fechas nulas ni rangos absurdos); esas reglas están en el
     * controlador, que es donde se convierten en un 400 con mensaje.
     *
     * @return siempre un DTO cuando el SP responde; {@code null} sólo si no devolvió fila
     */
    @Override
    public CalculoAntiguedadDto calcularAntiguedad(Date desde, Date hasta) {
        Map<String, Object> filtro = new LinkedHashMap<>();
        filtro.put("desde", desde);
        filtro.put("hasta", hasta);

        List<CalculoAntiguedadDto> r =
                spHelper.ejecutarListado(SP, filtro, "U", CalculoAntiguedadDto.class);
        return r.isEmpty() ? null : r.get(0);
    }

    /**
     * ACCION 'G' — de quién es la boleta.
     *
     * <p>Se lee con {@code ejecutarListadoDinamico} y no con un {@code BeanPropertyRowMapper}
     * sobre {@link Permiso} a propósito: de las 12 columnas que devuelve esa ACCION acá sólo
     * interesa una, y mapear el resto ataría un gate de seguridad a los tipos primitivos del
     * modelo (un {@code audUsuarioI} en NULL haría reventar el mapeo y, con él, la descarga del
     * reporte). Ojo: ese método NO agrega {@code @ACCION} solo, va en el Map.
     */
    @Override
    public Long codEmpleadoDeBoleta(long codPermiso) {
        Map<String, Object> filtro = new LinkedHashMap<>();
        filtro.put("codPermiso", codPermiso);
        filtro.put("ACCION", "G");

        List<Map<String, Object>> filas = spHelper.ejecutarListadoDinamico(SP, filtro);
        if (filas.isEmpty()) return null;

        Object cod = filas.get(0).get("codEmpleado");
        return cod == null ? null : ((Number) cod).longValue();
    }

    // ==================================================================
    // VACACIÓN COLECTIVA
    // ==================================================================

    /**
     * ACCION 'E' — el padrón de la carga colectiva.
     *
     * <p><b>El SP reusa {@code @codPermiso} como filtro de EMPRESA</b>
     * ({@code WHERE @codPermiso IS NULL OR @codPermiso = te.codEmpresa}). Es un defecto suyo, de
     * 2016, y no se puede tocar: acá se traduce y se documenta, para que nadie mande el código de
     * un permiso creyendo que filtra por permiso.
     *
     * <p>Se lee con {@code ejecutarListadoDinamico} y no con un {@code BeanPropertyRowMapper}
     * porque el SP llama {@code relacionVigente} a lo que el resto del módulo llama
     * {@code codRelEmplEmpr}: mapear por nombre dejaría la relación en 0 <b>en silencio</b>, que es
     * exactamente la clase de error que este módulo ya pagó una vez.
     *
     * <p>Ojo con lo que ese SELECT deja afuera: encadena {@code INNER JOIN} a cargo y sucursal, así
     * que alguien con relación activa pero sin cargo de planilla no aparece en el padrón. Es
     * correcto para elegir gente —sin sucursal no hay feriados que descontarle— y es la razón por
     * la que la simulación NO usa esta consulta: ahí el que falta tiene que salir igual, marcado.
     */
    @Override
    public List<EmpleadoColectivoDto> padronColectivo(Long codEmpresa) {
        Map<String, Object> filtro = new LinkedHashMap<>();
        // Sin empresa, el parámetro NO viaja: el SP ya tiene DEFAULT NULL y su WHERE es
        // `@codPermiso IS NULL OR ...`. Mandar un null suelto obligaría al driver a adivinar el
        // tipo (setNull con Types.NULL), que con el de SQL Server es un error de conversión crudo.
        if (codEmpresa != null && codEmpresa > 0) {
            filtro.put("codPermiso", codEmpresa);   // sí: @codPermiso ES el filtro de empresa
        }
        filtro.put("ACCION", "E");

        List<Map<String, Object>> filas = spHelper.ejecutarListadoDinamico(SP, filtro);
        List<EmpleadoColectivoDto> padron = new ArrayList<>(filas.size());
        for (Map<String, Object> f : filas) {
            EmpleadoColectivoDto d = new EmpleadoColectivoDto();
            d.setCodEmpleado(numero(f.get("codEmpleado")));
            d.setDatoEmpleado(texto(f.get("datoEmpleado")));
            d.setCodRelEmplEmpr(numero(f.get("relacionVigente")));
            d.setCodSucursal(numero(f.get("codSucursal")));
            d.setDatoSucursal(texto(f.get("datoSucursal")));
            d.setDatoEmpresa(texto(f.get("datoEmpresa")));
            // El padrón es una lista para elegir: todavía no hay rango, así que no hay días ni
            // motivo por el que excluir a nadie. Eso lo dice la simulación.
            d.setDias(0);
            d.setDiasTxt(Fmt.dias(0));
            d.setEntra(true);
            d.setDetalle("");
            padron.add(d);
        }
        return padron;
    }

    /**
     * El dry-run de la vacación colectiva. <b>No escribe nada.</b>
     *
     * <h3>Las validaciones del cabezal, cortando en el primer fallo</h3>
     * Son las del legacy: rango completo, {@code desde} y {@code hasta} en el <b>mismo año
     * calendario</b>, motivo no vacío y al menos un empleado.
     *
     * <p><b>El "cantidadDias &ge; 0,5" del legacy va al final y no en el medio</b>, y es a
     * propósito: allá el número lo traía el formulario y se podía mirar antes de saber a quiénes se
     * lo iban a aplicar. Acá el servidor lo CALCULA, y lo calcula por persona, así que no existe
     * hasta que hay padrón. Se comprueba igual —si a nadie le llega a medio día, el lote se
     * rechaza— pero después de resolver a quiénes.
     *
     * <p>Los límites de calendario del formulario legacy (desde hoy−3 días, horas acotadas a
     * 08:30–12:30 y 14:30–18:30) <b>no se replican como regla del servidor</b>: eran del formulario
     * y no se revalidaban al guardar. Rechazar por eso mataría la regularización de un feriado que
     * se carga tarde, que es el caso más frecuente (el puente de Semana Santa 2026 se cargó 39 días
     * después). Quedan como pista para la UI.
     */
    @Override
    public List<EmpleadoColectivoDto> simularVacacionColectiva(List<Long> codEmpleados, Date desde,
                                                               Date hasta, String motivo) {
        validarRango(desde, hasta);
        validarMotivo(motivo);   // corta acá; el valor recortado lo usa el que escribe
        return evaluar(codEmpleados, desde, hasta);
    }

    /**
     * Las fechas: rango completo y {@code desde}/{@code hasta} en el <b>mismo año calendario</b>.
     * Separado de {@link #simularVacacionColectiva} porque las tres pantallas individuales lo
     * necesitan sin el motivo, que en el modal se tipea después de elegir las fechas.
     */
    private static void validarRango(Date desde, Date hasta) {
        if (desde == null || hasta == null) {
            throw new SpBusinessException("Debe indicar desde cuándo y hasta cuándo va la vacación.");
        }
        if (!desde.before(hasta)) {
            throw new SpBusinessException(
                    "La fecha y hora de fin tienen que ser posteriores a las de inicio.");
        }
        if (anioDe(desde) != anioDe(hasta)) {
            // Regla del legacy. Un permiso a caballo de dos años se le imputa a la gestión
            // equivocada y descuadra el saldo de las dos.
            throw new SpBusinessException(
                    "La vacación tiene que empezar y terminar en el mismo año: " + anioDe(desde)
                  + " y " + anioDe(hasta) + " son gestiones distintas. Cárguela en dos tramos.");
        }
    }

    /**
     * A quiénes les entra el rango, con cuántos días y por qué no. <b>Es el motor único</b>: lo
     * llaman la simulación colectiva, la previsualización individual y las tres altas, así que el
     * número que muestra la pantalla es literalmente el que se graba.
     */
    private List<EmpleadoColectivoDto> evaluar(List<Long> codEmpleados, Date desde, Date hasta) {
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

        // Dos consultas para todo el lote, no dos por persona. La primera es LA MISMA que resuelve
        // la relación en el abono: una sola definición de "relación laboral activa" en el módulo.
        Map<Long, Object[]> relaciones = AbonoDiasDao.relacionesActivas(jdbcTemplate, ids);
        Map<Long, Map<String, Object>> calculo = diasPorEmpleado(ids, desde, hasta);

        List<EmpleadoColectivoDto> previa = new ArrayList<>(ids.size());
        for (Long id : ids) {
            EmpleadoColectivoDto d = new EmpleadoColectivoDto();
            d.setCodEmpleado(id);

            Object[] rel = relaciones.get(id);
            Map<String, Object> c = calculo.get(id);
            d.setDatoEmpleado(rel == null ? "Empleado " + id : texto(rel[2]));
            if (c != null) {
                d.setCodSucursal(numero(c.get("codSucursal")));
                d.setDatoSucursal(texto(c.get("datoSucursal")));
                d.setDatoEmpresa(texto(c.get("datoEmpresa")));
            }

            double dias = c == null ? 0 : redondear(((Number) c.get("dias")).doubleValue());
            d.setDias(dias);
            d.setDiasTxt(Fmt.dias(dias));

            if (rel == null) {
                omitir(d, "No figura en el padrón de empleados.");
            } else if ((Long) rel[1] == 0L) {
                omitir(d, "No tiene relación laboral activa.");
            } else if ((Long) rel[1] > 1L) {
                omitir(d, "Tiene " + rel[1] + " relaciones laborales activas; corrija en el ERP "
                        + "antes de darle vacación.");
            } else if (c != null && esUno(c.get("yaTienePermiso"))) {
                d.setCodRelEmplEmpr((Long) rel[0]);
                omitir(d, "Ya tiene un permiso cargado que se cruza con ese rango"
                        + rangoQueChoca(c) + ".");
            } else if (dias < DIAS_MINIMO) {
                d.setCodRelEmplEmpr((Long) rel[0]);
                omitir(d, "Ese rango no le deja ni medio día hábil (feriado o descanso en su "
                        + "sucursal).");
            } else {
                d.setCodRelEmplEmpr((Long) rel[0]);
                d.setEntra(true);
                d.setDetalle("");
            }
            previa.add(d);
        }

        previa.sort(java.util.Comparator.comparing(EmpleadoColectivoDto::getDatoEmpleado,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        return previa;
    }

    /**
     * La carga, TODO O NADA.
     *
     * <p>{@code @Transactional} acá y no en el controlador porque es acá donde está el bucle, igual
     * que {@code AbonoDiasDao.aplicarGrupal}. Dos consecuencias que hay que respetar: el
     * controlador inyecta {@link IPermiso} (la transacción vive en el proxy, no en el objeto) y
     * <b>nadie puede envolver esta llamada en un try/catch que se trague la excepción</b>, porque
     * el rollback de Spring depende de que la excepción salga.
     *
     * <p>Se re-simula adentro de la transacción en vez de confiar en lo que trajo el cliente: entre
     * la pantalla de confirmación y el botón puede haber escrito otra persona, y quién entra —y con
     * cuántos días— tiene que ser el de este instante.
     */
    @Override
    @Transactional
    public int aplicarVacacionColectiva(List<Long> codEmpleados, Date desde, Date hasta,
                                        String motivo, long codAutorizador) {
        SpEscritura.exigirUsuarioAuditor(codAutorizador);

        List<EmpleadoColectivoDto> previa =
                simularVacacionColectiva(codEmpleados, desde, hasta, motivo);
        List<EmpleadoColectivoDto> entran = new ArrayList<>();
        for (EmpleadoColectivoDto d : previa) {
            if (d.isEntra()) entran.add(d);
        }
        if (entran.isEmpty()) {
            throw new SpBusinessException(
                    "No queda nadie a quien darle la vacación: " + resumenOmisiones(previa));
        }

        final String motivoOk = validarMotivo(motivo);
        for (EmpleadoColectivoDto d : entran) {
            // El empleado, la relación y los días DE CADA UNO. Los del cabezal no se copian: los
            // días son distintos por sucursal y la relación es de cada persona.
            insertarPermiso(d.getCodEmpleado(), d.getCodRelEmplEmpr(), TIPO_VACACION, desde, hasta,
                            motivoOk, d.getDias(), codAutorizador, "registrar la vacación colectiva");
        }

        log.info("Vacacion colectiva aplicada: {} empleados, desde={}, hasta={}, autorizador={}",
                 entran.size(), desde, hasta, codAutorizador);
        return entran.size();
    }

    // ══════════════════════════════════════════════════════════════════════
    // NÓMINA DE PERMISOS y las tres altas individuales
    // ══════════════════════════════════════════════════════════════════════

    /**
     * La grilla del kardex — {@code p_list_Permiso} ACCION {@code 'Q'}, con los cuatro filtros del
     * legacy y la misma semántica del SP:
     * <ul>
     *   <li>{@code tipoPermiso} vacío, {@code null} o {@code "0"} (lo que manda el combo "Todos")
     *       viaja como NULL = todos los tipos.</li>
     *   <li>{@code fechaInicio} compara contra el <b>inicio</b> del permiso
     *       ({@code CONVERT(date,tp.desde) >= @desde}) y {@code fechaFin} contra el <b>fin</b>.</li>
     *   <li>{@code fecRango} es otra cosa: {@code @fecRango BETWEEN tp.desde AND tp.hasta}, o sea
     *       "quién estaba de permiso el día X". El rango del permiso atrapa a la fecha, no al
     *       revés.</li>
     * </ul>
     * Los filtros que no vienen no se mandan (quedan en el DEFAULT NULL del SP): mandar un 0 en
     * {@code codRelEmplEmpr} devolvería la grilla vacía en vez de todas las relaciones.
     *
     * <p>{@code codEmpleado} sale siempre del empleado seleccionado en pantalla, nunca del filtro:
     * sin él, la ACCION {@code 'Q'} devuelve los 8.459 permisos de la empresa entera.
     */
    /**
     * El detalle de un tramo del desglose: <b>qué permisos suman ese número</b>.
     *
     * <p>Tres ACCIONes distintas del mismo SP, con la misma forma de salida que el kardex:
     * <ul>
     *   <li>{@code SALDO_PENULTIMO} → {@code 'H'}: vac y pva con {@code desde <= fecIniPenUl}.</li>
     *   <li>{@code UTILIZADA} → {@code 'J'}: vac y pva entre {@code fecIniPenUl} y hoy.</li>
     *   <li>{@code PROGRAMADA} → {@code 'K'}: vac con {@code desde > hoy}.</li>
     * </ul>
     *
     * <p><b>Las fechas las resuelve el servidor, no el cliente.</b> {@code fecIniPenUl} sale de la
     * misma ACCION {@code 'D'} que produjo el monto del tramo, así que el detalle no puede
     * discrepar del total por una fecha calculada de otra manera del lado de la app. Cuesta una
     * consulta más y evita la clase de bug que nadie encuentra mirando la pantalla.
     *
     * <p>{@code 'J'} filtra {@code tipoPermiso='PVA'} en mayúsculas: la base es
     * {@code Modern_Spanish_CI_AS}, o sea insensible a mayúsculas, y por eso engancha las filas
     * {@code 'pva'} igual. No se toca el SP.
     */
    /**
     * Los días del rango que <b>no descuentan</b> vacación, con el motivo de cada uno.
     *
     * <p>Es lo que la pantalla necesita para explicar la resta en vez de mostrar sólo el total.
     * Devuelve las dos causas que el cliente no puede deducir de la fecha:
     * <ul>
     *   <li><b>Feriados</b> — globales, o de la sucursal del empleado ({@code trh_diaNoLaborable}
     *       más {@code trh_diaNoLaborable_sucursal}).</li>
     *   <li><b>Sábados que no le tocan</b> — el sábado está en el rol y la persona no tiene
     *       asignación. Los domingos no vienen: el cliente los sabe solo.</li>
     * </ul>
     *
     * <p><b>Es la misma regla que aplica {@code f_CalcularDiasHabilesPermiso}</b>, que es la que
     * de verdad graba los días. Si las dos se separan, la pantalla explicaría una resta distinta
     * de la que se guarda: cualquier cambio en la función se replica acá y al revés.
     *
     * <p>Consulta directa y no un SP, como {@link #completarAlcanceD0} y {@link #diasPorEmpleado}:
     * no existe una ACCION que devuelva esto. Está en el pedido al DBA junto con las otras dos.
     */
    @Override
    public List<DiaNoHabilDto> diasNoHabiles(long codEmpleado, Date desde, Date hasta) {
        if (codEmpleado <= 0 || desde == null || hasta == null) {
            return new ArrayList<>();
        }
        final String sql =
                "  SELECT d.fecha AS fecha, 'FERIADO' AS tipo, d.motivo AS motivo"
              + "    FROM trh_diaNoLaborable d"
              + "   WHERE d.fecha BETWEEN ? AND ?"
              + "     AND ( NOT EXISTS (SELECT 1 FROM trh_diaNoLaborable_sucursal ds"
              + "                        WHERE ds.codDiaNoLaborable = d.codDiaNoLaborable)"
              + "        OR EXISTS (SELECT 1 FROM trh_diaNoLaborable_sucursal ds"
              + "                    WHERE ds.codDiaNoLaborable = d.codDiaNoLaborable"
              + "                      AND ds.codSucursal = ("
              + "                            SELECT TOP 1 cs.codSucursal"
              + "                              FROM trh_empleadoCargo ec"
              + "                              JOIN tb_cargo_sucursal cs"
              + "                                ON ec.codCargoSucursal = cs.codCargoSucursal"
              + "                             WHERE ec.codEmpleado = ?"
              + "                             ORDER BY ec.fechaInicio DESC)))"
              + "   UNION ALL"
              + "  SELECT s.fecha, 'SABADO_LIBRE', 'No le toca trabajar este sábado'"
              + "    FROM trs_Sabado s"
              + "   WHERE s.fecha BETWEEN ? AND ?"
              + "     AND NOT EXISTS (SELECT 1"
              + "                       FROM trs_Asignacion a"
              + "                       JOIN trs_Participante p ON p.idParticipante = a.idParticipante"
              + "                      WHERE a.idSabado = s.idSabado AND p.codEmpleado = ?)"
              + "   ORDER BY fecha";

        java.sql.Date d1 = new java.sql.Date(desde.getTime());
        java.sql.Date d2 = new java.sql.Date(hasta.getTime());
        return jdbcTemplate.query(sql,
                new Object[]{ d1, d2, codEmpleado, d1, d2, codEmpleado },
                (rs, i) -> {
                    DiaNoHabilDto d = new DiaNoHabilDto();
                    d.setFecha(rs.getDate("fecha"));
                    d.setTipo(rs.getString("tipo"));
                    d.setMotivo(rs.getString("motivo"));
                    return d;
                });
    }

    /**
     * <b>Quién está fuera</b> — los permisos de TODA la empresa en una fecha o en un rango.
     *
     * <p>Es la misma ACCION {@code 'Q'} del kardex pero <b>sin {@code codEmpleado}</b>: ahí el SP
     * deja de filtrar por persona y devuelve la empresa entera. {@link #kardex} lo prohíbe a
     * propósito —sin empleado son 8.459 filas— así que esto es un método aparte con su propio
     * candado: <b>exige al menos un filtro de fecha</b>. Sin eso devolvería el kardex histórico
     * completo de todos, que no es una pantalla, es una descarga.
     *
     * <p>Los dos usos que cubre, con los mismos parámetros del legacy:
     * <ul>
     *   <li>{@code fecRango} = un día → «quién estaba de permiso ese día». El SP compara
     *       {@code @fecRango BETWEEN tp.desde AND tp.hasta}: el rango del permiso atrapa a la
     *       fecha, no al revés.</li>
     *   <li>{@code desde}/{@code hasta} → «quiénes salen en esa ventana», comparando contra el
     *       inicio del permiso.</li>
     * </ul>
     *
     * <p>Reemplaza al cronograma del sistema anterior, cuyos tres reportes Jasper quedaron sin
     * datos: los alimentaba una colección Java desde {@code cronogramaBackBean}, que no existe en
     * el código fuente. Acá el dato sale del SP y no de un bean perdido.
     */
    @Override
    public List<PermisoKardexDto> quienEstaFuera(Date fecRango, Date desde, Date hasta) {
        if (fecRango == null && desde == null && hasta == null) {
            throw new SpBusinessException(
                    "Indique una fecha o un rango: sin eso la consulta devuelve el histórico completo.");
        }
        Map<String, Object> filtro = new HashMap<>();
        if (fecRango != null) filtro.put("fecRango", new java.sql.Timestamp(fecRango.getTime()));
        if (desde    != null) filtro.put("desde",    new java.sql.Timestamp(desde.getTime()));
        if (hasta    != null) filtro.put("hasta",    new java.sql.Timestamp(hasta.getTime()));

        List<PermisoKardexDto> filas = spHelper.ejecutarListado(SP, filtro, "Q", PermisoKardexDto.class);
        for (PermisoKardexDto f : filas) {
            f.setHoras(f.getCantidadDias() * 8.0);
            f.setCantidadDiasTxt(Fmt.dias(f.getCantidadDias()));
            f.setHorasTxt(Fmt.horas(f.getHoras()));
        }
        return filas;
    }

    /**
     * <b>Boletas entre fechas</b> — {@code p_list_Permiso @ACCION='W'}.
     *
     * <p>El buscador global de boletas del sistema anterior, el que se usa cuando alguien pide una
     * copia y no se sabe de quién era. Devuelve toda la empresa; el nombre viene en
     * {@code datoEmpleado}.
     *
     * <p><b>No es lo mismo que {@link #quienEstaFuera}</b>, aunque las dos sean globales:
     * <ul>
     *   <li>{@code 'W'} acota por permisos <b>contenidos</b> en la ventana
     *       ({@code desde >= @desde AND hasta <= @hasta}): las boletas emitidas en ese período.</li>
     *   <li>{@code 'Q'} con {@code fecRango} pregunta quién estaba de permiso <b>un día</b>, y ahí
     *       el rango del permiso atrapa a la fecha.</li>
     * </ul>
     * Una vacación del 28 de julio al 3 de agosto sale en «quién estaba fuera el 1 de agosto» y
     * <b>no</b> sale al buscar boletas de agosto, porque empezó en julio.
     *
     * <p>Mismo candado que la otra global: <b>exige al menos una fecha</b>. Sin filtro son las
     * 8.459 filas de la tabla.
     */
    @Override
    public List<PermisoKardexDto> boletasEntreFechas(Date desde, Date hasta, String tipoPermiso) {
        if (desde == null && hasta == null) {
            throw new SpBusinessException(
                    "Indique al menos una fecha: sin eso la búsqueda devuelve todas las boletas.");
        }
        Map<String, Object> filtro = new HashMap<>();
        if (desde != null) filtro.put("desde", new java.sql.Timestamp(desde.getTime()));
        if (hasta != null) filtro.put("hasta", new java.sql.Timestamp(hasta.getTime()));
        String tipo = tipoPermiso == null ? "" : tipoPermiso.trim();
        if (!tipo.isEmpty() && !"0".equals(tipo)) {
            filtro.put("tipoPermiso", tipo);
        }

        List<PermisoKardexDto> filas = spHelper.ejecutarListado(SP, filtro, "W", PermisoKardexDto.class);
        for (PermisoKardexDto f : filas) {
            f.setHoras(f.getCantidadDias() * 8.0);
            f.setCantidadDiasTxt(Fmt.dias(f.getCantidadDias()));
            f.setHorasTxt(Fmt.horas(f.getHoras()));
        }
        return filas;
    }

    @Override
    public List<PermisoKardexDto> detalleDeTramo(long codEmpleado, String clave) {
        DesgloseSaldoDto d = desgloseSaldo(codEmpleado);
        if (d == null) return new ArrayList<>();

        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);
        if (d.getCodRelEmplEmpr() > 0) {
            filtro.put("codRelEmplEmpr", d.getCodRelEmplEmpr());
        }

        final String accion;
        if (TramoSaldoDto.SALDO_PENULTIMO.equals(clave)) {
            accion = "H";
            filtro.put("desde", d.getFecIniPenUl());
        } else if (TramoSaldoDto.UTILIZADA.equals(clave)) {
            accion = "J";
            filtro.put("desde", d.getFecIniPenUl());
            filtro.put("tipoPermiso", "vac");
        } else if (TramoSaldoDto.PROGRAMADA.equals(clave)) {
            accion = "K";
            filtro.put("tipoPermiso", "vac");
        } else {
            throw new SpBusinessException("Ese tramo no tiene detalle para mostrar.");
        }

        List<PermisoKardexDto> filas = spHelper.ejecutarListado(SP, filtro, accion, PermisoKardexDto.class);
        for (PermisoKardexDto f : filas) {
            f.setHoras(f.getCantidadDias() * 8.0);
            f.setCantidadDiasTxt(Fmt.dias(f.getCantidadDias()));
            f.setHorasTxt(Fmt.horas(f.getHoras()));
        }
        return filas;
    }

    @Override
    public List<PermisoKardexDto> kardex(long codEmpleado, Long codRelEmplEmpr, String tipoPermiso,
                                         Date fechaInicio, Date fechaFin, Date fecRango) {
        if (codEmpleado <= 0) {
            throw new SpBusinessException("Debe seleccionar un empleado.");
        }
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);
        if (codRelEmplEmpr != null && codRelEmplEmpr > 0) {
            filtro.put("codRelEmplEmpr", codRelEmplEmpr);
        }
        String tipo = tipoPermiso == null ? "" : tipoPermiso.trim();
        if (!tipo.isEmpty() && !"0".equals(tipo)) {
            filtro.put("tipoPermiso", tipo);
        }
        if (fechaInicio != null) filtro.put("desde",    new java.sql.Timestamp(fechaInicio.getTime()));
        if (fechaFin    != null) filtro.put("hasta",    new java.sql.Timestamp(fechaFin.getTime()));
        if (fecRango    != null) filtro.put("fecRango", new java.sql.Timestamp(fecRango.getTime()));

        List<PermisoKardexDto> filas = spHelper.ejecutarListado(SP, filtro, "Q", PermisoKardexDto.class);
        for (PermisoKardexDto f : filas) {
            // La única cuenta de esta pantalla, y es la misma con la que la función SQL produjo los
            // días (SUM(minutos)/480). No hay un segundo motor: se re-expresa el número grabado.
            f.setHoras(f.getCantidadDias() * 8.0);
            f.setCantidadDiasTxt(Fmt.dias(f.getCantidadDias()));
            f.setHorasTxt(Fmt.horas(f.getHoras()));
        }
        return filas;
    }

    /**
     * El combo. {@code incluirVacacionYPago = false} replica {@code Tipos.cargarList13A()} del
     * legacy —los 7 tipos del modal de permiso, sin {@code vac} ni {@code pva}, que tienen su
     * propia pantalla—; {@code true} devuelve los 9 para el filtro de la nómina.
     */
    @Override
    public List<TipoPermisoDto> tiposPermiso(boolean incluirVacacionYPago) {
        List<TipoPermisoDto> tipos = jdbcTemplate.query(SQL_TIPOS,
                (rs, i) -> new TipoPermisoDto(rs.getString("codigo"), rs.getString("descripcion")));
        if (!incluirVacacionYPago) {
            tipos.removeIf(t -> TIPO_VACACION.equals(t.getCodigo())
                             || TIPO_PAGO_VACACION.equals(t.getCodigo()));
        }
        return tipos;
    }

    /**
     * Lo que muestran EN VIVO los tres campos calculados del modal ("Horas de permiso", "Días de
     * permiso", "Total días de vacación") mientras el usuario mueve las fechas — y lo que decide si
     * el botón Guardar tiene sentido: si vuelve {@code entra = false}, {@code detalle} dice por qué.
     *
     * <p><b>No calcula nada acá.</b> Es {@link #evaluar}, o sea la misma
     * {@code f_CalcularDiasHabilesPermiso} que graba el alta. El legacy tenía dos motores en Java
     * ({@code calcularDiasHrsSolicitadas} para el permiso y {@code calcularHrsPorEmpleado} para la
     * vacación) que ni siquiera coincidían entre sí ni con la función SQL; reimplementar cualquiera
     * de los dos sería una tercera cifra sobre el mismo número.
     *
     * <p><b>El radio "Horario Estándar / Continuo" no llega hasta acá y no es un descuido:</b> la
     * función lo deduce de la hora de fin ({@code IF CAST(@hasta AS TIME) > '17:30'} conmuta a tope
     * de 600 min/día y una hora de almuerzo). En la pantalla el radio elige la ventana que ofrece
     * el reloj, no el motor de cálculo. Si viajara como parámetro habría que ignorarlo —radio que
     * miente— o calcular en Java —tercera cifra—.
     */
    @Override
    public EmpleadoColectivoDto previsualizarPermiso(long codEmpleado, String tipoPermiso,
                                                     Date desde, Date hasta) {
        if (codEmpleado <= 0) {
            throw new SpBusinessException("Debe seleccionar un empleado.");
        }
        validarRango(desde, hasta);

        EmpleadoColectivoDto d = evaluar(Collections.singletonList(codEmpleado), desde, hasta).get(0);
        d.setHoras(d.getDias() * 8.0);
        d.setHorasTxt(Fmt.horas(d.getHoras()));
        d.setHorasAReponer(horasAReponer(tipoPermiso, d.getDias()));
        d.setHorasAReponerTxt(Fmt.horas(d.getHorasAReponer()));
        return d;
    }

    /**
     * Alta individual: los modales "Programar permiso" (tipo del combo) y "Programar vacación"
     * ({@code tipoPermiso = 'vac'}). Es el mismo endpoint porque la única diferencia es el tipo.
     *
     * <p><b>Es {@link #aplicarVacacionColectiva} con un solo empleado.</b> Mismas validaciones,
     * mismo cálculo, mismo INSERT: si a esta persona no le entra el rango, el mensaje que recibe es
     * palabra por palabra el que la colectiva pone en su columna "por qué no entra".
     *
     * <p><b>El control de duplicado es el cruce de rangos</b>, no una ventana de tiempo: dos altas
     * idénticas se solapan consigo mismas, así que el segundo toque choca contra el permiso que
     * acaba de escribir el primero y se rechaza. Es la misma comprobación que ya protege a la
     * colectiva, y la única que tiene el módulo — {@code p_abm_Permiso} no valida nada.
     */
    @Override
    @Transactional
    public PermisoKardexDto registrarPermiso(long codEmpleado, String tipoPermiso, Date desde,
                                             Date hasta, String motivo, long codAutorizador) {
        SpEscritura.exigirUsuarioAuditor(codAutorizador);
        if (codEmpleado <= 0) {
            throw new SpBusinessException("No se seleccionó al empleado.");
        }
        final String tipo = validarTipoPermiso(tipoPermiso);
        validarRango(desde, hasta);
        final String motivoOk = validarMotivo(motivo);

        EmpleadoColectivoDto d = evaluar(Collections.singletonList(codEmpleado), desde, hasta).get(0);
        if (!d.isEntra()) {
            throw new SpBusinessException(d.getDetalle());
        }

        insertarPermiso(codEmpleado, d.getCodRelEmplEmpr(), tipo, desde, hasta, motivoOk,
                        d.getDias(), codAutorizador, "registrar el permiso");
        log.info("Alta de permiso: empleado={}, tipo={}, desde={}, hasta={}, dias={}, usuario={}",
                 codEmpleado, tipo, desde, hasta, d.getDias(), codAutorizador);

        return releerUltimo(codEmpleado, tipo, desde, hasta);
    }

    /**
     * Modal "Pago de vacaciones" ({@code tipoPermiso = 'pva'}): días que se PAGAN en vez de
     * tomarse. Es la pantalla de más riesgo del módulo y la que menos ayuda tiene del SP.
     *
     * <p>Cuatro diferencias con las otras dos altas, todas heredadas de {@code savePagoDiasVacac}:
     * <ol>
     *   <li>{@code hasta = desde} — lo forzaba el legacy y por eso las 32 filas históricas tienen
     *       las dos fechas iguales. Se fuerza acá también.</li>
     *   <li><b>Los días los tipea el usuario</b>: {@code f_CalcularDiasHabilesPermiso} no aplica
     *       (no hay rango horario que medir). Sin motor de cálculo, las validaciones de abajo son
     *       toda la defensa que hay.</li>
     *   <li>El cruce de rangos <b>no protege</b>: con {@code desde == hasta} el rango es de ancho
     *       cero y {@code @desde &lt; p.hasta AND @hasta &gt; p.desde} nunca da verdadero contra sí
     *       mismo. Por eso acá sí hace falta la ventana del doble toque.</li>
     *   <li>Se compara contra el saldo. El legacy no lo hacía: hay una fila histórica de 247 días
     *       pagados.</li>
     * </ol>
     *
     * @param confirmado el usuario ya vio el impacto real (saldo antes y después) y lo aceptó.
     */
    @Override
    @Transactional
    public PermisoKardexDto registrarVacacionPagada(long codEmpleado, Date fecha, double dias,
                                                    String motivo, long codAutorizador,
                                                    boolean confirmado) {
        SpEscritura.exigirUsuarioAuditor(codAutorizador);
        if (codEmpleado <= 0) {
            throw new SpBusinessException("No se seleccionó al empleado.");
        }
        if (fecha == null) {
            throw new SpBusinessException("Falta la fecha del pago de vacación.");
        }
        final String motivoOk = validarMotivo(motivo);
        validarDiasPagados(dias, confirmado);

        final long codRel = AbonoDiasDao.relacionActivaDe(jdbcTemplate, codEmpleado);
        // El saldo ANTES que el duplicado, a propósito: las dos preguntas comparten el mismo flag
        // `confirmado` (como en toda la escritura del módulo), así que la que se hace primero es la
        // que el usuario ve. Se elige la que habla de plata. El doble toque real no depende de esto:
        // su 409 se dispara aunque el flag venga en true.
        validarContraSaldo(codEmpleado, dias, confirmado);
        validarPagoSinRepetir(codEmpleado, fecha, dias, motivoOk, confirmado);

        // hasta = desde: regla del legacy, y lo que tienen las 32 filas reales.
        insertarPermiso(codEmpleado, codRel, TIPO_PAGO_VACACION, fecha, fecha, motivoOk, dias,
                        codAutorizador, "registrar el pago de vacación");
        log.info("Alta de vacacion PAGADA: empleado={}, relacion={}, fecha={}, dias={}, usuario={}",
                 codEmpleado, codRel, fecha, dias, codAutorizador);

        return releerUltimo(codEmpleado, TIPO_PAGO_VACACION, fecha, fecha);
    }

    // "Buscar Vac Ganadas" del kardex —la vacación ASIGNADA por rango de fechas,
    // p_list_vacacionAsignada ACCION 'D'— NO vive acá: es IVacacionAsignada.buscarPorRango, en el
    // DAO que ya es dueño de trh_vacacionAsignada. Esta nota está para que nadie la busque acá.

    // ==================================================================
    // Auxiliares
    // ==================================================================

    /**
     * Los días hábiles de CADA empleado del lote, más la sucursal con la que se calcularon y si ya
     * tiene un permiso que se cruce con el rango. Una sola consulta para todo el lote.
     *
     * <p><b>El cálculo no se reimplementa en Java.</b> Lo hace
     * {@code dbo.f_CalcularDiasHabilesPermiso}, que es la que ya usa el puente de sábado migrado y
     * la que descuenta {@code trh_diaNoLaborable} filtrado por
     * {@code trh_diaNoLaborable_sucursal}. Escribir un segundo motor en Java sería una tercera
     * verdad sobre el mismo número —el legacy ya tiene la suya y no coincide con ésta— y el saldo
     * del empleado dejaría de cuadrar.
     *
     * <p><b>La sucursal que se muestra es la que USÓ la función</b>, resuelta por
     * {@code trh_empleadoCargo.codCargoSucursal}. Ojo: el padrón ({@code ACCION 'E'}) muestra la
     * de {@code codCargoSucPlanilla}, que puede ser otra. Acá manda la de la función, porque es la
     * que explica por qué a esta persona le tocaron 4,5 días y no 5.
     *
     * <p>El chequeo de cruce es el mismo {@code @desde &lt; p.hasta AND @hasta &gt; p.desde} que
     * usan {@code p_abm_SolicitudVacacion} y {@code trs_sp_puenteVacacion}: es la ÚNICA protección
     * contra el doble permiso, porque {@code p_abm_Permiso} no valida nada. Sin esto, correr la
     * carga dos veces le descuenta los días dos veces a todo el mundo — y en {@code trh_permiso}
     * ya hay 291 pares solapados de esa forma.
     *
     * <p><b>Excepción consciente a la regla de oro del ERP</b> (toda la lógica en SPs), la misma
     * que ya documentan {@link #completarAlcanceD0} y {@code AbonoDiasDao.relacionesActivas}:
     * ninguna ACCION devuelve esto. <b>TODO:</b> pedir al DBA una ACCION que lo haga y borrar este
     * método.
     */
    private Map<Long, Map<String, Object>> diasPorEmpleado(List<Long> ids, Date desde, Date hasta) {
        StringBuilder marcadores = new StringBuilder(ids.size() * 2);
        for (int i = 0; i < ids.size(); i++) marcadores.append(i == 0 ? "?" : ",?");

        String sql =
                "SELECT e.codEmpleado"
              + "     , ISNULL(cs.codSucursal, 0)                       AS codSucursal"
              + "     , ISNULL(s.nombre, '')                            AS datoSucursal"
              + "     , ISNULL(emp.nombre, '')                          AS datoEmpresa"
              + "     , dbo.f_CalcularDiasHabilesPermiso(e.codEmpleado, ?, ?) AS dias"
              + "     , CASE WHEN pe.codPermiso IS NULL THEN 0 ELSE 1 END     AS yaTienePermiso"
              + "     , pe.desde                                        AS chocaDesde"
              + "     , pe.hasta                                        AS chocaHasta"
              + "  FROM tb_empleado e"
              + "  OUTER APPLY (SELECT TOP 1 c2.codSucursal"
              + "                 FROM trh_empleadoCargo ec"
              + "                 JOIN tb_cargo_sucursal c2 ON c2.codCargoSucursal = ec.codCargoSucursal"
              + "                WHERE ec.codEmpleado = e.codEmpleado"
              + "                ORDER BY ec.fechaInicio DESC) cs"
              + "  LEFT JOIN tb_sucursal s   ON s.codSucursal = cs.codSucursal"
              + "  LEFT JOIN tb_empresa  emp ON emp.codEmpresa = s.codEmpresa"
              + "  OUTER APPLY (SELECT TOP 1 p.codPermiso, p.desde, p.hasta"
              + "                 FROM trh_permiso p"
              + "                WHERE p.codEmpleado = e.codEmpleado"
              + "                  AND ? < p.hasta AND ? > p.desde) pe"
              + " WHERE e.codEmpleado IN (" + marcadores + ")";

        java.sql.Timestamp d = new java.sql.Timestamp(desde.getTime());
        java.sql.Timestamp h = new java.sql.Timestamp(hasta.getTime());
        Object[] args = new Object[ids.size() + 4];
        args[0] = d; args[1] = h; args[2] = d; args[3] = h;
        for (int i = 0; i < ids.size(); i++) args[i + 4] = ids.get(i);

        Map<Long, Map<String, Object>> resp = new HashMap<>();
        for (Map<String, Object> f : jdbcTemplate.queryForList(sql, args)) {
            resp.put(((Number) f.get("codEmpleado")).longValue(), f);
        }
        return resp;
    }

    /**
     * <b>El único INSERT de {@code trh_permiso} del módulo.</b> Lo llaman la vacación colectiva y
     * las tres altas individuales; lo único que cambia entre ellas son el tipo, las fechas y los
     * días. {@code codPermiso = 0} y {@code ACCION = 'I'} es lo que le dice al SP que esto es un
     * alta y no la edición de otra fila.
     *
     * <p>Un segundo camino de escritura significaría dos juegos de validaciones que se van
     * separando con cada cambio; ésta es la razón por la que existe el método.
     */
    private void insertarPermiso(long codEmpleado, long codRelEmplEmpr, String tipoPermiso,
                                 Date desde, Date hasta, String motivo, double dias,
                                 long codAutorizador, String queHacia) {
        SpEscritura.ejecutar(jdbcTemplate, SQL_ALTA_PERMISO, queHacia, ps -> {
            ps.setLong(1, 0L);
            ps.setLong(2, codEmpleado);
            ps.setLong(3, codAutorizador);
            ps.setString(4, tipoPermiso);
            ps.setTimestamp(5, new java.sql.Timestamp(desde.getTime()));
            ps.setTimestamp(6, new java.sql.Timestamp(hasta.getTime()));
            ps.setString(7, motivo);
            ps.setDouble(8, dias);
            ps.setLong(9, codRelEmplEmpr);
            ps.setLong(10, codAutorizador);
            ps.setString(11, "I");
        });
    }

    /**
     * La fila recién insertada. {@code p_abm_Permiso} no devuelve {@code SCOPE_IDENTITY} ni hace un
     * SELECT de retorno, así que la única forma de saber qué quedó es releer: se filtra la nómina
     * por (empleado, tipo, día de inicio, día de fin) y se toma el {@code codPermiso} más alto, que
     * es el último en entrar. Mismo recurso que usa el alta de vacación asignada.
     *
     * <p>No se compara la hora exacta a propósito: {@code datetime} de SQL Server redondea a
     * 1/300 de segundo y la igualdad por milisegundos fallaría contra la fila que acabamos de
     * escribir.
     */
    private PermisoKardexDto releerUltimo(long codEmpleado, String tipoPermiso, Date desde, Date hasta) {
        PermisoKardexDto ultima = null;
        for (PermisoKardexDto f : kardex(codEmpleado, null, tipoPermiso, desde, hasta, null)) {
            if (ultima == null || f.getCodPermiso() > ultima.getCodPermiso()) ultima = f;
        }
        if (ultima == null) {
            throw new SpBusinessException(
                    "El permiso no aparece en la base después de guardarlo. Vuelva a consultar la "
                  + "nómina antes de reintentar.");
        }
        return ultima;
    }

    /**
     * El tipo tiene que ser uno de {@code v_tipos} grupo 13. El legacy sólo miraba que tuviera 2
     * caracteres ("Tipo de Permiso Indeterminado"), así que un tipo inventado entraba a la tabla y
     * después desaparecía de la grilla —la ACCION {@code 'Q'} hace JOIN con {@code v_tipos}—: la
     * fila descontaba días y no se veía en ningún lado.
     *
     * <p>{@code pva} se rechaza acá aunque exista: el pago de vacación tiene su propio método,
     * con el control de saldo y de doble toque que este camino no hace.
     */
    private String validarTipoPermiso(String tipoPermiso) {
        String tipo = tipoPermiso == null ? "" : tipoPermiso.trim();
        if (TIPO_PAGO_VACACION.equalsIgnoreCase(tipo)) {
            throw new SpBusinessException(
                    "El pago de vacación se registra desde la pantalla de vacación pagada.");
        }
        for (TipoPermisoDto t : tiposPermiso(true)) {
            if (t.getCodigo().equalsIgnoreCase(tipo)) return t.getCodigo();
        }
        throw new SpBusinessException("Tipo de permiso indeterminado: elija uno de la lista.");
    }

    /**
     * "Horas a reponer" del modal. Regla literal del legacy
     * ({@code WizardPermiso.verificarTipoPermiso}): {@code otro} y {@code pcr} deben reponer las
     * horas del permiso; el resto, cero.
     *
     * <p><b>No se persiste</b> — {@code p_abm_Permiso} no recibe {@code cantDiasAdeuad} y
     * {@code trh_permiso} no tiene la columna. Es un cartel para la pantalla, y por eso es una
     * función pura: no toca la base y se puede probar sin Spring.
     */
    static double horasAReponer(String tipoPermiso, double dias) {
        String tipo = tipoPermiso == null ? "" : tipoPermiso.trim().toLowerCase();
        return TIPOS_CON_REPOSICION.contains(tipo) ? dias * 8.0 : 0.0;
    }

    /**
     * Días a pagar. Son días PAGADOS y el SP acepta cualquier número, así que acá está todo lo que
     * separa un pago legítimo de un error de tipeo.
     *
     * @param confirmado si el usuario ya confirmó un valor fuera del rango histórico.
     */
    static void validarDiasPagados(double dias, boolean confirmado) {
        if (dias < DIAS_MINIMO) {
            throw new SpBusinessException(
                    "La cantidad de días a pagar tiene que ser de por lo menos "
                  + Fmt.dias(DIAS_MINIMO) + ".");
        }
        if (Math.abs(dias * 2 - Math.round(dias * 2)) > 1e-9) {
            // El input del legacy es step=0.5 y no existe un pago de 1/3 de día.
            throw new SpBusinessException("Los días a pagar van de medio en medio día.");
        }
        if (dias > DIAS_PVA_TOPE) {
            throw new SpBusinessException(
                    "No se pueden pagar más de " + (int) DIAS_PVA_TOPE + " días en un solo registro.");
        }
        if (dias > DIAS_PVA_INUSUAL && !confirmado) {
            throw new SpConfirmableException(
                    "Va a pagar " + Fmt.dias(dias) + ", y en las 32 vacaciones pagadas que hay "
                  + "cargadas lo habitual son hasta " + Fmt.dias(DIAS_PVA_INUSUAL)
                  + ". Confirme si es correcto.");
        }
    }

    /**
     * Los dos niveles de duplicado del pago de vacación, con la misma regla que la vacación
     * asignada:
     * <ul>
     *   <li><b>Doble toque</b> — un pago IDÉNTICO (mismos días, mismo motivo, mismo día) escrito
     *       hace menos de {@link #VENTANA_DOBLE_TOQUE_MS}: 409, <b>aunque venga confirmado</b>. El
     *       segundo toque del teléfono manda el mismo cuerpo, confirmación incluida.</li>
     *   <li><b>Repetición deliberada</b> — ya hay un pago ese día, pero viejo o distinto: 400
     *       confirmable. Existe el caso real (dos pagos el 24/08/2020 al mismo empleado).</li>
     * </ul>
     */
    private void validarPagoSinRepetir(long codEmpleado, Date fecha, double dias, String motivo,
                                       boolean confirmado) {
        List<Map<String, Object>> previos = jdbcTemplate.queryForList(
                SQL_PVA_DEL_DIA, codEmpleado, new java.sql.Timestamp(fecha.getTime()));
        if (previos.isEmpty()) return;

        long ahora = System.currentTimeMillis();
        for (Map<String, Object> p : previos) {
            double diasPrevios = ((Number) p.get("cantidadDias")).doubleValue();
            Date audFechaI = (Date) p.get("audFechaI");
            boolean identico = Double.compare(diasPrevios, dias) == 0
                    && motivo.equalsIgnoreCase(texto(p.get("motivo")));
            boolean reciente = audFechaI != null
                    && ahora - audFechaI.getTime() < VENTANA_DOBLE_TOQUE_MS;
            if (identico && reciente) {
                throw new SpConflictException(
                        "Ese mismo pago de vacación acaba de registrarse (registro "
                      + numero(p.get("codPermiso")) + "). No se cargó de nuevo.");
            }
        }
        if (!confirmado) {
            Map<String, Object> ultimo = previos.get(0);
            throw new SpConfirmableException(
                    "Ese día ya tiene un pago de vacación de "
                  + Fmt.dias(((Number) ultimo.get("cantidadDias")).doubleValue()) + " (registro "
                  + numero(ultimo.get("codPermiso")) + "). Si de verdad hay que pagar otro, "
                  + "confirme.");
        }
    }

    /**
     * El saldo alcanza. <b>Es un aviso, no un bloqueo</b>: el saldo de la ficha está filtrado por
     * la relación laboral ACTIVA (supuesto D0) y el 78 % de los permisos vive en otra relación, así
     * que rechazar de plano bloquearía pagos legítimos. Se pide confirmación diciendo el número, que
     * es exactamente lo que el legacy no hacía.
     */
    private void validarContraSaldo(long codEmpleado, double dias, boolean confirmado) {
        if (confirmado) return;
        List<FichaSaldoDto> ficha = fichaSaldoEmpleado(codEmpleado);
        if (ficha.isEmpty()) return;                 // ya se explicó solo; no se traba el pago

        double saldo = ficha.get(0).getTotalDias();
        if (dias > saldo) {
            throw new SpConfirmableException(
                    "Le va a pagar " + Fmt.dias(dias) + " y su saldo es de " + Fmt.dias(saldo)
                  + ": quedaría en " + Fmt.dias(saldo - dias) + ". Confirme si corresponde igual.");
        }
    }

    /** El rango del permiso que choca, para que el mensaje diga cuál y no sólo que hay uno. */
    private static String rangoQueChoca(Map<String, Object> c) {
        Object d = c.get("chocaDesde");
        Object h = c.get("chocaHasta");
        if (!(d instanceof Date) || !(h instanceof Date)) return "";
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return " (del " + f.format((Date) d) + " al " + f.format((Date) h) + ")";
    }

    /**
     * Obligatorio y de 2 a 100 caracteres — el largo de {@code trh_permiso.motivo}.
     *
     * <p>No se trunca en silencio, que es lo que haría el SP al declarar {@code VARCHAR(100)}: se
     * avisa y se corta la operación antes de escribir 40 filas con el motivo mutilado.
     */
    static String validarMotivo(String motivo) {
        String limpio = motivo == null ? "" : motivo.trim();
        if (limpio.length() < 2) {
            // Sin "colectiva": lo comparten la colectiva y las tres altas individuales.
            throw new SpBusinessException("El motivo del permiso es obligatorio.");
        }
        if (limpio.length() > MOTIVO_MAX) {
            throw new SpBusinessException(
                    "El motivo no puede pasar de " + MOTIVO_MAX + " caracteres (tiene "
                  + limpio.length() + "). Acórtelo: la columna no da para más.");
        }
        return limpio;
    }

    /**
     * A octavos de día, que es la unidad real: la función devuelve minutos/480 y sale con basura
     * binaria ({@code 0.4999999999}), que después se compara contra el umbral de 0,5.
     */
    private static double redondear(double dias) {
        return Math.round(dias * 8.0) / 8.0;
    }

    private static int anioDe(Date fecha) {
        Calendar c = Calendar.getInstance();
        c.setTime(fecha);
        return c.get(Calendar.YEAR);
    }

    private static void omitir(EmpleadoColectivoDto d, String motivo) {
        d.setEntra(false);
        d.setDetalle(motivo);
    }

    /** Para el mensaje de "no queda nadie": qué pasó, agrupado, sin listar 85 nombres. */
    private static String resumenOmisiones(List<EmpleadoColectivoDto> previa) {
        Map<String, Integer> porMotivo = new LinkedHashMap<>();
        for (EmpleadoColectivoDto d : previa) {
            porMotivo.merge(d.getDetalle(), 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : porMotivo.entrySet()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(e.getValue()).append(" — ").append(e.getKey());
        }
        return sb.toString();
    }

    private static long numero(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private String conflictoRelacionesActivas(int filas) {
        return "El empleado tiene más de una relación laboral activa (" + filas
                + "); corrija en el ERP antes de consultar.";
    }

    /**
     * <b>SUPUESTO D0 — pendiente de confirmación de RR.HH. (ver plan §5).</b> Los tres campos que
     * permiten rotular el alcance del saldo: desde cuándo corre la relación vigente y cuánta
     * historia quedó afuera.
     *
     * <p><b>Excepción consciente a la regla de oro del ERP</b> (toda la lógica en SPs): esto es
     * SQL directo desde Java. Ninguna ACCION de {@code p_list_Permiso} devuelve estos datos y el
     * alcance de la fase no permite tocar los SP. Es una lectura pura, una sola consulta, sin
     * lógica de negocio. <b>TODO:</b> pedir al DBA una ACCION que los devuelva y borrar este
     * método.
     *
     * <p>Sin este rótulo un saldo correcto se lee como un saldo equivocado — que es exactamente
     * lo que bloquea el criterio de aceptación #6.
     */
    private void completarAlcanceD0(FichaSaldoDto d) {
        final String sql =
                "SELECT ree.fechaIni AS relacionVigenteDesde"
              + "     , (SELECT COUNT(*) FROM tb_relEmplEmpr r"
              + "         WHERE r.codEmpleado = ree.codEmpleado AND r.codRelEmplEmpr <> ree.codRelEmplEmpr) AS relacionesAnteriores"
              + "     , (SELECT COUNT(*) FROM trh_permiso p"
              + "         WHERE p.codEmpleado = ree.codEmpleado AND p.codRelEmplEmpr <> ree.codRelEmplEmpr)"
              + "     + (SELECT COUNT(*) FROM trh_vacacionAsignada v"
              + "         WHERE v.codEmpleado = ree.codEmpleado AND v.codRelEmplEmpr <> ree.codRelEmplEmpr) AS movimientosFuera"
              + "  FROM tb_relEmplEmpr ree"
              + " WHERE ree.codRelEmplEmpr = ?";

        List<Map<String, Object>> filas =
                jdbcTemplate.queryForList(sql, d.getDatoRelEmplEmprVigente());
        if (filas.isEmpty()) return;   // la relación se borró entre una consulta y la otra

        Map<String, Object> f = filas.get(0);
        d.setRelacionVigenteDesde((Date) f.get("relacionVigenteDesde"));
        d.setTieneRelacionesAnteriores(((Number) f.get("relacionesAnteriores")).intValue() > 0);
        d.setMovimientosFueraDeRelacionVigente(((Number) f.get("movimientosFuera")).intValue());
    }

    /**
     * La segunda consulta discriminante: por qué el SP no devolvió nada.
     *
     * <p>Las ACCIONes 'C' y 'D' encadenan tres {@code INNER JOIN} —relación activa, cargo,
     * empresa— y cualquiera de ellos hace desaparecer la fila entera, sin decir cuál. Decir "no
     * tiene relación laboral activa" cuando lo que falta es el cargo manda a alguien a revisar
     * el lugar equivocado, así que se pregunta y se responde con el motivo real.
     *
     * <p>Una sola consulta con tres {@code EXISTS} — no tiene {@code FROM}, así que siempre
     * devuelve exactamente una fila. Misma excepción a la regla de oro que
     * {@link #completarAlcanceD0}, mismo TODO.
     *
     * <p>Si los tres dan bien, <b>no lanza nada</b>: el vacío es legítimo (→ 204).
     */
    private void explicarVacio(long codEmpleado) {
        final String sql =
                "SELECT CASE WHEN EXISTS(SELECT 1 FROM tb_empleado WHERE codEmpleado = ?) THEN 1 ELSE 0 END AS existe"
              + "     , CASE WHEN EXISTS(SELECT 1 FROM tb_relEmplEmpr WHERE codEmpleado = ? AND esActivo = 1) THEN 1 ELSE 0 END AS conRelacion"
              + "     , CASE WHEN EXISTS(SELECT 1 FROM trh_empleadoCargo WHERE codEmpleado = ?) THEN 1 ELSE 0 END AS conCargo";

        Map<String, Object> f = jdbcTemplate.queryForMap(sql, codEmpleado, codEmpleado, codEmpleado);

        if (!esUno(f.get("existe"))) {
            throw new SpBusinessException(
                    "No existe un empleado con el codigo " + codEmpleado + ".");
        }
        if (!esUno(f.get("conRelacion"))) {
            throw new SpBusinessException(
                    "El empleado no tiene una relacion laboral activa, asi que no se le puede "
                  + "calcular el saldo de vacacion. Registre la relacion laboral en el ERP.");
        }
        if (!esUno(f.get("conCargo"))) {
            throw new SpBusinessException(
                    "El empleado no tiene ningun cargo asignado, y el saldo se calcula sobre el "
                  + "cargo. Asignele un cargo en el ERP.");
        }
        // Todo en orden: el vacío es legítimo y el controlador responde 204.
    }

    private boolean esUno(Object valor) {
        return valor != null && ((Number) valor).intValue() == 1;
    }
}
