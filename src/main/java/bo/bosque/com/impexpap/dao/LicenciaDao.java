package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Licencia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class LicenciaDao implements  ILicencia {

    /**
     * El DataSource
     */
     @Autowired()
    JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para obtener la licencia de conducir de una persona
     * @param codPersona
     * @return
     */
    public List<Licencia> obtenerLicencia(int codPersona) {

        List<Licencia> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_Licencia @codPersona=?, @ACCION=?",
                    new Object[] { codPersona, "L" },
                    new int[] {Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{
                        Licencia temp = new Licencia();
                        temp.setCodLicencia(rs.getInt(1));
                        temp.setCodPersona(rs.getInt(2));
                        temp.setCategoria(rs.getString(3));
                        temp.setFechaCaducidad(rs.getDate(4));
                        temp.setAudUsuario(rs.getInt(5));
                        return temp;
                    });

        }catch (BadSqlGrammarException e){
            System.out.println("Error: LicenciaDao en obtenerLicencia, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;

    }
}
