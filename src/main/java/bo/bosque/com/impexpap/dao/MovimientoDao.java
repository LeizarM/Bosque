package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Movimiento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;

@Repository
public class MovimientoDao implements IMovimiento{

    private static final Logger logger = LoggerFactory.getLogger(MovimientoDao.class);

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_tgas_Movimiento " +
                    "@idMovimiento = ?, " +
                    "@tipoMovimiento = ?, " +
                    "@idOrigen = ?, " +
                    "@codigoOrigen = ?, " +
                    "@sucursalOrigen = ?, " +
                    "@idDestino = ?, " +
                    "@codigoDestino = ?, " +
                    "@sucursalDestino = ?, " +
                    "@codSucursal = ?, " +
                    "@fechaMovimiento = ?, " +
                    "@valor = ?, " +
                    "@valorEntrada = ?, " +
                    "@valorSalida = ?, " +
                    "@valorSaldo = ?, " +
                    "@unidadMedida = ?, " +
                    "@estado = ?, " +
                    "@obs = ?, "+
                    "@codEmpleado = ?, " +
                    "@idCompraGarrafa = ?,"+
                    "@audUsuario = ?, " +
                    "@ACCION = ?";

    private final JdbcTemplate jdbcTemplate;

    public MovimientoDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public boolean registrarMovimiento( Movimiento mb, String acc ) {

        try {
            int affectedRows = jdbcTemplate.update(SQL_STORED_PROCEDURE, ps -> {
                ps.setEscapeProcessing(true);
                ps.setLong(1, mb.getIdMovimiento());
                ps.setString(2, mb.getTipoMovimiento());
                ps.setLong(3, mb.getIdOrigen());
                ps.setString(4, mb.getCodigoOrigen());
                ps.setLong(5, mb.getSucursalOrigen());
                ps.setLong(6, mb.getIdDestino());
                ps.setString(7, mb.getCodigoDestino());
                ps.setLong(8, mb.getSucursalDestino());
                ps.setInt(9, mb.getCodSucursal());
                ps.setDate(10, (Date) mb.getFechaMovimiento());
                ps.setDouble(11, mb.getValor());
                ps.setDouble(12, mb.getValorEntrada());
                ps.setDouble(13, mb.getValorSalida());
                ps.setDouble(14, mb.getValorSaldo());
                ps.setString(15, mb.getUnidadMedida());
                ps.setInt(16, mb.getEstado());
                ps.setString(17, mb.getObs());
                ps.setLong(18, mb.getCodEmpleado());
                ps.setLong(19, mb.getIdCompraGarrafa());
                ps.setInt(20, mb.getAudUsuario());
                ps.setString(21, acc);
            });

            return affectedRows > 0;

        } catch (DataAccessException ex) {
            logDataAccessException(ex, "Error al registrar el movimiento contenedores");
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
