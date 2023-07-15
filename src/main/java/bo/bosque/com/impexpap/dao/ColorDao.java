package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Color;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class ColorDao implements IColor {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el color
     *
     * @param color
     * @param acc
     * @return
     */
    @Override
    public boolean registrarColor(Color color, String acc) {

        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_Color @idColor=?, @color=?, @estado=?, @audUsuario=?",
                    ps -> {
                        ps.setInt(1, color.getIdColor());
                        ps.setString(2, color.getColor());
                        ps.setInt(3, color.getEstado());
                        ps.setInt(4, color.getAudUsuario());
                        ps.setString(5, acc);
                    });
        }catch( BadSqlGrammarException e ){
            System.out.println("Error: ColorDao en registrarColor, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }
}
