package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ExperienciaLaboral;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class ExperienciaLaboralDao implements IExperienciaLaboral {

    /**
     * El Datasoruce
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento que devolvera la experiencia laboral por empleado
     * @param codEmpleado
     * @return
     */
    public List<ExperienciaLaboral> obtenerExperienciaLaboral( int codEmpleado ) {

        List<ExperienciaLaboral> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_ExperienciaLaboral @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "L" },
                    new int[] {Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{
                        ExperienciaLaboral temp = new ExperienciaLaboral();
                        temp.setCodExperienciaLaboral(rs.getInt(1));
                        temp.setCodEmpleado(rs.getInt(2));
                        temp.setNombreEmpresa(rs.getString(3));
                        temp.setCargo(rs.getString(4));
                        temp.setDescripcion(rs.getString(5));
                        temp.setFechaInicio(rs.getDate(6));
                        temp.setFechaFin(rs.getDate(7));
                        temp.setNroReferencia(rs.getString(8));
                        temp.setAudUsuario(rs.getInt(9));
                        return temp;
                    }

            );
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: ExperienciaLaboralDao en obtenerExperienciaLaboral, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
