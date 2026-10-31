package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.config.SpConflictException;
import bo.bosque.com.impexpap.dto.AsientoSapPeriodo;
import bo.bosque.com.impexpap.dto.AsientoSapResumen;
import bo.bosque.com.impexpap.model.AsientoSapConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El asiento de la planilla de sueldos en SAP: corre el SP de la empresa en su base de SAP, arma el
 * asiento, comprueba que cuadre y lo crea por el Service Layer.
 *
 * <p>Reemplaza al script de Node que hacía lo mismo desde una PC, con las contraseñas de {@code sa} y
 * de {@code finanzas} escritas en el código. Aquí el SP se alcanza por el linked server con la conexión
 * de siempre, y lo de cada empresa (SP, columnas, usuario y contraseña cifrada del Service Layer) sale
 * de {@code tsap_AsientoPlanilla} en cada envío: un cambio en la configuración rige sin reiniciar.
 */
@Service
public class AsientoPlanillaSapService {

    private static final Logger log = LoggerFactory.getLogger(AsientoPlanillaSapService.class);
    private static final int TIMEOUT_POR_DEFECTO = 120;

    private final JdbcTemplate jdbcTemplate;
    private final AsientoPlanillaSapPropiedades props;
    private final AsientoSapConfigService configuracion;
    private final SapServiceLayerCliente sap;

    /** Base + referencia de los envíos en curso: dos clics seguidos no crean dos asientos. */
    private final Set<String> enviando = ConcurrentHashMap.newKeySet();

    public AsientoPlanillaSapService(JdbcTemplate jdbcTemplate, AsientoPlanillaSapPropiedades props,
                                     AsientoSapConfigService configuracion, SapServiceLayerCliente sap) {
        this.jdbcTemplate = jdbcTemplate;
        this.props = props;
        this.configuracion = configuracion;
        this.sap = sap;
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
        return armar(periodo, configuracion.vigente(exigirEmpresa(periodo)));
    }

    /**
     * Arma el asiento de nuevo (no confía en la vista previa del cliente) y lo crea en SAP.
     *
     * @return el {@code JdtNum} del asiento creado
     */
    public int enviar(AsientoSapPeriodo periodo, int codUsuario) {
        exigirHabilitado();
        AsientoSapConfig cfg = configuracion.vigente(exigirEmpresa(periodo));
        String clave = configuracion.clave(cfg);
        AsientoSapResumen asiento = armar(periodo, cfg);
        if (!asiento.isCuadra()) {
            throw new SpBusinessException("El asiento no cuadra: débito " + asiento.getTotalDebito()
                    + ", crédito " + asiento.getTotalCredito() + ", diferencia " + asiento.getDiferencia() + ".");
        }
        String marca = asiento.getBaseSap() + "|" + asiento.getReferencia();
        if (!enviando.add(marca)) {
            throw new SpConflictException("Ese asiento ya se está enviando a SAP. Espera a que termine.");
        }
        try {
            SapServiceLayerCliente.Sesion sesion =
                    sap.abrirSesion(cfg.getSlUrl(), asiento.getBaseSap(), cfg.getSlUsuario().trim(), clave);
            try {
                if (Boolean.TRUE.equals(cfg.getEvitarDuplicados())) {
                    List<Integer> previos = sap.asientosConReferencia(sesion, cfg.getCampoReferencia().trim(),
                            asiento.getReferencia());
                    if (!previos.isEmpty()) {
                        throw new SpConflictException("SAP ya tiene el asiento N° " + previos.get(0) + " con la referencia "
                                + asiento.getReferencia() + ". No se envió otro.");
                    }
                }
                int jdtNum = sap.crearAsiento(sesion, AsientoPlanillaArmador.payload(asiento, cfg.getCampoReferencia()));
                log.info("Asiento de planilla {}/{} creado en SAP '{}': JdtNum={}, referencia={}, lineas={}, codUsuario={}",
                        asiento.getMes(), asiento.getAnio(), asiento.getBaseSap(), jdtNum, asiento.getReferencia(),
                        asiento.getLineas().size(), codUsuario);
                return jdtNum;
            } finally {
                sap.cerrarSesion(sesion);
            }
        } finally {
            enviando.remove(marca);
        }
    }

    /** Inicia y cierra sesión con la configuración guardada de esa empresa, para comprobarla sin enviar nada. */
    public String probar(int codEmpresa) {
        AsientoSapConfig cfg = configuracion.vigente(codEmpresa);
        String clave = configuracion.clave(cfg);
        SapServiceLayerCliente.Sesion sesion =
                sap.abrirSesion(cfg.getSlUrl(), cfg.getBaseSap().trim(), cfg.getSlUsuario().trim(), clave);
        sap.cerrarSesion(sesion);
        return "Conexión correcta: " + cfg.getSlUsuario().trim() + " entró a " + cfg.getBaseSap().trim() + ".";
    }

    private AsientoSapResumen armar(AsientoSapPeriodo p, AsientoSapConfig cfg) {
        int anio = p.getAnio();
        int mes = p.getMes();
        List<Map<String, Object>> filas = ejecutarSp(comando(cfg), anio, mes, cfg);
        if (filas.isEmpty()) {
            throw new SpBusinessException("El SP " + cfg.getSp() + " no devolvió líneas para " + mes + "/" + anio + ".");
        }
        return AsientoPlanillaArmador.armar(filas, anio, mes, cfg);
    }

    /** {@code EXEC [linked].[base].[esquema].[sp] @Añio = ?, @Mes = ?}, con cada nombre ya validado. */
    String comando(AsientoSapConfig cfg) {
        AsientoSapConfigService.validar(cfg);
        String esquema = cfg.getEsquema() == null || cfg.getEsquema().trim().isEmpty() ? "dbo" : cfg.getEsquema().trim();
        StringBuilder sb = new StringBuilder("EXEC ");
        String linked = cfg.getLinkedServer() == null ? "" : cfg.getLinkedServer().trim();
        if (!linked.isEmpty()) {
            sb.append('[').append(linked).append("].");
        }
        sb.append('[').append(cfg.getBaseSap().trim()).append("].[").append(esquema)
          .append("].[").append(cfg.getSp().trim()).append("] @")
          .append(cfg.getParamAnio().trim()).append(" = ?, @")
          .append(cfg.getParamMes().trim()).append(" = ?");
        return sb.toString();
    }

    private List<Map<String, Object>> ejecutarSp(String comando, int anio, int mes, AsientoSapConfig cfg) {
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
            log.error("Fallo {} en '{}' para {}/{}", cfg.getSp(), cfg.getBaseSap(), mes, anio, e);
            throw new SpBusinessException("No se pudo ejecutar " + cfg.getSp() + " en " + cfg.getBaseSap() + ": "
                    + e.getMostSpecificCause().getMessage());
        }
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
