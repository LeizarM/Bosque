package bo.bosque.com.impexpap.dao;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


import bo.bosque.com.impexpap.model.GaranteReferencia;

@Repository
public class GaranteReferenciaDao implements IGaranteReferencia {

    private JdbcTemplate jdbcTemplate;

    public GaranteReferenciaDao( JdbcTemplate jdbcTemplate ) {
        this.jdbcTemplate = jdbcTemplate;
    }


    /**
     * Metodo para listar los garantes o referencias de un empleado
     * @param codEmpleado
     * @return
     */
    public List<GaranteReferencia> obtenerGaranteReferencia( int codEmpleado ) {
        List<GaranteReferencia> lstTemp = new ArrayList<>();
        try {
            lstTemp = this.jdbcTemplate.query("execute p_list_GaranteReferencia @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "R" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)-> {
                        GaranteReferencia garRef = new GaranteReferencia();

                        garRef.setCodGarante(rs.getInt(1));
                        garRef.setCodPersona(rs.getInt(2));
                        garRef.setNombreCompleto(rs.getString(3));
                        garRef.setDireccionTrabajo(rs.getString(4));
                        garRef.setEmpresaTrabajo(rs.getString(5));
                        garRef.setTipo(rs.getString(6));
                        garRef.setObservacion( rs.getString(7));
                        garRef.setEsEmpleado( rs.getString(8));

                        return garRef;

                    });
        }catch (BadSqlGrammarException e) {
            System.out.println("Error: GaranteReferenciaDao en obtenerGaranteReferencia, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return lstTemp;
    }
}
