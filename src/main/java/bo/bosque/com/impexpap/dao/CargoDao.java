package bo.bosque.com.impexpap.dao;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import bo.bosque.com.impexpap.model.Cargo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CargoDao implements ICargo {

    /**
     * El Datasource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(CargoDao.class);

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_Cargo " +
                    "@codCargo = ?, " +
                    "@codCargoPadre = ?, " +
                    "@descripcion = ?, " +
                    "@codEmpresa = ?, " +
                    "@codNivel = ?, " +
                    "@posicion = ?, " +
                    "@estado = ?, " +
                    "@audUsuarioI = ?, " +
                    "@ACCION = ?";



    /**
     * Procedimiento que obtendra los cargos por empresa
     * @param codEmpresa
     * @return
     */
    public List<Cargo> obtenerCargoXEmpresa( int codEmpresa ) {

        List<Cargo> lstTemp  = new ArrayList<>();

        try{

            lstTemp = this.jdbcTemplate.query("execute p_list_cargo @codEmpresa=?, @ACCION = ?",
                    new Object[]{ codEmpresa, "L" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) -> {
                        Cargo temp  = new Cargo();
                        temp.setCodEmpresa( rs.getInt(1) );
                        temp.setCodCargoPadre(rs.getInt(2));
                        temp.setDescripcion(rs.getString(3));
                        temp.setCodEmpresa(rs.getInt(4));
                        temp.setNombreEmpresa(rs.getString(5));
                        temp.setCodNivel(rs.getInt(6));
                        temp.setPosicion(rs.getInt(7));
                        temp.setAudUsuario(rs.getInt(8));
                        return temp;
                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: CargoDao en obtenerCargoXEmpresa, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Obtendra los cargos por empresa pero de forma mas informativa
     *
     * @param codEmpresa
     * @return
     */
    @Override
    public List<Cargo> obtenerCargoXEmpresaNew( int codEmpresa ) {

        List<Cargo> lstTemp  = new ArrayList<>();

        try{

            lstTemp = this.jdbcTemplate.query("execute p_list_cargo @codEmpresa=?, @ACCION = ?",
                    new Object[]{ codEmpresa, "B" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) -> {
                        Cargo temp  = new Cargo();

                        temp.setCodCargo( rs.getInt(1) );
                        temp.setCodCargoPadre(rs.getInt(2));
                        temp.setCodCargoPadreOriginal( rs.getInt(3));
                        temp.setDescripcion(rs.getString(4));
                        temp.setCodEmpresa(rs.getInt(5));
                        temp.setCodNivel(rs.getInt(6));
                        temp.setNivel(rs.getInt(7));
                        temp.setEstado(rs.getInt(8));
                        temp.setTieneEmpleadosActivos(rs.getInt(9));
                        temp.setTieneEmpleadosTotales(rs.getInt(10));
                        temp.setEstaAsignadoSucursal( rs.getInt(11));
                        temp.setCanDeactivate( rs.getInt(12) );
                        temp.setNumDependientes( rs.getInt(13) );
                        temp.setNumDependenciasTotales( rs.getInt(14) );
                        temp.setNumDependenciasCompletas( rs.getInt(15) );
                        temp.setNumDeDependencias( rs.getInt(16) );
                        temp.setNumHijosActivos( rs.getInt(17) );
                        temp.setNumHijosTotal( rs.getInt(18) );
                        temp.setResumenCompleto( rs.getString(19) );
                        temp.setEstadoPadre( rs.getString(20) );
                        temp.setPosicion( rs.getInt(21) );
                        temp.setEsVisible( rs.getInt(22) );

                        return temp;
                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: CargoDao en obtenerCargoXEmpresaNew, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;


    }

    /**
     * Registar / Actualizar un cargo
     *
     * @param mb
     * @return
     */
    @Override
    public boolean registrarCargo( Cargo mb, String acc ) {
        try {
            int affectedRows = jdbcTemplate.update(SQL_STORED_PROCEDURE, ps -> {
                ps.setEscapeProcessing(true);
                ps.setInt(1, mb.getCodCargo());
                ps.setInt(2, mb.getCodCargoPadre());
                ps.setString(3, mb.getDescripcion());
                ps.setInt(4, mb.getCodEmpresa());
                ps.setInt(5, mb.getCodNivel());
                ps.setInt(6, mb.getPosicion());
                ps.setInt(7, mb.getEstado());
                ps.setInt(8, mb.getAudUsuario());
                ps.setString(9, acc);
            });

            return affectedRows > 0;

        } catch (DataAccessException ex) {
            logDataAccessException(ex, "Error al registrar el cargo para la empresa seleccionada");
            return false;
        }


    }

    /**
     * Obtendra los empleados por cargo
     *
     * @param codCargo
     * @return
     */
    @Override
    public List<Cargo> obtenerEmpleadosXCargo(int codCargo) {
        List<Cargo> lstTemp  = new ArrayList<>();

        try{

            lstTemp = this.jdbcTemplate.query("execute p_list_Cargo @codCargo=?, @ACCION = ?",
                    new Object[]{ codCargo, "C" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) -> {
                        Cargo temp  = new Cargo();
                        temp.setCodEmpleado( rs.getInt(1) );
                        temp.setNombreCompleto(rs.getString(2));
                        temp.setDescripcion(rs.getString(3));
                        temp.setNombreEmpresa(rs.getString(4));
                        temp.setSucursal(rs.getString(5));
                        temp.setEstado(rs.getInt(6));
                        return temp;
                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: CargoDao en obtenerEmpleadosXCargo, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
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
