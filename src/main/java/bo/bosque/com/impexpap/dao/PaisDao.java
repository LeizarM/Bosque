package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Pais;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PaisDao implements IPais {

    /**
     * El DataSource
     */
    @Autowired()
    JdbcTemplate jdbcTemplate;

    /**
     * Obtendra la lista de paises registrados
     * @return
     */
    public List<Pais> obtenerPais() {
        List<Pais> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_Pais @ACCION=?",
                    new Object[]{"L"},
                    new int[]{ Types.VARCHAR },
                    (rs, rowCount)->{
                        Pais temp = new Pais();
                        temp.setCodPais( rs.getInt(1) );
                        temp.setPais( rs.getString(2 ) );
                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: PaisDao en obtenerPais, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
