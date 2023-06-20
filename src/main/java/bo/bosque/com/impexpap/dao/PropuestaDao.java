package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Propuesta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;

@Repository
public class PropuestaDao implements  IPropuesta{

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * para registrar la propuesta
     *
     * @param propuesta
     * @param acc
     * @return
     */
    @Override
    public boolean registrarPropuesta(Propuesta propuesta, String acc) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_propuesta @idPropuesta=?, @codEmpresa=?, @tipo=?,@titulo=?, @obs=?, @estado=? ,@audUsGenerado=?,  @audFecGenerado=?, @audUsuario=?, @ACCION=?",
                    ps -> {
                        ps.setEscapeProcessing(true);
                        ps.setQueryTimeout(180);
                        ps.setInt(1, propuesta.getIdPropuesta());
                        ps.setInt(2, propuesta.getCodEmpresa());
                        ps.setInt(3, propuesta.getTipo());
                        ps.setString(4, propuesta.getTitulo());
                        ps.setString(5, propuesta.getObs());
                        ps.setInt(6, propuesta.getEstado());
                        ps.setInt(7, propuesta.getAudUsGenerado());
                        ps.setDate(8, (Date) propuesta.getAudFecGenerado());
                        ps.setInt(9, propuesta.getAudUsuario());
                        ps.setString(10, acc);
                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: PropuestaDao en registrarPropuesta, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return false;
    }
}
