package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Telefono;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TelefonoDao implements ITelefono {

    /**
     * El Datasource
     */
     @Autowired()
     private JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para obtener el listade de telefonos por persona
     * @param codPersona
     * @return
     */
    public List<Telefono> obtenerTelefonos( int codPersona ) {
         List<Telefono> lstTemp = new ArrayList<>();

         try{
             lstTemp = this.jdbcTemplate.query(" execute p_list_Telefono @codPersona=?, @ACCION=?",
                     new Object[] { codPersona, "L" },
                     new int[] { Types.INTEGER, Types.VARCHAR },
                     (rs, rowCount)->{
                         Telefono temp = new Telefono();
                            temp.setCodTelefono(rs.getInt(1));
                            temp.setCodPersona(rs.getInt(2));
                            temp.setCodTipoTel(rs.getInt(3));
                            temp.setTelefono(rs.getString(5));
                            temp.setAudUsuario(rs.getInt(6));
                         return temp;
                     }
             );

         }catch (BadSqlGrammarException e) {
             System.out.println("Error: EmailDao en obtenerCorreos, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
             lstTemp = new ArrayList<>();
             this.jdbcTemplate = null;
         }

        return lstTemp;
    }
}
