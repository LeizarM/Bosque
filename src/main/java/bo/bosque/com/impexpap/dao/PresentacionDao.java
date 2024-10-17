package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Presentacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class PresentacionDao implements IPresentacion {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para registrar la presentacion
     * @param presentacion
     * @param acc
     * @return
     */
    @Override
    public boolean registrarPresentacion(Presentacion presentacion, String acc) {

        int resp;

        try{

            resp = this.jdbcTemplate.update("execute p_abm_presentacion @idPresentacion=?, @presentacion=?, @estado=?, @audUsuario=?, @ACCION=?",
                    ps -> {
                        ps.setEscapeProcessing(true);
                        ps.setQueryTimeout(180);
                        ps.setInt(1, presentacion.getIdPresentacion());
                        ps.setString(2, presentacion.getPresentacion());
                        ps.setInt(3, presentacion.getEstado());
                        ps.setInt(4, presentacion.getAudUsuario());
                        ps.setString(5, acc);
                    });

        }catch (  BadSqlGrammarException e ){
            System.out.println("Error: PresentacionDao en registrarPresentacion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }
}
