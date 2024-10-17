package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.GrupoFamiliaSap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class GrupoFamiliaDao implements IGrupoFamiliaSap{

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el grupo de familia SAP
     *
     * @param grupoFamiliaSap
     * @param acc
     * @return
     */
    @Override
    public boolean registrarGrupoFamiliaSap(GrupoFamiliaSap grupoFamiliaSap, String acc) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute @idGrpFamiliaSap=?, @codGrpFamSap=?,@codGrpFamSapEpp=?,@grpFam =?,@alias =?,@audUsuario =?,@ACCION  = ? ",
                    ps-> {
                        ps.setInt(1, grupoFamiliaSap.getIdGrpFamiliaSap());
                        ps.setInt(2, grupoFamiliaSap.getCodGrpFamSap());
                        ps.setInt(3, grupoFamiliaSap.getCodGrpFamSapEpp());
                        ps.setString(4, grupoFamiliaSap.getGrpFam());
                        ps.setString(5, grupoFamiliaSap.getAlias());
                        ps.setInt(6, grupoFamiliaSap.getAudUsuario());
                        ps.setString(7, acc);
                    });
        }catch(BadSqlGrammarException e){
            System.out.println("Error: GrupoFamiliaDao en registrarGrupoFamiliaSap, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }
}
