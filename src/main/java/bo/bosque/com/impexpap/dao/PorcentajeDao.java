package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Porcentaje;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class PorcentajeDao implements IPorcentaje {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el porcentaje
     * @param porcentaje
     * @param acc
     * @return
     */
    @Override
    public boolean registrarPorcentaje( Porcentaje porcentaje, String acc ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_porcentaje @idPorcen=?, @codigoFamilia=?, @idClasificacion=? ,@porcen=? ,@audUsuario=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, porcentaje.getIdPorcen());
                        ps.setInt(2, porcentaje.getCodigoFamilia());
                        ps.setInt(3,porcentaje.getIdClasificacion());
                        ps.setFloat(4, porcentaje.getPorcen());
                        ps.setInt(5, porcentaje.getAudUsuario());
                        ps.setString(6, acc);
                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: PorcentajeDao en registrarPorcentaje, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }
}
