package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RegistroResma;
import bo.bosque.com.impexpap.model.TipoDano;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class TipoDanoDao implements ITipoDano{

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Registra un nuevo tipo de daño
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarTipoDano(TipoDano mb, String acc) {
        return false;
    }

    /**
     * Obtiene la lista de tipos de daño
     *
     * @return
     */
    @Override
    public List<TipoDano> lstTipoDano() {

        List<TipoDano> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tmme_TipoDano @ACCION=?",
                    new Object[]{ "L" },
                    new int[]{ Types.VARCHAR },
                    (rs, rowCount) ->{

                        TipoDano temp = new TipoDano();

                        temp.setIdTd(rs.getInt(1));
                        temp.setDescripcion(rs.getString(2));
                        temp.setAudusuario(rs.getInt(3));
                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: TipoDanoDao en lstTipoDano, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
