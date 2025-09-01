package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Email;
import bo.bosque.com.impexpap.model.Persona;
import bo.bosque.com.impexpap.model.Telefono;
import bo.bosque.com.impexpap.model.TipoTelefono;
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
                            temp.setTipo(rs.getString(4));
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
    /**
     * Procedimiento para obtener el tipo de telefono
     * @param
     * @return
     */
    public List<TipoTelefono> obtenerTipoTelefono() {
        List<TipoTelefono> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query(" execute p_list_TipoTelefono @ACCION=?",
                    new Object[] { "L" },
                    new int[] {Types.VARCHAR },
                    (rs, rowCount)->{
                        TipoTelefono temp = new TipoTelefono();
                        temp.setCodTipoTel(rs.getInt(1));
                        temp.setTipo(rs.getString(2));
                        temp.setAudUsuario(rs.getInt(3));
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

    /**
     * Procedimiento para registro de Telefono
     * @param tel
     * @param acc
     * @return
     */
    public boolean registrarTelefono(Telefono tel, String acc) {
       int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_Telefono @codTelefono=?, @codPersona=?, @codTipoTel=?, @telefono=?, @audUsuarioI=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, tel.getCodTelefono() );
                        ps.setInt(2, tel.getCodPersona() );
                        ps.setInt(3, tel.getCodTipoTel() );
                        ps.setString(4, tel.getTelefono());
                        ps.setInt(5, tel.getAudUsuario());
                        ps.setString(6, acc);

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: TelefonoDao en registrarTelefono, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp != 0;
    }

    /**
     * Procedimiento para obtener el ultimo codigo de persona
     * @param audUsuario
     * @return
     */
    public int obtenerUltimoCodPersona( int audUsuario ) {
        Telefono temp = new Telefono();
        try {
            temp = this.jdbcTemplate.queryForObject("execute p_list_Telefono @audUsuarioI=?, @ACCION=?",
                    new Object[] { audUsuario, "A" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Telefono tel = new Telefono();
                        tel.setCodPersona(rs.getInt(1));
                        return tel;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: PersonaDao en obtenerUltimoPersona, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            temp = new Telefono();
        }
        return temp.getCodPersona();
    }

}
