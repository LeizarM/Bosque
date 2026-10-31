package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.config.SpConflictException;
import bo.bosque.com.impexpap.dto.AsientoSapPeriodo;
import bo.bosque.com.impexpap.dto.AsientoSapResumen;
import bo.bosque.com.impexpap.model.AsientoSapConfig;
import bo.bosque.com.impexpap.model.SapConexion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El asiento de la planilla de sueldos en SAP: corre el SP de la empresa en su base de SAP, arma el
 * asiento, comprueba que cuadre y lo crea por el Service Layer.
 *
 * <p>Reemplaza al script de Node que hacía lo mismo desde una PC, con las contraseñas de {@code sa} y
 * de {@code finanzas} escritas en el código. Lo de cada empresa (SP, columnas, textos) sale de
 * {@code tsap_AsientoPlanilla}, y con qué SAP se conecta, de su conexión en {@link SapConexiones}: las dos
 * se leen en cada envío, así un cambio rige sin reiniciar.
 */
@Service
public class AsientoPlanillaSapService {

    private static final Logger log = LoggerFactory.getLogger(AsientoPlanillaSapService.class);
    private static final int TIMEOUT_POR_DEFECTO = 120;

    private final JdbcTemplate jdbcTemplate;
    private final AsientoPlanillaSapPropiedades props;
    private final AsientoSapConfigService configuracion;
    private final SapConexiones conexiones;

    /** Base + referencia de los envíos en curso: dos clics seguidos no crean dos asientos. */
    private final Set<String> enviando = ConcurrentHashMap.newKeySet();

    public AsientoPlanillaSapService(JdbcTemplate jdbcTemplate, AsientoPlanillaSapPropiedades props,
                                     AsientoSapConfigService configuracion, SapConexiones conexiones) {
        this.jdbcTemplate = jdbcTemplate;
        this.props = props;
        this.configuracion = configuracion;
        this.conexiones = conexiones;
    }

    /** Las pantallas que dejan ver y enviar el asiento. */
    public String[] pantallas() {
        return props.getPantallas() == null ? new String[0] : props.getPantallas().toArray(new String[0]);
    }

    /** Las pantallas que dejan editar la configuración. */
    public String[] pantallasConfig() {
        return props.getPantallasConfig() == null ? new String[0] : props.getPantallasConfig().toArray(new String[0]);
    }

    /** Arma el asiento para revisarlo. No toca SAP. */
    public AsientoSapResumen vistaPrevia(AsientoSapPeriodo periodo) {
        exigirHabilitado();
        AsientoSapConfig cfg = configuracion.vigente(exigirEmpresa(periodo));
        return armar(periodo, cfg, conexionDe(cfg));
    }

    /**
     * Arma el asiento de nuevo (no confía en la vista previa del cliente) y lo crea en SAP.
     *
     * @return el {@code JdtNum} del asiento creado
     */
    public int enviar(AsientoSapPeriodo periodo, int codUsuario) {
        exigirHabilitado();
        AsientoSapConfig cfg = configuracion.vigente(exigirEmpresa(periodo));
        SapConexion cx = conexionDe(cfg);
        AsientoSapResumen asiento = armar(periodo, cfg, cx);
        if (!asiento.isCuadra()) {
            throw new SpBusinessException("El asiento no cuadra: débito " + asiento.getTotalDebito()
                    + ", crédito " + asiento.getTotalCredito() + ", diferencia " + asiento.getDiferencia() + ".");
        }
        String marca = asiento.getBaseSap() + "|" + asiento.getReferencia();
        if (!enviando.add(marca)) {
            throw new SpConflictException("Ese asiento ya se está enviando a SAP. Espera a que termine.");
        }
        try {
            return conexiones.sesion(cx, s -> {
                if (Boolean.TRUE.equals(cfg.getEvitarDuplicados())) {
                    List<Integer> previos = asientosConReferencia(s, cfg.getCampoReferencia().trim(), asiento.getReferencia());
                    if (!previos.isEmpty()) {
                        throw new SpConflictException("SAP ya tiene el asiento N° " + previos.get(0) + " con la referencia "
                                + asiento.getReferencia() + ". No se envió otro.");
                    }
                }
                int jdtNum = crearAsiento(s, AsientoPlanillaArmador.payload(asiento, cfg.getCampoReferencia()));
                log.info("Asiento de planilla {}/{} creado en SAP '{}' (conexion {}): JdtNum={}, referencia={}, lineas={}, codUsuario={}",
                        asiento.getMes(), asiento.getAnio(), asiento.getBaseSap(), cx.getIdConexion(), jdtNum,
                        asiento.getReferencia(), asiento.getLineas().size(), codUsuario);
                return jdtNum;
            });
        } finally {
            enviando.remove(marca);
        }
    }

    /** Inicia y cierra sesión con la conexión de esa empresa, para comprobarla sin enviar nada. */
    public String probar(int codEmpresa) {
        AsientoSapConfig cfg = configuracion.existente(codEmpresa);
        if (cfg.getIdConexion() == null) throw new SpBusinessException("Elige la conexión de SAP.");
        return conexiones.probar(cfg.getIdConexion());
    }

    /** La conexión de la configuración, que tiene que ser de la misma empresa. */
    private SapConexion conexionDe(AsientoSapConfig cfg) {
        SapConexion cx = conexiones.vigente(cfg.getIdConexion());
        if (cx.getCodEmpresa() == null || !cx.getCodEmpresa().equals(cfg.getCodEmpresa())) {
            throw new SpBusinessException("La conexión «" + cx.getNombre() + "» no es de la empresa del asiento. "
                    + "Elige otra en Planillas > Asiento SAP: configuración.");
        }
        return cx;
    }

    private AsientoSapResumen armar(AsientoSapPeriodo p, AsientoSapConfig cfg, SapConexion cx) {
        int anio = p.getAnio();
        int mes = p.getMes();
        List<Map<String, Object>> filas = ejecutarSp(comando(cfg, cx), anio, mes, cfg, cx);
        if (filas.isEmpty()) {
            throw new SpBusinessException("El SP " + cfg.getSp() + " no devolvió líneas para " + mes + "/" + anio + ".");
        }
        return AsientoPlanillaArmador.armar(filas, anio, mes, cfg, cx);
    }

    /** {@code EXEC [linked].[base].[esquema].[sp] @Añio = ?, @Mes = ?}, con cada nombre ya validado. */
    String comando(AsientoSapConfig cfg, SapConexion cx) {
        AsientoSapConfigService.validar(cfg);
        return "EXEC " + SapConexiones.nombreSp(cx, cfg.getEsquema(), cfg.getSp())
                + " @" + cfg.getParamAnio().trim() + " = ?, @" + cfg.getParamMes().trim() + " = ?";
    }

    private List<Map<String, Object>> ejecutarSp(String comando, int anio, int mes, AsientoSapConfig cfg, SapConexion cx) {
        JdbcTemplate acotado = new JdbcTemplate(jdbcTemplate.getDataSource());
        // 0 es "sin límite" para JDBC: un SAP colgado retendría una de las 5 conexiones del pool.
        acotado.setQueryTimeout(props.getTimeoutSegundos() > 0 ? props.getTimeoutSegundos() : TIMEOUT_POR_DEFECTO);
        try {
            return acotado.execute(comando, (PreparedStatementCallback<List<Map<String, Object>>>) ps -> {
                ps.setInt(1, anio);
                ps.setInt(2, mes);
                boolean hayFilas = ps.execute();
                // Un SP sin SET NOCOUNT ON manda antes los "N filas afectadas" de sus tablas temporales.
                while (!hayFilas && ps.getUpdateCount() != -1) {
                    hayFilas = ps.getMoreResults();
                }
                if (!hayFilas) return Collections.<Map<String, Object>>emptyList();
                try (ResultSet rs = ps.getResultSet()) {
                    return new RowMapperResultSetExtractor<>(new ColumnMapRowMapper()).extractData(rs);
                }
            });
        } catch (DataAccessException e) {
            log.error("Fallo {} en '{}' para {}/{}", cfg.getSp(), cx.getBaseSap(), mes, anio, e);
            throw new SpBusinessException("No se pudo ejecutar " + cfg.getSp() + " en " + cx.getBaseSap() + ": "
                    + e.getMostSpecificCause().getMessage());
        }
    }

    /** Los {@code JdtNum} de los asientos que ya tienen esa referencia en ese campo. */
    private static List<Integer> asientosConReferencia(SapSesion s, String campo, String referencia) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("$select", "JdtNum");
        query.put("$filter", campo + " eq '" + referencia.replace("'", "''") + "'");
        Object valor = s.get("JournalEntries", query, "buscar asientos anteriores").get("value");
        List<Integer> numeros = new ArrayList<>();
        if (valor instanceof List) {
            for (Object o : (List<?>) valor) {
                Object n = o instanceof Map ? ((Map<?, ?>) o).get("JdtNum") : null;
                if (n instanceof Number) numeros.add(((Number) n).intValue());
            }
        }
        return numeros;
    }

    /** Crea el asiento y devuelve su {@code JdtNum}. */
    private static int crearAsiento(SapSesion s, Map<String, Object> asiento) {
        Object n = s.post("JournalEntries", asiento, "crear el asiento").get("JdtNum");
        if (!(n instanceof Number)) {
            throw new SpBusinessException("SAP respondió sin el número del asiento (JdtNum). "
                    + "Revisa en SAP si se creó antes de volver a enviarlo.");
        }
        return ((Number) n).intValue();
    }

    private void exigirHabilitado() {
        if (!props.isHabilitado()) {
            throw new SpBusinessException("El envío del asiento de planilla a SAP está apagado en este servidor "
                    + "(sap.asiento-planilla.habilitado).");
        }
    }

    private static int exigirEmpresa(AsientoSapPeriodo p) {
        if (p == null || p.getAnio() == null || p.getMes() == null || p.getCodEmpresa() == null) {
            throw new SpBusinessException("Faltan anio, mes o codEmpresa.");
        }
        if (p.getMes() < 1 || p.getMes() > 12 || p.getAnio() < 2000 || p.getAnio() > 2100) {
            throw new SpBusinessException("El periodo " + p.getMes() + "/" + p.getAnio() + " no es válido.");
        }
        return p.getCodEmpresa();
    }
}
