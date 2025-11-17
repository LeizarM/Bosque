package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.model.Precio;
import bo.bosque.com.impexpap.model.SociosTigo;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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



        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_Usuario @codUsuario=?, @codEmpleado=?, @login=?, @password=?, @password2=?, @tipoUsuario=?, @esAutorizador=?, @estado=?  ,@audUsuarioI=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, login.getCodUsuario() );
                        ps.setInt(2, login.getCodEmpleado() );
                        ps.setString(3, login.getLogin() );
                        ps.setString(4, login.getPassword());
                        ps.setString(5,login.getPassword2());
                        ps.setString(6, login.getTipoUsuario());
                        ps.setString(7, login.getEsAutorizador());
                        ps.setString(8, login.getEstado());
                        ps.setInt(9, login.getAudUsuarioI());
                        ps.setString(10, oper);
                        //ps.executeUpdate();
                    });


        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: LoginDaoImpl en abmLogin, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp != 0;
    }

    /**
     * Procedimiento para verificar usuario usando el procedimiento almacenado
     * @param login Nombre de usuario
     * @param password2 Contraseña
     * @param ip Dirección IP del cliente
     * @return Objeto Login con información del usuario
     */
    public Login verifyUser(String login, String password2, String ip) {
        Login temp = new Login();
        try {
            // Llamamos al procedimiento almacenado que solo verifica si el usuario existe
            // La verificación de contraseña se hará en el controlador con Spring Security
            temp = this.jdbcTemplate.queryForObject(
                    "execute p_list_Usuario @login=?, @password2=?, @ip=?, @ACCION=?",
                    new Object[] { login, password2, ip, "C" },
                    new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Login login1 = new Login();
                        login1.setCodUsuario(rs.getInt(1));
                        login1.setCodEmpleado(rs.getInt(2));

                        // Si el codUsuario es positivo, cargamos todos los datos
                        if (login1.getCodUsuario() > 0) {
                            try {
                                login1.getEmpleado().getPersona().setDatoPersona(rs.getString(3));
                                login1.setCodSucursal(rs.getInt(4));
                                login1.setNombreSucursal(rs.getString(5));
                                login1.setCodCiudad(rs.getInt(6));
                                login1.setNombreCiudad(rs.getString(7));
                                login1.getEmpleado().getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion(rs.getString(8));
                                login1.setTipoUsuario(rs.getString(9));
                                login1.setCodEmpresa(rs.getInt(10));
                                login1.setNombreEmpresa(rs.getString(11));
                                login1.setElTemaSelecionado(rs.getString(12));
                                login1.setVersionApp(rs.getString(13));
                            } catch (Exception e) {
                                System.out.println("Error al mapear datos del usuario: " + e.getMessage());
                            }
                        }

                        // Obtener el número de intentos fallidos
                        try {
                            login1.setIntentosFallidos(rs.getInt(14));
                        } catch (Exception e) {
                            System.out.println("Error al obtener intentos fallidos: " + e.getMessage());
                            login1.setIntentosFallidos(0);
                        }

                        return login1;
                    }
            );
        } catch (BadSqlGrammarException e) {
            System.out.println("Error en verifyUser: " + e.getMessage() +
                    ", SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            temp = new Login();
            this.jdbcTemplate = null;
        } catch (Exception e) {
            System.out.println("Error en verifyUser: " + e.getMessage());
            temp = new Login();
        }

        return temp;
    }

    /**
     * Registra un intento fallido de login y verifica si la cuenta debe ser bloqueada
     * @param login Nombre de usuario
     * @param ip Dirección IP del cliente
     * @return Login actualizado con información de intentos fallidos y estado
     */
    public Login registerFailedAttempt(String login, String ip) {
        Login temp = new Login();
        try {
            // Llamamos al procedimiento almacenado con acción 'F'
            this.jdbcTemplate.queryForObject(
                    "execute p_list_Usuario @login=?, @ip=?, @ACCION=?",
                    new Object[] { login, ip, "F" },
                    new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR },
                    (rs, rowNum) -> {
                        // Obtenemos los resultados del procedimiento
                        temp.setCodUsuario(rs.getInt("codUsuario"));
                        temp.setIntentosFallidos(rs.getInt("intentosFallidos"));
                        return temp;
                    }
            );

            return temp;
        } catch (Exception e) {
            System.out.println("Error al registrar intento fallido: " + e.getMessage());
            return temp;
        }
    }

    /**
     * Registra un inicio de sesión exitoso en la bitácora
     * utilizando el procedimiento almacenado con acción 'S'
     *
     * @param login Nombre de usuario
     * @param ip Dirección IP del cliente
     */
    public void registerSuccessfulLogin(String login, String ip) {
        try {
            // Llamamos al procedimiento almacenado con acción 'S'
            this.jdbcTemplate.queryForObject(
                    "execute p_list_Usuario @login=?, @ip=?, @ACCION=?",
                    new Object[] { login, ip, "S" },
                    new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR },
                    (rs, rowNum) -> {
                        // No necesitamos procesar el resultado
                        return null;
                    }
            );
        } catch (Exception e) {
            System.out.println("Error al registrar login exitoso: " + e.getMessage());
        }
    }

    /**
     * Verifica si el nombre de usuario ya existe por empleado
     *
     * @param login
     * @return
     */
    @Override
    public int verifDuplicidad(Login login, String oper) {
        int existeDuplicado = 0;

        try {
            // Usar queryForList sin especificar tipo de columna
            // Esto funciona independientemente de qué columnas devuelva el SP
            List<Map<String, Object>> resultados = this.jdbcTemplate.queryForList(
                    "EXEC p_list_Usuario @codUsuario=?, @codEmpleado=?, @login=?, @ACCION=?",
                    new Object[]{login.getCodUsuario(), login.getCodEmpleado(), login.getLogin(), oper},
                    new int[]{Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR}
            );

            existeDuplicado = resultados.size();

        } catch (EmptyResultDataAccessException e) {
            existeDuplicado = 0;
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: LoginDaoImpl en verifDuplicidad, DataAccessException->" +
                    e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            existeDuplicado = 0;
        }

        return existeDuplicado;
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

    /**
     * Obtendra todos los usuarios
     *
     * @return
     */
    @Override
    public List<Login> getAllUsers() {

        List<Login> lstTemp = new ArrayList<>();
        try {
            lstTemp =  this.jdbcTemplate.query("execute dbo.p_list_Usuario @ACCION=?",
                    new Object[] { "L" },
                    new int[] {  Types.VARCHAR },
                    (rs, rowNum) -> {
                        Login temp = new Login();


                        temp.setCodUsuario( rs.getInt(1));
                        temp.setCodEmpleado( rs.getInt(2 ) );
                        temp.setFila( rowNum + 1);
                        temp.setNombreCompleto( rs.getString(4));
                        temp.setLogin( rs.getString(5));
                        temp.setTipoUsuario( rs.getString(6));
                        temp.setEsAutorizador( rs.getString(7));
                        temp.setEstado( rs.getString(8));

                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: LoginDaoImpl en getAllUsers, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;

    }


}
