package bo.bosque.com.impexpap.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mantiene al día la copia local de los datos de SAP que usan el listado de depósitos y la lista
 * de notas de remisión, para que ningún usuario pague esa consulta dentro de su request.
 *
 * <h3>Qué problema arregla (medido el 30/09/2026)</h3>
 * {@code p_list_tdep_DepositoCheques} ('C' y 'B') y {@code p_list_tdep_NotaRemision} ('A')
 * consultaban al otro servidor en <b>cada llamada</b> y traían el conjunto completo: 42.319 pagos
 * (~2,2 s) que además se separaban con XML (~4,4 s). El listado tardaba 8,7 s aunque se pidiera un
 * solo mes. El SP {@code p_sync_tdep_SapCache} hace esa consulta una vez y deja la copia en tablas
 * locales; los SPs de listado leen de ahí (decenas de milisegundos).
 *
 * <h3>Por qué en la aplicación y no en un job del Agente de SQL Server</h3>
 * Es el mismo patrón que la sincronización de entregas ({@link DatabaseTaskScheduler}): se
 * despliega con el backend, se apaga con una propiedad y sus errores salen en el log de la
 * aplicación, sin tocar la configuración del servidor de base de datos.
 *
 * <h3>Los números</h3>
 * <ul>
 *   <li><b>{@code fixedDelay} y no {@code fixedRate}:</b> cuenta desde que <i>terminó</i> la corrida
 *       anterior, así que un servidor remoto lento nunca acumula corridas atrasadas.</li>
 *   <li><b>120 s:</b> es lo que tarda en reflejarse un pago nuevo de SAP como «Verificado». Se
 *       cambia con {@code tdep.sap-cache.intervalo-ms}.</li>
 *   <li><b>Arranque a los 45 s</b> para no pelearle el inicio al resto del contexto.</li>
 * </ul>
 *
 * <h3>Si algo falla</h3>
 * Nunca relanza: el hilo del scheduler no se muere y la corrida siguiente lo intenta de nuevo. El
 * SP captura sus propios errores y los deja en {@code tdep_SapCacheEstado.error} (conservando la
 * copia anterior); aquí se lee ese campo y se lo pasa al log. Sin corrida exitosa en 10 minutos,
 * los SPs de listado refrescan en línea por su cuenta ({@code p_tdep_AsegurarCacheSap}), así que
 * este job es una optimización de latencia y no una dependencia dura.
 *
 * <p>Para apagarlo: {@code tdep.sap-cache.habilitado=false}. Requiere haber aplicado
 * el script {@code 2026-09-30_tdep_2_copia_local_sap.sql}; sin él, cada corrida deja un aviso (solo
 * cuando el mensaje cambia, para no llenar el log cada dos minutos).
 */
@Component
@ConditionalOnProperty(name = "tdep.sap-cache.habilitado", havingValue = "true", matchIfMissing = true)
public class SincronizacionSapCacheScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SincronizacionSapCacheScheduler.class);

    static final String SQL_SINCRONIZAR = "EXEC dbo.p_sync_tdep_SapCache";
    static final String SQL_ERROR_ULTIMO =
            "SELECT error FROM dbo.tdep_SapCacheEstado WHERE clave = 'sap' AND error IS NOT NULL";

    private final JdbcTemplate jdbcTemplate;

    /** Último aviso emitido, para no repetir el mismo cada corrida. */
    private String ultimoAviso;

    public SincronizacionSapCacheScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(
            fixedDelayString = "${tdep.sap-cache.intervalo-ms:120000}",
            initialDelayString = "${tdep.sap-cache.retraso-inicial-ms:45000}")
    public void refrescar() {
        long inicio = System.nanoTime();
        try {
            jdbcTemplate.execute(SQL_SINCRONIZAR);
            List<String> errores = jdbcTemplate.queryForList(SQL_ERROR_ULTIMO, String.class);
            if (errores.isEmpty()) {
                if (ultimoAviso != null) {
                    logger.info("Copia local de SAP (depósitos y notas) refrescada de nuevo con normalidad.");
                    ultimoAviso = null;
                }
                logger.debug("Copia local de SAP refrescada en {} ms", (System.nanoTime() - inicio) / 1_000_000L);
            } else {
                avisar("El refresco de la copia de SAP falló y se conserva la anterior: " + errores.get(0));
            }
        } catch (Exception e) {
            avisar("No se pudo refrescar la copia de SAP (¿está aplicado 2026-09-30_tdep_2_copia_local_sap.sql?): "
                    + e.getMessage());
        }
    }

    /** WARN la primera vez que aparece un mensaje; si se repite igual, DEBUG. */
    private void avisar(String mensaje) {
        if (mensaje.equals(ultimoAviso)) {
            logger.debug(mensaje);
        } else {
            logger.warn(mensaje);
            ultimoAviso = mensaje;
        }
    }
}
