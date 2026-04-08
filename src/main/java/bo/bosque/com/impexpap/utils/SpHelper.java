package bo.bosque.com.impexpap.utils;

import bo.bosque.com.impexpap.config.SpBusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class SpHelper {

    private static final Logger logger = LoggerFactory.getLogger(SpHelper.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // Caché para no leer los metadatos de la base de datos en cada petición
    private final Map<String, SimpleJdbcCall> spCache = new ConcurrentHashMap<>();

    @Autowired
    public SpHelper(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Ejecuta un SP de ABM usando un Map con EXACTAMENTE los parámetros a enviar.
     * Útil cuando solo se necesita actualizar un subconjunto de campos del SP;
     * el resto de los parámetros queda en NULL en el SP y no se pisan.
     *
     * <p>Implementación: construye un bloque T-SQL con variables locales de salida,
     * llama al SP pasándolas como OUTPUT y luego hace SELECT de esas variables.
     * Esto evita la restricción del driver SQL Server que rechaza {@code @param=? OUTPUT}
     * en prepared statements, y evita que {@code SimpleJdbcCall} valide parámetros
     * "requeridos" (sin DEFAULT) que intencionalmente no se envían.
     *
     * <pre>
     *   DECLARE @__error INT = 0, @__errormsg NVARCHAR(500) = N'', @__idGenerado BIGINT = 0;
     *   EXEC spName @p1=?, @p2=?, @ACCION=?,
     *               @error=@__error OUTPUT, @errormsg=@__errormsg OUTPUT, @idGenerado=@__idGenerado OUTPUT;
     *   SELECT @__error AS [error], @__errormsg AS errormsg, @__idGenerado AS idGenerado;
     * </pre>
     */
    public RespuestaSp ejecutarAbmMap(String spName, Map<String, Object> params, String accion) {
        // LinkedHashMap para garantizar orden estable: mismo orden en SQL y en lista de valores
        final Map<String, Object> paramsCopy = new LinkedHashMap<>(params);
        paramsCopy.put("ACCION", accion);

        // Construir bloque T-SQL completo
        final StringBuilder sql = new StringBuilder();
        sql.append("DECLARE @__error INT = 0, @__errormsg NVARCHAR(500) = N'', @__idGenerado BIGINT = 0; ");
        sql.append("EXEC ").append(spName);

        final List<Object> inputValues = new ArrayList<>(paramsCopy.size());
        boolean first = true;
        for (Map.Entry<String, Object> entry : paramsCopy.entrySet()) {
            sql.append(first ? " " : ", ").append("@").append(entry.getKey()).append("=?");
            inputValues.add(entry.getValue());
            first = false;
        }
        sql.append(", @error=@__error OUTPUT, @errormsg=@__errormsg OUTPUT, @idGenerado=@__idGenerado OUTPUT; ");
        sql.append("SELECT @__error AS [error], @__errormsg AS errormsg, @__idGenerado AS idGenerado;");

        logger.debug("ejecutarAbmMap → SP={}, params={}", spName, paramsCopy.keySet());

        try {
            return jdbcTemplate.execute(
                    (java.sql.Connection con) -> con.prepareStatement(sql.toString()),
                    (PreparedStatement ps) -> {
                        for (int i = 0; i < inputValues.size(); i++) {
                            ps.setObject(i + 1, inputValues.get(i));
                        }
                        // execute() soporta batches multi-sentencia correctamente
                        boolean hasResult = ps.execute();

                        // Avanzar hasta encontrar el ResultSet del SELECT final
                        // (DECLARE y EXEC con SET NOCOUNT ON no generan ResultSets)
                        while (!hasResult) {
                            if (ps.getUpdateCount() == -1) break;
                            hasResult = ps.getMoreResults();
                        }

                        if (!hasResult) {
                            throw new RuntimeException("El SP no retornó resultados de salida.");
                        }

                        try (ResultSet rs = ps.getResultSet()) {
                            if (!rs.next()) {
                                throw new RuntimeException("El SP retornó ResultSet vacío.");
                            }
                            int error       = rs.getInt("error");
                            String errormsg = rs.getString("errormsg");
                            long idGenerado = rs.getLong("idGenerado");

                            if (error != 0) {
                                logger.warn("El SP {} devolvió un error de negocio: {}", spName, errormsg);
                                throw new SpBusinessException(errormsg);
                            }
                            return new RespuestaSp(error, errormsg, idGenerado);
                        }
                    }
            );
        } catch (DataAccessException ex) {
            logger.error("Error de DB al ejecutar SP {} con Map. Acción: {}", spName, accion, ex);
            throw new RuntimeException("Error de conexión o sintaxis en la base de datos.", ex);
        } catch (SpBusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error inesperado procesando Map para SP {}", spName, ex);
            throw new RuntimeException("Error interno procesando parámetros.", ex);
        }
    }

    /**
     * Ejecuta cualquier SP de ABM de forma dinámica.
     */
    public RespuestaSp ejecutarAbm(String spName, Object model, String accion) {
        try {
            SimpleJdbcCall call = spCache.computeIfAbsent(spName, name ->
                    new SimpleJdbcCall(jdbcTemplate).withProcedureName(name)
            );

            Map<String, Object> atributosObjeto = objectMapper.convertValue(model, new TypeReference<Map<String, Object>>() {});
            atributosObjeto.put("ACCION", accion);

            MapSqlParameterSource inParams = new MapSqlParameterSource(atributosObjeto);
            Map<String, Object> out = call.execute(inParams);

            int error = out.get("error") != null ? ((Number) out.get("error")).intValue() : 99;
            String errormsg = (String) out.getOrDefault("errormsg", "Error desconocido");
            long idGenerado = out.get("idGenerado") != null ? ((Number) out.get("idGenerado")).longValue() : 0L;


            // Si el SP de SQL  devuelve un error, cortamos la ejecución y lanzamos la excepción.
            if (error != 0) {
                logger.warn("El SP {} devolvió un error de negocio: {}", spName, errormsg);
                throw new SpBusinessException(errormsg);
            }

            // Si llegamos aquí, es porque error == 0 (Éxito)
            return new RespuestaSp(error, errormsg, idGenerado);

        } catch (DataAccessException ex) {
            logger.error("Error de DB al ejecutar el SP {}. Acción: {}", spName, accion, ex);
            // Lanzamos una excepción para que el GlobalExceptionHandler devuelva un Error 500
            throw new RuntimeException("Error de conexión o sintaxis en la base de datos.", ex);
        } catch (SpBusinessException ex) {
            // Si es nuestra excepción de negocio, simplemente la dejamos pasar hacia arriba
            throw ex;
        } catch (Exception ex) {
            logger.error("Error inesperado procesando objeto para SP {}", spName, ex);
            throw new RuntimeException("Error interno procesando parámetros.", ex);
        }
    }

    /**
     * Ejecuta un SP de listado recibiendo un modelo Java como filtro.
     *
     * Jackson serializa TODOS los campos del modelo a un Map, incluyendo
     * primitivos en su valor default (int=0, double=0.0).  El SP los recibe
     * tal cual: si el SP trata 0 como "sin filtro" (Monedas, TiposCargo…)
     * funciona.  Si el SP usa NULL como "sin filtro" (SolicitudPago…) y recibe
     * 0, podría filtrar incorrectamente.
     *
     * Para esos casos, usar el overload con {@code Map<String, Object>} que
     * permite enviar SOLO los parámetros necesarios.
     *
     * Limpieza del Map:
     *   ✘ null       → para que el SP use su DEFAULT NULL (sin filtro)
     *   ✘ Collection → listas anidadas del modelo, no son parámetros del SP
     *   ✔ Number==0  → se CONSERVA; muchos SPs usan 0 como "devolver todo"
     */
    public <T> List<T> ejecutarListado(String spName, Object model, String accion, Class<T> clazz) {
        Map<String, Object> params = model != null
                ? objectMapper.convertValue(model, new TypeReference<Map<String, Object>>() {})
                : new HashMap<>();

        // Solo quitar null y colecciones; conservar 0
        params.entrySet().removeIf(entry -> {
            Object val = entry.getValue();
            return val == null || val instanceof Collection || val instanceof Map;
        });

        return ejecutarListado(spName, params, accion, clazz);
    }

    /**
     * Ejecuta un SP de listado recibiendo un Map con EXACTAMENTE los parámetros
     * que se quieren enviar al SP.  No se aplica ninguna limpieza.
     *
     * Usar este método cuando se necesita control preciso, por ejemplo para
     * cargar un registro por ID sin que los campos default (0) interfieran
     * con el filtrado del SP.
     *
     * Ejemplo:
     * <pre>
     *   Map&lt;String, Object&gt; filtro = new HashMap&lt;&gt;();
     *   filtro.put("idSolicitud", 33L);
     *   spHelper.ejecutarListado("p_list_tpex_SolicitudPago", filtro, "L", SolicitudPago.class);
     *   // → EXEC p_list_tpex_SolicitudPago @idSolicitud=?, @ACCION=?
     *   //   Solo envía idSolicitud=33 y ACCION='L'; todo lo demás queda en DEFAULT NULL
     * </pre>
     */
    public <T> List<T> ejecutarListado(String spName, Map<String, Object> params, String accion, Class<T> clazz) {
        try {
            Map<String, Object> paramsCopy = new HashMap<>(params);
            paramsCopy.put("ACCION", accion);

            // Construir: EXEC spName @param1=?, @param2=?, ...
            StringBuilder sql = new StringBuilder("EXEC ").append(spName);
            List<Object> values = new ArrayList<>(paramsCopy.size());
            boolean first = true;
            for (Map.Entry<String, Object> entry : paramsCopy.entrySet()) {
                sql.append(first ? " " : ", ")
                   .append("@").append(entry.getKey()).append("=?");
                values.add(entry.getValue());
                first = false;
            }

            return jdbcTemplate.query(
                    sql.toString(),
                    BeanPropertyRowMapper.newInstance(clazz),
                    values.toArray()
            );

        } catch (DataAccessException ex) {
            logger.error("Error de DB al ejecutar SP de listado {}. Acción: {}", spName, accion, ex);
            throw new RuntimeException("Error al consultar los datos en la base de datos.", ex);
        }
    }
}