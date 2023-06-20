package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.GrupoFamTipoRangoGram;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class GrupoFamTipoRangoGramDao implements  IGrupoFamTipoRangoGram{

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el Grupo Familia y Tipo Rango Gramaje
     *
     * @param grupoFamTipoRangoGram
     * @param acc
     */
    @Override
    public boolean registrarGrupoFamTipoRangoGram(GrupoFamTipoRangoGram grupoFamTipoRangoGram, String acc) {

        int resp;
        try {

            resp = this.jdbcTemplate.update("execute @idGrpFamiliaSap=?, @idTipo=?, @idRangoGram=?, @audUsuario=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, grupoFamTipoRangoGram.getIdGrpFamiliaSap());
                        ps.setInt(2, grupoFamTipoRangoGram.getIdTipo());
                        ps.setInt(3, grupoFamTipoRangoGram.getIdRangoGram());
                        ps.setInt(4, grupoFamTipoRangoGram.getAudUsuario());
                        ps.setString(5, acc);
                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: GrupoFamTipoRangoGramDao en registrarGrupoFamTipoRangoGram, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }
}
