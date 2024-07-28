package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Articulo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class ArticuloDao implements IArticulo {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el Articulos
     *
     * @param articulo
     * @param acc
     * @return
     */
    @Override
    public boolean registrarArticulo( Articulo articulo, String acc ) {

        int resp;

        try {
            resp = this.jdbcTemplate.update("execute abm_articulo @codArticulo=?, @codigoFamilia=?, @datoArt=?, @datoArtExt=?, @stock=?, @utm=?, @audUsuario=?, @ACCION=?",
                    ps -> {
                        ps.setString(1, articulo.getCodArticulo());
                        ps.setInt(2, articulo.getCodigoFamilia());
                        ps.setString(3,articulo.getDatoArt());
                        ps.setString(4, articulo.getDatoArtExt());
                        ps.setFloat(5, articulo.getStock());
                        ps.setFloat(6, articulo.getUtm());
                        ps.setInt(7, articulo.getAudUsuario());
                        ps.setString(8, acc);

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: ArticuloDao en registrarArticulo, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }
}
