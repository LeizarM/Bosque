package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Tipo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;

@Repository
public class TipoDao implements ITipo {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el tipo de papel
     *
     * @param tipo
     * @param acc
     * @return
     */
    @Override
    public boolean registrarTipo(Tipo tipo, String acc) {

        int resp;

        try{

            resp = this.jdbcTemplate.update("execute p_abm_tipo @idTipo=?, @tipo=?, @estado=?, @audUsuario=?, @ACCION=?",
                    ps -> {
                        ps.setEscapeProcessing(true);
                        ps.setQueryTimeout(180);
                        ps.setInt(1, tipo.getIdTipo());
                        ps.setString(2, tipo.getTipo());
                        ps.setInt(3, tipo.getEstado());
                        ps.setInt(4, tipo.getAudUsuario());
                        ps.setString(5, acc);
                    });

        }catch (  BadSqlGrammarException e){
            System.out.println("Error: TipoDao en registrarTipo, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }


        return resp != 0;
    }
}
