package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.MaquinaMontacarga;
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
public class MaquinaMontacargaDao implements IMaquinaMontacarga{


    private static final Logger logger = LoggerFactory.getLogger(MaquinaMontacarga.class);

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_tgas_MaquinaMontacarga " +
                    "@idMaquina = ?, " +
                    "@codigo = ?, " +
                    "@marca = ?, " +
                    "@clase = ?, " +
                    "@anio = ?, " +
                    "@color = ?, " +
                    "@codSucursal = ?, " +
                    "@estado = ?, " +
                    "@audUsuario = ?, " +
                    "@ACCION = ?";

    private final JdbcTemplate jdbcTemplate;

    public MaquinaMontacargaDao( JdbcTemplate jdbcTemplate ) {

        this.jdbcTemplate = jdbcTemplate;
    }



    @Override
    public List<MaquinaMontacarga> listMaquinaMontacargas() {
        List<MaquinaMontacarga> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tgas_MaquinaMontacarga  @ACCION=?",
                    new Object[] { "A" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        MaquinaMontacarga temp = new MaquinaMontacarga();

                        temp.setIdMaquina(rs.getInt(1));
                        temp.setCodigo(rs.getString(2));
                        temp.setNombreSucursal(rs.getString(3));

                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en listMaquinaMontacargas"); // Usamos el método auxiliar
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
