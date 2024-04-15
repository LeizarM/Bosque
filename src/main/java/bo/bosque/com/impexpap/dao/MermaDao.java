package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Merma;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class MermaDao implements IMerma {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Para registrar la merma
     * @param regMerma
     * @param acc
     * @return
     */
    public boolean registrarMerma( Merma regMerma, String acc ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tprod_merma  @idMe = ?, @idLp = ?, @codArticulo = ?, @descripcion = ?, @peso = ?, @audUsuario = ?, @ACCION = ?",
                    ps -> {

                        ps.setInt(1, regMerma.getIdMe());
                        ps.setInt(2, regMerma.getIdLp());
                        ps.setString(3, regMerma.getCodArticulo());
                        ps.setString(4, regMerma.getDescripcion());
                        ps.setFloat(5, regMerma.getPeso());
                        ps.setInt(6, regMerma.getAudUsuario());
                        ps.setString(7, acc);

                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: MermaDao en registrarMerma, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }
}
