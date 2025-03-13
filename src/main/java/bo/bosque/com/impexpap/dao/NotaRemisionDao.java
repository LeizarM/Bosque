package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.NotaRemision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

@Repository
public class NotaRemisionDao implements  INotaRemision{


    private static final Logger logger = LoggerFactory.getLogger(DepositoChequeDao.class);

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_tdep_DepositoCheques " +
                    "@idNR = ?, " +
                    "@idDeposito = ?, " +
                    "@docNum = ?, " +
                    "@fecha = ?, " +
                    "@numFact = ?, " +
                    "@totalMonto = ?, " +
                    "@saldoPendiente = ?, " +
                    "@audUsuario = ?, " +
                    "@ACCION = ?";
    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar una nueva Nota de Remision
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarNotaRemision( NotaRemision mb, String acc ) {

        try {
            int affectedRows = jdbcTemplate.update(SQL_STORED_PROCEDURE, ps -> {
                ps.setEscapeProcessing(true);
                ps.setInt(1, mb.getIdNR());
                ps.setInt(2, mb.getIdDeposito());
                ps.setInt(3, mb.getDocNum() );
                ps.setDate(4, (Date) mb.getFecha());
                ps.setInt(5, mb.getNumFact());
                ps.setFloat(6, mb.getTotalMonto());
                ps.setFloat(7, mb.getSaldoPendiente());
                ps.setInt(8, mb.getAudUsuario());
                ps.setString(9, acc);
            });

            return affectedRows > 0;

        } catch (DataAccessException ex) {
            logDataAccessException(ex, "Error al registrar depósito de cheque");
            return false;
        }


    }

    @Override
    public List<NotaRemision> listarNotasRemisiones( NotaRemision mb, String ac ) {
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
