package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Contenedor;
import bo.bosque.com.impexpap.model.TipoContenedor;
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
public class TipoContenedorDao implements  ITipoContenedor{

    private static final Logger logger = LoggerFactory.getLogger(TipoContenedor.class);


    private final JdbcTemplate jdbcTemplate;

    public TipoContenedorDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Obtendra el tipo de contenedor
     * @return
     */
    @Override
    public List<TipoContenedor> lstTipoContenedores() {
        List<TipoContenedor> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_tipo  @ACCION=?",
                    new Object[] { "L" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        TipoContenedor temp = new TipoContenedor();

                        temp.setIdTipo(rs.getInt(1));
                        temp.setTipo(rs.getString(2));
                        temp.setAudUsuario(rs.getInt(3));


                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar los tipos de contenedores"); // Usamos el método auxiliar
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
