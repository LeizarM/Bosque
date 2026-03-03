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

import java.util.HashMap;
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
     * Ejecuta cualquier SP de Listado y mapea el resultado a una clase específica.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> ejecutarListado(String spName, Object model, String accion, Class<T> clazz) {
        try {
            String cacheKey = spName + "_list_" + clazz.getName();

            SimpleJdbcCall call = spCache.computeIfAbsent(cacheKey, key ->
                    new SimpleJdbcCall(jdbcTemplate)
                            .withProcedureName(spName)
                            .returningResultSet("resultado", BeanPropertyRowMapper.newInstance(clazz))
            );

            Map<String, Object> atributosObjeto = model != null
                    ? objectMapper.convertValue(model, new TypeReference<Map<String, Object>>() {})
                    : new HashMap<>();

            atributosObjeto.put("ACCION", accion);

            MapSqlParameterSource inParams = new MapSqlParameterSource(atributosObjeto);
            Map<String, Object> out = call.execute(inParams);

            return (List<T>) out.get("resultado");

        } catch (DataAccessException ex) {
            logger.error("Error de DB al ejecutar SP de listado {}. Acción: {}", spName, accion, ex);
            // Ya NO devolvemos new ArrayList<>(). Si la BD falla, debemos avisarle al cliente con un Error 500.
            throw new RuntimeException("Error al consultar los datos en la base de datos.", ex);
        }
    }
}