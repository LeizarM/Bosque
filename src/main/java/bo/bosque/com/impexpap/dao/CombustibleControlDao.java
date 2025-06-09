package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CombustibleControl;
import bo.bosque.com.impexpap.model.DepositoCheque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CombustibleControlDao implements ICombustibleControl {

    private static final Logger logger = LoggerFactory.getLogger(CombustibleControl.class);

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_tgas_CombustibleControl " +
                    "@idC = ?, " +
                    "@idCoche = ?, " +
                    "@fecha = ?, " +
                    "@estacionServicio = ?, " +
                    "@nroFactura = ?, " +
                    "@importe = ?, " +
                    "@kilometraje = ?, " +
                    "@codEmpleado = ?, " +
                    "@diferencia = ?, " +
                    "@codSucursalCoche = ?,"+
                    "@obs = ?,"+
                    "@litros = ?,"+
                    "@tipoCombustible = ?,"+
                    "@idCM = ?,"+
                    "@audUsuario = ?, " +
                    "@ACCION = ?";

    private final JdbcTemplate jdbcTemplate;

    public CombustibleControlDao( JdbcTemplate jdbcTemplate ) {

        this.jdbcTemplate = jdbcTemplate;
    }


    /**
     * Método para registrar un control de combustible
     * @param mb
     * @param acc
     * @return
     */
    @Transactional
    public boolean registrarCombustibleControl( CombustibleControl mb, String acc ) {
        try {
            int affectedRows = jdbcTemplate.update(SQL_STORED_PROCEDURE, ps -> {
                ps.setEscapeProcessing(true);
                ps.setLong(1, mb.getIdC());
                ps.setInt(2, mb.getIdCoche());
                ps.setDate(3, (Date) mb.getFecha());
                ps.setString(4, mb.getEstacionServicio());
                ps.setString(5, mb.getNroFactura());
                ps.setFloat(6, mb.getImporte());
                ps.setFloat(7, mb.getKilometraje());
                ps.setInt(8, mb.getCodEmpleado());
                ps.setFloat(9, mb.getDiferencia());
                ps.setInt(10, mb.getCodSucursalCoche());
                ps.setString(11, mb.getObs());
                ps.setFloat(12, mb.getLitros());
                ps.setString(13, mb.getTipoCombustible() );
                ps.setInt(14, mb.getIdCM());
                ps.setInt(15, mb.getAudUsuario());
                ps.setString(16, acc);
            });

            return affectedRows > 0;

        } catch (DataAccessException ex) {
            logDataAccessException(ex, "Error al registrar Control de Combustible");
            return false;
        }

    }

    /**
     * Método para listar todos los coches
     * @return
     */
    @Transactional(readOnly = true)
    public List<CombustibleControl> listarCoches() {
        List<CombustibleControl> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_CombustibleControl  @ACCION=?",
                    new Object[] { "A" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        CombustibleControl temp = new CombustibleControl();

                        temp.setIdCoche(rs.getInt(1));
                        temp.setCoche(rs.getString(2));
                        temp.setCodSucursalCoche(rs.getInt(3));

                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en listarCoches"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;
    }

    @Override
    public List<CombustibleControl> listarCochesKilometraje(int idCoche) {

        List<CombustibleControl> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_CombustibleControl @idCoche=?, @ACCION=?",
                    new Object[] { idCoche, "B" },
                    new int[] { Types.INTEGER,Types.VARCHAR },
                    (rs, rowNum) -> {
                        CombustibleControl temp = new CombustibleControl();

                        temp.setIdC(rs.getInt(1));
                        temp.setIdCoche(rs.getInt(2));
                        temp.setCoche(rs.getString(3));
                        temp.setFecha(rs.getDate(4));
                        temp.setLitros(rs.getFloat(5));
                        temp.setImporte(rs.getFloat(6));
                        temp.setKilometraje(rs.getFloat(7));
                        temp.setKilometrajeAnterior(rs.getFloat(8));
                        temp.setDiferencia(rs.getFloat(9));
                        temp.setTipoCombustible(rs.getString(10));
                        temp.setObs(rs.getString(11));
                        temp.setIdCM((int) rs.getLong(12));


                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en listarCoches"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;


    }

    @Override
    public List<CombustibleControl> esConsumoBajo( float kilometraje, int idCoche ) {

        List<CombustibleControl> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_CombustibleControl @kilometraje=?, @idCoche=?, @ACCION = ?",
                    new Object[] { kilometraje, idCoche, "C" },
                    new int[] {Types.FLOAT, Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        CombustibleControl temp = new CombustibleControl();

                        temp.setEsMenor(rs.getInt(1));
                        temp.setDiferencia(rs.getFloat(2));

                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en esConsumoBajo"); // Usamos el método auxiliar
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
