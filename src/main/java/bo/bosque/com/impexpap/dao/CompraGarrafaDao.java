package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CompraGarrafa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class CompraGarrafaDao implements ICompraGarrafa{

    private static final Logger logger = LoggerFactory.getLogger(CompraGarrafaDao.class);

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_tgas_CompraGarrafa " +
                    "@idCG = ?, " +
                    "@codSucursal = ?, " +
                    "@descripcion = ?, " +
                    "@cantidad = ?, " +
                    "@monto = ?, " +
                    "@audUsuario = ?, " +
                    "@ACCION = ?";

    private final JdbcTemplate jdbcTemplate;

    public CompraGarrafaDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public boolean registrarCompra( CompraGarrafa mb, String acc ) {

        try {
            int affectedRows = jdbcTemplate.update(SQL_STORED_PROCEDURE, ps -> {
                ps.setEscapeProcessing(true);
                ps.setInt(1, mb.getIdCG());
                ps.setInt(2, mb.getCodSucursal());
                ps.setString(3, mb.getDescripcion());
                ps.setInt(4, mb.getCantidad());
                ps.setFloat(5, mb.getMonto());
                ps.setInt(6, mb.getAudUsuario());
                ps.setString(7, acc);
            });

            return affectedRows > 0;

        } catch (DataAccessException ex) {
            logDataAccessException(ex, "Error al registrar depósito de cheque");
            return false;
        }


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
