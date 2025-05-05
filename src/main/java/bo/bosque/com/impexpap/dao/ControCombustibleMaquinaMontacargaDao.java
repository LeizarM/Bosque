package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CombustibleControl;
import bo.bosque.com.impexpap.model.ControCombustibleMaquinaMontacarga;
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
public class ControCombustibleMaquinaMontacargaDao implements IControCombustibleMaquinaMontacarga {


    private static final Logger logger = LoggerFactory.getLogger(ControCombustibleMaquinaMontacarga.class);

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_tgas_ControlCombustibleMaquinaMontacarga " +
                    "@idCM = ?, " +
                    "@idMaquina = ?, " +
                    "@fecha = ?, " +
                    "@litrosIngreso = ?, " +
                    "@litrosSalida = ?, " +
                    "@saldoLitros = ?, " +
                    "@horasUso = ?, " +
                    "@horometro = ?, " +
                    "@codEmpleado = ?, " +
                    "@codAlmacen = ?,"+
                    "@obs = ?,"+
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
                ps.setInt(2, mb.getIdMaquina());
                ps.setDate(3, (Date) mb.getFecha());
                ps.setFloat(4, mb.getLitrosIngreso());
                ps.setFloat(5, mb.getLitrosSalida());
                ps.setFloat(6, mb.getSaldoLitros());
                ps.setFloat(7, mb.getHorasUso());
                ps.setFloat(8, mb.getHorometro());
                ps.setInt(9, mb.getCodEmpleado());
                ps.setString(10, mb.getCodAlmacen());
                ps.setString(11, mb.getObs());
                ps.setInt(12, mb.getAudUsuario());
                ps.setString(13, acc);
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
    public List<ControCombustibleMaquinaMontacarga> lstBidonesXMaquina( int idMaquina ) {
        List<ControCombustibleMaquinaMontacarga> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_ControlCombustibleMaquinaMontacarga @idMaquina=?, @ACCION=?",
                    new Object[] { idMaquina, "B" },
                    new int[] { Types.INTEGER,Types.VARCHAR },
                    (rs, rowNum) -> {
                        ControCombustibleMaquinaMontacarga temp = new ControCombustibleMaquinaMontacarga();

                        temp.setIdCM(rs.getLong(1));
                        temp.setMaquina(rs.getString(2));
                        temp.setFecha(rs.getDate(3));
                        temp.setLitrosIngreso(rs.getFloat(4));
                        temp.setLitrosSalida(rs.getFloat(5));
                        temp.setSaldoLitros(rs.getFloat(6));
                        temp.setHorasUso(rs.getFloat(7));
                        temp.setHorometro(rs.getFloat(8));
                        temp.setNombreCompleto(rs.getString(9));
                        temp.setWhsName(rs.getString(10));
                        temp.setObs(rs.getString(11));


                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en listar los bidones por maquina"); // Usamos el método auxiliar
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
