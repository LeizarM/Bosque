package bo.bosque.com.impexpap.dao;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import bo.bosque.com.impexpap.model.Autorizacion;
import bo.bosque.com.impexpap.utils.Tipos;


@Repository
public class AutorizacionDao implements IAutorizacion{


    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Metodo para listar las autorizaciones
     * @return
     */
    @Override
    public List<Autorizacion> listAutorizacion() {
        List<Autorizacion> lstTemp = new ArrayList<Autorizacion>();
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_autorizacion @ACCION = ?",
                    new Object[] { "A" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        Autorizacion temp = new Autorizacion();

                        temp.setIdAutorizacion( rs.getInt(1 ) );
                        temp.setIdPropuesta( rs.getInt(2 ) );
                        temp.getRegProp().setTipo ( rs.getInt(3 ) );
                        temp.getRegProp().setTitulo( rs.getString(4 ));
                        temp.setDatoUsuarioP( rs.getString(5 ) );
                        temp.getRegProp().setAudFecha( rs.getDate(6 ) );
                        temp.setEstadoCad( rs.getString(7 ) );
                        temp.setDatoUsuarioAP( rs.getString(8 ) );
                        temp.setAudFecha(rs.getDate(9 ) );
                        temp.setDatoUsuarioGP(rs.getString(10 ) );
                        temp.getRegProp().setAudFecGenerado( rs.getDate(11) );
                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: VistaDaoImpl en AutorizacionDao, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<Autorizacion>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Obtendra una lista de los estados de las propuestas
     * @return
     */
    public List<Tipos> lstEstadoPropuestas() {
        return new Tipos().lstEstadoPropuesta();
    }


}
