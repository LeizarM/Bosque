package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.PrestamoEstado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class PrestamoEstadoDao implements IPrestamoEstado {


    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;




    /**
     * Para registrar un nuevo estado de préstamo
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarPrestamoEstado(PrestamoEstado mb, String acc) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tpre_PrestamoEstado @idPE=?, @idPrestamo=?, @idEst=?, @momento=?,  @audUsuario =?, @ACCION=? ",
                    ps -> {

                        ps.setEscapeProcessing( true );
                        ps.setInt(1, mb.getIdPE());
                        ps.setInt(2, mb.getIdPrestamo());
                        ps.setInt(3, mb.getIdEst());
                        ps.setString(4, mb.getMomento());
                        ps.setInt(5, mb.getAudUsuario());
                        ps.setString(6, acc);

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: PrestamoEstadoDao en registrarPrestamoEstado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;




    }
}
