package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Login;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

@Service
//@Repository
public class LoginDaoImpl implements ILoginDao, UserDetailsService {

    /**
     * El Datasource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para el abm del login
     * @param login
     * @param oper
     * @return true si se realizo con exito
     */
    public boolean abmLogin( Login login, String oper ) {
        return false;
    }

    /**
     * Prccedimiento para obtener el usuario
     * @param login
     * @param password
     * @return
     */
    public Login verifyUser(String login, String password, String ip) {
        Login temp = new Login();
        try {
              temp =  this.jdbcTemplate.queryForObject("execute p_list_Usuario @login=?, @password=?, @ip=? ,@ACCION=?",
                      new Object[] { login, password, ip ,"C" },
                      new int[] { Types.VARCHAR, Types.VARCHAR ,Types.VARCHAR, Types.VARCHAR }
                    ,(rs, rowNum) -> {
                        Login login1 = new Login();

                        login1.setCodUsuario(rs.getInt(1 ));
                        login1.getEmpleado().getPersona().setDatoPersona(rs.getString(2 ));
                        login1.setCodSucursal(rs.getInt(3 ));
                        login1.setNombreSucursal(rs.getString(4 ));
                        login1.setCodCiudad(rs.getInt(5 ));
                        login1.setNombreCiudad(rs.getString(6 ));
                        login1.getEmpleado().getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion(rs.getString(7 ));
                        login1.setTipoUsuario( rs.getString(8) );
                        login1.setCodEmpresa(rs.getInt(9 ));
                        login1.setNombreEmpresa(rs.getString(10 ));
                        login1.setElTemaSelecionado(rs.getString(11 ));

                        return login1;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: LoginDaoImpl en verifyUser, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            temp = new Login();
            this.jdbcTemplate = null;
        }
        return temp;

    }

    /**
     * Procedimiento para verificar si existe el usuario en la BD
     * @param login
     * @return Userdetails
     * @throws UsernameNotFoundException
     */
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {

        Login temp = new Login();
        try {
            temp = this.jdbcTemplate.queryForObject("execute p_list_Usuario @login=?, @ACCION=?",
                    new Object[]{login, "B"},
                    new int[]{Types.VARCHAR, Types.VARCHAR}
                    , (rs, rowNum) -> {
                        Login login1 = new Login();
                        login1.setLogin(rs.getString(1));
                        login1.setTipoUsuario( rs.getString( 2) );
                        login1.setPassword( rs.getString( 3 ) );
                        return login1;
                    });

        } catch (BadSqlGrammarException e) {
            System.out.println("Error: LoginDaoImpl en loadUserByUsername, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            temp = new Login();
            this.jdbcTemplate = null;
        } catch (UsernameNotFoundException e) {
            System.out.println("Error: LoginDaoImpl en loadUserByUsername, UsernameNotFoundException->" + e.getMessage());
        }catch (EmptyResultDataAccessException e) {
            System.out.println("No record found in database for "+login+" "+ e.getMessage());

        }
        GrantedAuthority authority = new SimpleGrantedAuthority(temp.getTipoUsuario());
        return new User(temp.getLogin(), temp.getPassword() ,Arrays.asList( authority ));

    }
}
