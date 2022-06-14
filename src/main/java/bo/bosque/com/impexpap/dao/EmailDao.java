package bo.bosque.com.impexpap.dao;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import bo.bosque.com.impexpap.model.Email;
import org.springframework.stereotype.Repository;


@Repository
public class EmailDao implements IEmail {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento que obtendra los correos por persona
     * @param codPersona
     * @return
     */
    public List<Email> obtenerCorreos(int codPersona) {

        List<Email> lstTemp = new ArrayList<>();
        try {
            lstTemp = this.jdbcTemplate.query("execute p_list_Email @codPersona=?, @ACCION=?",
                        new Object[] { codPersona, "L" },
                        new int[] { Types.INTEGER, Types.VARCHAR },
                        (rs, rowCount)-> {
                            Email email = new Email();
                            email.setCodEmail(rs.getInt(1));
                            email.setCodPersona(rs.getInt(2));
                            email.setEmail(rs.getString(3));
                            email.setAudUsuario(rs.getInt(4));
                            return email;

            });
        }catch (BadSqlGrammarException e) {
            System.out.println("Error: EmailDao en obtenerCorreos, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return lstTemp;
    }

    /**
     * Procedimiento para registrar los Emails
     * @param email
     * @param acc
     * @return
     */
    public boolean registrarEmail( Email email, String acc ){

        System.out.println(email.toString());
        System.out.println(acc);
        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_Email @codEmail=?, @codPersona=?, @email=?, @audUsuarioI=?, @ACCION=?",
                    ps -> {
                    	ps.setInt(1, email.getCodEmail() );
                    	ps.setInt(2, email.getCodPersona() );
                    	ps.setString(3, email.getEmail() );
                    	ps.setInt(4, email.getAudUsuario());
                    	ps.setString(5, acc);
                    	//ps.executeUpdate();
                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: EmailDao en registrarEmail, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp != 0;
    }
}
