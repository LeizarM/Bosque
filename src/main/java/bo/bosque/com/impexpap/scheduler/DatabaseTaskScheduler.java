package bo.bosque.com.impexpap.scheduler;
import bo.bosque.com.impexpap.dao.IEntregaChofer;
import bo.bosque.com.impexpap.model.EntregaChofer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DatabaseTaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTaskScheduler.class);
    private final IEntregaChofer entregaChoferDao;

    public DatabaseTaskScheduler(IEntregaChofer entregaChoferDao) {
        this.entregaChoferDao = entregaChoferDao;
    }

    /**
     * Este método se ejecuta cada 24 horas (00:00:0)
     */
    @Scheduled(cron = "59 58 23 * * *") // Se ejecuta todos los días a las 23:58:59
    public void executeDailyStoredProcedure() {
        try {
            logger.info("Iniciando ejecución del procedimiento almacenado ::: p_abm_trch_Entregas ");
            this.entregaChoferDao.registrarEntregaChofer(new EntregaChofer(), "C");
            logger.info("Procedimiento almacenado ejecutado exitosamente ::: p_abm_trch_Entregas");

        } catch (Exception e) {
            // Manejo de errores
            logger.error("Error al ejecutar el procedimiento p_abm_trch_Entregas: {}", e.getMessage(), e);
        }
    }
}