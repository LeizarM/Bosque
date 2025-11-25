package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CargoSucursal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CargoSucursalDao implements ICargoSucursal {

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_Cargo_sucursal " +
                    "@codCargoSucursal = ?, " +
                    "@codSucursal = ?, " +
                    "@codCargo = ?, " +
                    "@audUsuarioI = ?, " +
                    "@ACCION = ?";

    private static final Logger logger = LoggerFactory.getLogger(CargoSucursalDao.class);

    /**
     * El DataSource de la conexion
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento para registrar un nuevo cargo por sucursal
     *
     * @param mb
     * @return
     */
    @Override
    public boolean registrarCargoSucursal(CargoSucursal mb, String acc) {
        try {
            int affectedRows = jdbcTemplate.update(SQL_STORED_PROCEDURE, ps -> {
                ps.setEscapeProcessing(true);
                ps.setInt(1, mb.getCodCargoSucursal());
                ps.setInt(2, mb.getCodSucursal());
                ps.setInt(3, mb.getCodCargo());
                ps.setInt(4, mb.getAudUsuario());
                ps.setString(5, acc);
            });

            return affectedRows > 0;

        } catch (DataAccessException ex) {
            logDataAccessException(ex, "Error al registrar el cargo para una sucursal seleccionada");
            return false;
        }

    }

    /**
     * Procedimiento que obtendra los cargos por sucursal
     * @param codSucursal
     * @return
     */
    public List<CargoSucursal> obtenerSucursalesXEmpresa( int codSucursal ) {

        List<CargoSucursal> lstTemp = new ArrayList<>();

        try{

            lstTemp = this.jdbcTemplate.query("execute p_list_Cargo_sucursal @codSucursal=?, @ACCION=?",
                    new Object[]{  codSucursal, "A" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{
                        CargoSucursal temp = new CargoSucursal();

                        temp.setCodCargoSucursal(rs.getInt(1));
                        temp.setCodSucursal(rs.getInt(2));
                        temp.setCodCargo(rs.getInt(3));
                        temp.getSucursal().getEmpresa().setCodEmpresa(rs.getInt(4));
                        temp.getSucursal().setNombre(rs.getString(5));
                        temp.setDatoCargo(rs.getString(6));
                        temp.getSucursal().getEmpresa().setNombre(rs.getString(7));
                        return temp;
                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: CargoSucursalDao en obtenerSucursalesXEmpresa, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return lstTemp;
    }

    /**
     * Obtendra un cargo por sucursal
     *
     * @return
     */
    @Override
    public List<CargoSucursal> obtenerCargoEnSucursales( int codCargo ) {
        List<CargoSucursal> lstTemp = new ArrayList<>();

        try{

            lstTemp = this.jdbcTemplate.query("execute p_list_Cargo_sucursal @codCargo=?, @ACCION=?",
                    new Object[]{  codCargo, "A" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{
                        CargoSucursal temp = new CargoSucursal();

                        temp.setCodCargoSucursal(rs.getInt(1));
                        temp.setCodSucursal(rs.getInt(2));
                        temp.setCodCargo(rs.getInt(3));
                        temp.getSucursal().getEmpresa().setCodEmpresa(rs.getInt(4));
                        temp.getSucursal().setNombre(rs.getString(5));
                        temp.setDatoCargo(rs.getString(6));
                        temp.getSucursal().getEmpresa().setNombre(rs.getString(7));
                        return temp;
                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: CargoSucursalDao en obtenerCargoEnSucursales, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
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
