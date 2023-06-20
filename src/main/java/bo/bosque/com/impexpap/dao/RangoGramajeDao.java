package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RangoGramaje;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class RangoGramajeDao implements IRangoGramaje {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el Rango Gramaje
     *
     * @param rangoGramaje
     * @param acc
     * @return
     */
    @Override
    public boolean registrarRangoGramaje(RangoGramaje rangoGramaje, String acc) {

        int resp;

        try{
            resp =  this.jdbcTemplate.update("execute p_abm_rangoGramaje @idRangoGram=?, @min=?, @max=?, @audUsuario=?, @ACCION=?",
                    ps ->{
                        ps.setEscapeProcessing(true);
                        ps.setQueryTimeout(180);
                        ps.setInt(1, rangoGramaje.getIdRangoGram());
                        ps.setFloat(2, rangoGramaje.getMin());
                        ps.setFloat(3, rangoGramaje.getMax());
                        ps.setInt(4, rangoGramaje.getAudUsuario());
                        ps.setString(5, acc);
                    });
        }catch( BadSqlGrammarException e){
            System.out.println("Error: RangoGramajeDao en registrarRangoGramaje, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }
}
