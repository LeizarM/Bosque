package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.DetalleResmado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class DetalleResmadoDao implements  IDetalleResmado {


    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Para registrar el Detalle Resmado
     *
     * @param rdr
     * @param acc
     * @return true or false
     */
    @Override
    public boolean registrarDetalleResmado( DetalleResmado rdr, String acc ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tprod_DetalleResmado @idDetRes=?, @idRes=?,  @codArticulo=?, @descripcion=?, @cantResma=?,  @audUsuario=? ,  @ACCION=?",
                    ps -> {

                        ps.setEscapeProcessing( true );
                        ps.setInt( 1, rdr.getIdRetRes());
                        ps.setInt(2, rdr.getIdRes());
                        ps.setString(3, rdr.getCodArticulo());
                        ps.setString(4, rdr.getDescripcion());
                        ps.setFloat(5, rdr.getCantResma());
                        ps.setInt(6, rdr.getAudUsuario());
                        ps.setString(7, acc);

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: DetalleResmadoDao en registrarDetalleResmado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;



    }
}
