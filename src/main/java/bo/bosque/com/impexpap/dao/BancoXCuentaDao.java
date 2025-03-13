package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.BancoXCuenta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;

@Repository
public class BancoXCuentaDao implements  IBancoXCuenta{

    private static final Logger logger = LoggerFactory.getLogger(DepositoChequeDao.class);

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Override
    public List<BancoXCuenta> listarBancosXCuentas() {
        return null;
    }


    /**
     * Método auxiliar para registrar errores de acceso a datos
     * @param ex Excepción ocurrida
     * @param mensaje Mensaje descriptivo del error
     */
    private void logDataAccessException(DataAccessException ex, String mensaje) {
        logger.error(mensaje);
        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof SQLException) {
            SQLException sqlEx = (SQLException) rootCause;
            logger.error("Código de error SQL: {}, Estado SQL: {}",
                    sqlEx.getErrorCode(),
                    sqlEx.getSQLState());
        }
        logger.error("Detalle del error:", ex);
    }
}
