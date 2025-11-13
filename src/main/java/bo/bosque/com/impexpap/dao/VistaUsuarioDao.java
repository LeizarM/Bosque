package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.VistaUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;


@Repository
public class VistaUsuarioDao implements  IVistaUsuario{


    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;



    /**
     * Para registrar la vista por usuario
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarVistaUsuario( VistaUsuario mb, String acc ) {


        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_VistaUsuario  @codUsuario = ?, @codVista = ?, @nivelAcceso = ?, @autorizador = ?,  @audUsuarioI = ?, @ACCION = ?",
                    ps -> {

                        ps.setInt(1, mb.getCodUsuario());
                        ps.setInt(2, mb.getCodVista());
                        ps.setInt(3, mb.getNivelAcceso());
                        ps.setInt(4, mb.getAutorizador());
                        ps.setInt(5, mb.getAudUsuarioI());
                        ps.setString(6, acc);

                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: VistaUsuarioDao en registrarVistaUsuario, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }
}
