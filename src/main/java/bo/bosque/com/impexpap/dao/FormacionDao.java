package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Formacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class FormacionDao implements  IFormacion {

    /**
     * El DataSource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento para obtener la formacion de un empleado
     * @param codEmpleado
     * @return
     */
    public List<Formacion> obtenerFormacion( int codEmpleado ) {
        List<Formacion> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_Formacion @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "L" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
            (rs, rowCount)->{

                Formacion temp = new Formacion();

                temp.setCodFormacion(rs.getInt(1));
                temp.setCodEmpleado(rs.getInt(2));
                temp.setDescripcion(rs.getString(3));
                temp.setDuracion(rs.getInt(4));
                temp.setTipoDuracion(rs.getString(5));
                temp.setTipoFormacion(rs.getString(6));
                temp.setFechaFormacion(rs.getDate(7));
                temp.setAudUsuario(rs.getInt(8));
                return temp;

            });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: FormacionDao en obtenerFormacion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
