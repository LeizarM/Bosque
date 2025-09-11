package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ControCombustibleMaquinaMontacarga;
import bo.bosque.com.impexpap.model.Movimiento;
import bo.bosque.com.impexpap.model.TipoContenedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

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
     * Obtendra el reporte de movimientos contenedores por fechas y sucursal y tipo de contenedores.
     * @param fechaInicio
     * @param fechaFin
     * @param codSucursal
     * @param idTipo
     * @return
     */
    @Override
    public List<Movimiento> listarMovimientosXFechas(java.util.Date fechaInicio, java.util.Date fechaFin, int codSucursal, int idTipo) {

        List<Movimiento> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_Movimiento @codSucursal  = ?, @fechaInicio = ?, @fechaFin  = ?, @idTipo = ?, @ACCION = ?",
                    new Object[] { codSucursal, fechaInicio, fechaFin, idTipo,  "A" },
                    new int[] { Types.INTEGER, Types.DATE, Types.DATE, Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Movimiento temp = new Movimiento();

                        temp.setFechaMovimientoString(rs.getString(1));
                        temp.setTipoMovimiento(rs.getString(2));
                        temp.setOrigen(rs.getString(3));
                        temp.setDestino(rs.getString(4));
                        temp.setNombreCompleto(rs.getString(5));
                        temp.setObs(rs.getString(6));
                        temp.setValorEntrada(rs.getFloat(7));
                        temp.setValorSalida(rs.getFloat(8));
                        temp.setValorSaldo(rs.getFloat(9));


                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar los tipos de contenedores"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;

    }

    /**
     * Para listar el saldo actual de combustible por sucursal y por tipo de contenedores.
     * @return
     */
    @Override
    public List<Movimiento> lstSaldosActuales() {

        List<Movimiento> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_Movimiento  @ACCION = ?",
                    new Object[] {  "B" },
                    new int[] {  Types.VARCHAR },
                    (rs, rowNum) -> {
                        Movimiento temp = new Movimiento();

                        temp.setNombreSucursal(rs.getString(1));
                        temp.setCodSucursal(rs.getInt(2));
                        temp.setTipo(rs.getString(3));
                        temp.setValorSaldo(rs.getFloat(4));
                        temp.setFechaMovimientoString(rs.getString(5));
                        temp.setValorEntrada(rs.getFloat(6));



                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar los saldos de combustible por sucursal"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;

    }

    /**
     * Listara los bidones pendientes antes de registrar una compra de combustible
     *
     * @param sucursalDestino
     * @return
     */
    @Override
    public List<Movimiento> obtenerBidonesXSucursal( int sucursalDestino ) {

        List<Movimiento> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_Movimiento @sucursalDestino= ?, @ACCION = ?",
                    new Object[] { sucursalDestino ,"C" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Movimiento temp = new Movimiento();

                        temp.setIdMovimiento(rs.getLong(1));
                        temp.setNombreSucursal(rs.getString(2));
                        temp.setNombreCoche(rs.getString(3));
                        temp.setCodigoDestino(rs.getString(4)  );
                        temp.setFechaMovimiento(rs.getDate(5));
                        temp.setValorEntrada(rs.getFloat(6));


                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en obtenerBidonesXSucursal"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;


    }

    /**
     * Listara el detalle de un bidón según el id de movimiento
     *
     * @param idMovimiento
     * @return
     */
    @Override
    public List<Movimiento> obtenerDetalleBidon( long idMovimiento ) {

        List<Movimiento> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_Movimiento @idMovimiento= ?, @ACCION = ?",
                    new Object[] { idMovimiento ,"D" },
                    new int[] { Types.LONGVARCHAR, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Movimiento temp = new Movimiento();

                        temp.setIdMovimiento(rs.getLong(1));
                        temp.setNombreSucursal(rs.getString(2));
                        temp.setNombreCoche(rs.getString(3));
                        temp.setCodigoDestino(rs.getString(4)  );
                        temp.setFechaMovimiento(rs.getDate(5));
                        temp.setValorEntrada(rs.getFloat(6));


                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en obtenerDetalleBidon"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;


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
