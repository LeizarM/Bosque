package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.Resmado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;

@Repository
public class ResmadoDao implements IResmado {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Para registrar el Resmado
     *
     * @param rmd
     * @param acc
     * @return true or false
     */
    @Override
    public boolean registrarResmado( Resmado rmd, String acc ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tprod_Resmado @idRes=?, @idGrupo=?, @codEmpleado=?, @fecha=?, @total=?, @hraInicio=?, @hraFin=?,  @audUsuario=? ,  @ACCION=?",
                    ps -> {

                        ps.setEscapeProcessing( true );
                        ps.setInt(1, rmd.getIdRes());
                        ps.setInt(2, rmd.getIdGrupo());
                        ps.setInt(3, rmd.getCodEmpleado());
                        ps.setDate(4, (Date) rmd.getFecha());
                        ps.setFloat(5, rmd.getTotal());
                        ps.setString(6, rmd.getHraInicio());
                        ps.setString(7, rmd.getHraFin());
                        ps.setInt(8, rmd.getAudUsuario());
                        ps.setString(9, acc);

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: ResmadoDao en registrarLoteProduccion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;


    }
}
