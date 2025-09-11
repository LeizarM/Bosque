package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Contenedor;
import bo.bosque.com.impexpap.model.DepositoCheque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;



@Repository
public class ContenedorDao implements IContenedor{

    private static final Logger logger = LoggerFactory.getLogger(ContenedorDao.class);


    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_tgas_Contenedor " +
                    "@idContenedor = ?, " +
                    "@codigo = ?, " +
                    "@idTipo = ?, " +
                    "@codSucursal = ?, " +
                    "@descripcion = ?, " +
                    "@unidadMedida = ?, " +
                    "@audUsuario = ?, " +
                    "@ACCION = ?";

    private final JdbcTemplate jdbcTemplate;

    public ContenedorDao( JdbcTemplate jdbcTemplate ) {
        this.jdbcTemplate = jdbcTemplate;
    }


    /**
     * Metodo para listar los contenedores de combustible ya sea gas o diesel o garrafa
     * @return
     */
    @Override
    public List<Contenedor> listContenedor() {
        List<Contenedor> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_Contenedor  @ACCION=?",
                    new Object[] { "A" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        Contenedor temp = new Contenedor();

                        temp.setIdContenedor(rs.getInt(1));
                        temp.setCodigo(rs.getString(2));
                        temp.setCodSucursal(rs.getInt(3));
                        temp.setDescripcion(rs.getString(4));
                        temp.setUnidadMedida(rs.getString(5));
                        temp.setClase(rs.getString(6));
                        temp.setIdTipo(rs.getInt(7));
                        temp.setNombreSucursal(rs.getString(8));
                        temp.setSaldoActualCombustible(rs.getFloat(9));


                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar los contenedores"); // Usamos el método auxiliar
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
