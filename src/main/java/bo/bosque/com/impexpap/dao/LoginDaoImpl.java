package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Login;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Repository;


import java.sql.SQLException;

@Repository
public class LoginDaoImpl implements ILoginDao{

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
                      new Object[] { login, password, ip ,"V" }
                    ,(rs, rowNum) -> {
                        Login login1 = new Login();

                        login1.setCodUsuario(rs.getInt(1 ));
                        login1.getEmpleado().getPersona().setDatoPersona(rs.getString(2 ));
                        login1.getSucursal().setCodSucursal(rs.getInt(3 ));
                        login1.getSucursal().setNombre(rs.getString(4 ));
                        login1.getSucursal().setCodCiudad(rs.getInt(5 ));
                        login1.getSucursal().setNombreCiudad(rs.getString(6 ));
                        login1.getEmpleado().getCargo().setDescripcion(rs.getString(7 ));
                        login1.setTipoUsuario( rs.getString(8) );
                        login1.getSucursal().setCodEmpresa(rs.getInt(9 ));
                        login1.getSucursal().setNombreEmpresa(rs.getString(10 ));
                        login1.setElTemaSelecionado(rs.getString(11 ));

                        return login1;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: LoginDaoImpl en verifyUser, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            temp = new Login();
            this.jdbcTemplate = null;
        }
        //System.out.println( temp.toString() );
        return temp;

    }

    /**
     * Procedimiento para obtener menu
     */
}
