package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Sucursal;
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
public class SucursalDao implements  ISucursal {

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_Sucursal " +
                    "@codSucursal = ?, " +
                    "@nombre = ?, " +
                    "@codEmpresa = ?, " +
                    "@codCiudad = ?, " +
                    "@audUsuarioI = ?, " +
                    "@ACCION = ?";

    private static final Logger logger = LoggerFactory.getLogger(SucursalDao.class);


    /**
     * El Datasource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Registrar una sucursal por empresa
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarSucursal(Sucursal mb, String acc) {
        try {
            int affectedRows = jdbcTemplate.update(SQL_STORED_PROCEDURE, ps -> {
                ps.setEscapeProcessing(true);
                ps.setInt(1, mb.getCodSucursal());
                ps.setString(2, mb.getNombre());
                ps.setInt(3, mb.getCodEmpresa());
                ps.setInt(4, mb.getCodCiudad());
                ps.setInt(5, mb.getAudUsuarioI());
                ps.setString(6, acc);
            });

            return affectedRows > 0;

        } catch (DataAccessException ex) {
            logDataAccessException(ex, "Error al registrar la sucursal para la empresa seleccionada");
            return false;
        }
    }

    /**
     * Obtendra las sucursales por Empresa
     * @param codEmpresa
     * @return
     */
    public List<Sucursal> obtenerSucursalesXEmpresa(int codEmpresa) {
        List<Sucursal> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_sucursal @codEmpresa=?, @ACCION=?",
                    new Object[]{ codEmpresa, "L" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{
                        Sucursal temp = new Sucursal();

                        temp.setCodSucursal(rs.getInt(1));
                        temp.setNombre(rs.getString(2));
                        temp.setCodEmpresa(rs.getInt(3));
                        temp.getEmpresa().setNombre(rs.getString(4));
                        temp.setCodCiudad(rs.getInt(5));
                        temp.setNombreCiudad(rs.getString(6));
                        temp.setAudUsuarioI(rs.getInt(7));

                        return temp;

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: SucursalDao en obtenerSucursalesXEmpresa, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
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
