package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Login;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Repository;


import java.sql.SQLException;
import java.util.List;

@Repository
public class LoginDaoImpl implements ILoginDao{

    @Autowired
    JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para el abm de la informacion
     * @param login
     * @return
     */
    public boolean abmLogin(Login login) {
        return false;
    }

    /**
     * Prccedimiento para obtener el usuario
     * @param login
     * @param password
     * @return
     */
    public Login obtainUser(String login, String password) {
        Login temp;
        try {
             temp = this.jdbcTemplate.queryForObject("execute p_list_Usuario @login=?, @password=?, @ACCION=?",
                    new Object[] { login, password, "X1" }
                    ,(rs, rowNum) -> {
                        Login login1 = new Login();
                        login1.getEmp().setNumCuenta(rs.getInt(1));
                        login1.setCodUsuario(rs.getInt(2));
                        login1.setCodEmpleado(rs.getInt(3));
                        login1.setLogin(rs.getString(4));
                        login1.setPassword(rs.getString(5));
                        login1.setTipoUsuario(rs.getString(6));
                        login1.setEsAutorizador(rs.getString(7));
                        login1.setEstado(rs.getString(8));
                        login1.setAudUsuarioI(rs.getInt(9));
                        return login1;
                    });

        }  catch (Exception e){
            System.out.println(" LoginDaoImp en obtainUser Error general: " + e);
            e.printStackTrace();
            e.getMessage();
            temp = null;
            //probando
        }
        finally {
            try{
                this.jdbcTemplate = null;
            }
            catch (Exception e){
                System.out.println(" TarRuXCargoDao en abmTarRuXCargo Error cerrando: " + e);
            }
        }

        System.out.println(temp.toString());

        return temp;
    }
}
