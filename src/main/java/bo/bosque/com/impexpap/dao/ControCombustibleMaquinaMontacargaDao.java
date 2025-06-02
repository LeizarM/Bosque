package bo.bosque.com.impexpap.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import bo.bosque.com.impexpap.model.ControCombustibleMaquinaMontacarga;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class ControCombustibleMaquinaMontacargaDao implements IControCombustibleMaquinaMontacarga {


    private static final Logger logger = LoggerFactory.getLogger(ControCombustibleMaquinaMontacarga.class);

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_tgas_ControlCombustibleMaquinaMontacarga " +
                    "@idCM = ?, " +
                    "@idMaquinaVehiculoOrigen = ?, " +
                    "@idMaquinaVehiculoDestino = ?, " +
                    "@codSucursalMaqVehiOrigen = ?, " +
                    "@codSucursalMaqVehiDestino = ?, " +
                    "@codigoOrigen = ?, " +
                    "@codigoDestino = ?, " +
                    "@fecha = ?, " +
                    "@litrosIngreso = ?, " +
                    "@litrosSalida = ?, " +
                    "@saldoLitros = ?, " +
                    "@codEmpleado = ?, " +
                    "@codAlmacen = ?,"+
                    "@obs = ?,"+
                    "@tipoTransaccion = ?,"+
                    "@audUsuario = ?, " +
                    "@ACCION = ?";

    private final JdbcTemplate jdbcTemplate;

    public ControCombustibleMaquinaMontacargaDao( JdbcTemplate jdbcTemplate ) {

        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public boolean registrarControlCombustible(ControCombustibleMaquinaMontacarga mb, String acc) {
        try {
            int affectedRows = jdbcTemplate.update(SQL_STORED_PROCEDURE, ps -> {
                ps.setEscapeProcessing(true);
                ps.setLong(1, mb.getIdCM());
                ps.setInt(2, mb.getIdMaquinaVehiculoOrigen());
                ps.setInt(3, mb.getIdMaquinaVehiculoDestino());
                ps.setInt(4, mb.getCodSucursalMaqVehiOrigen());
                ps.setInt(5, mb.getCodSucursalMaqVehiDestino());
                ps.setString(6, mb.getCodigoOrigen());
                ps.setString(7, mb.getCodigoDestino());
                ps.setDate(8, (Date) mb.getFecha());
                ps.setFloat(9, mb.getLitrosIngreso());
                ps.setFloat(10, mb.getLitrosSalida());
                ps.setFloat(11, mb.getSaldoLitros());
                ps.setInt(12, mb.getCodEmpleado());
                ps.setString(13, mb.getCodAlmacen());
                ps.setString(14, mb.getObs());
                ps.setString(15, mb.getTipoTransaccion());
                ps.setInt(16, mb.getAudUsuario());
                ps.setString(17, acc);
            });

            return affectedRows > 0;

        } catch (DataAccessException ex) {
            logDataAccessException(ex, "Error al registrar el control de combustible de maquina de montacarga");
            return false;
        }
    }

    /**
     * Para listar todos los almacenes
     * @return
     */
    @Override
    public List<ControCombustibleMaquinaMontacarga> lstAlmacenes() {
        List<ControCombustibleMaquinaMontacarga> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_ControlCombustibleMaquinaMontacarga  @ACCION=?",
                    new Object[] { "A" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        ControCombustibleMaquinaMontacarga temp = new ControCombustibleMaquinaMontacarga();

                        temp.setWhsCode(rs.getString(1));
                        temp.setWhsName(rs.getString(2));

                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en lstAlmacenes"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;
    }


    @Override
    public List<ControCombustibleMaquinaMontacarga> lstRptMovBidonesXTipoTransaccion(java.util.Date fechaInicio, java.util.Date fechaFin) {
        List<ControCombustibleMaquinaMontacarga> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_ControlCombustibleMaquinaMontacarga @fechaInicio=?, @fechaFin=?, @ACCION=?",
                    new Object[] { fechaInicio, fechaFin, "B" },
                    new int[] {  Types.DATE, Types.DATE,  Types.VARCHAR },
                    (rs, rowNum) -> {
                        ControCombustibleMaquinaMontacarga temp = new ControCombustibleMaquinaMontacarga();

                        temp.setFecha(rs.getDate(1));
                        temp.setNombreCompleto(rs.getString(2));
                        temp.setCodigoOrigen(rs.getString(3));
                        temp.setNombreMaquinaOrigen(rs.getString(4));
                        temp.setCodigoDestino(rs.getString(5));
                        temp.setNombreMaquinaDestino(rs.getString(6));
                        temp.setNombreSucursal(rs.getString(7));
                        temp.setLitrosIngreso(rs.getFloat(8));
                        temp.setObs(rs.getString(9));
                        temp.setTipoTransaccion(rs.getString(10));

                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en lstRptMovBidonesXTipoTransaccion"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;
    }

    @Override
    public List<ControCombustibleMaquinaMontacarga> saldoActualCombustinbleXSucursal() {
        List<ControCombustibleMaquinaMontacarga> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_ControlCombustibleMaquinaMontacarga  @ACCION=?",
                    new Object[] {  "C" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        ControCombustibleMaquinaMontacarga temp = new ControCombustibleMaquinaMontacarga();

                        temp.setCodSucursalMaqVehiOrigen(rs.getInt(1));
                        temp.setNombreSucursal(rs.getString(2));
                        temp.setSaldoLitros(rs.getFloat(3));
                        temp.setFecha(rs.getDate(4)  );
                        temp.setLitrosIngreso(rs.getFloat(5));


                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en saldoActualCombustinbleXSucursal"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;
    }

    @Override
    public List<ControCombustibleMaquinaMontacarga> lstUltimosMovBidones() {

        List<ControCombustibleMaquinaMontacarga> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_ControlCombustibleMaquinaMontacarga  @ACCION=?",
                    new Object[] {  "D" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        ControCombustibleMaquinaMontacarga temp = new ControCombustibleMaquinaMontacarga();

                        temp.setCodigoOrigen(rs.getString(1));
                        temp.setCodigoDestino(rs.getString(2));
                        temp.setFecha(rs.getDate(3));
                        temp.setLitrosIngreso(rs.getFloat(4)  );
                        temp.setTipoTransaccion(rs.getString(5));


                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en lstUltimosMovBidones"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;


    }




    /**
     * Método auxiliar para registrar errores de acceso a datos
     * @param ex Excepción ocurrida
     * @param mensaje Mensaje descriptivo del error
     */
    private void logDataAccessException (DataAccessException ex, String mensaje){
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
