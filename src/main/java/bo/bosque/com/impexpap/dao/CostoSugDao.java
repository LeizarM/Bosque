package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CostoSug;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class CostoSugDao implements ICostoSug {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Registrar el costo sugerido
     * @param costoSug
     * @param acc
     * @return
     */
    @Override
    public boolean registrarCostoSug( CostoSug costoSug, String acc ) {

        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_costoSug  @idCosSug=?, @idPropuesta=? , @codigoFamilia=?, @costoSug=?,@audUsuario=?,  @ACCION=?",
                    ps ->{
                        ps.setInt(1,costoSug.getIdCosSug());
                        ps.setInt(2, costoSug.getIdPropuesta());
                        ps.setInt(3, costoSug.getCodigoFamilia());
                        ps.setFloat(4, costoSug.getCostoSug());
                        ps.setInt(5, costoSug.getAudUsuario());
                        ps.setString(6, acc);
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: CostoSugDao en registrarCostoSug, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }
}
