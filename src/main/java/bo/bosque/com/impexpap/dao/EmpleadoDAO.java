package bo.bosque.com.impexpap.dao;
import bo.bosque.com.impexpap.model.Empleado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class EmpleadoDAO implements IEmpleado{

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para obtener los Empleados
     * @return List<Empleado>
     */
    public List<Empleado> obtenerListaEmpleados( int esActivo ) {
        List<Empleado>lstTemp;
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_Empleado @esActivo=?, @ACCION=?",
                    new Object[] { esActivo, "B" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Empleado temp = new Empleado();
                        temp.setCodEmpleado( rs.getInt(1) );
                        temp.setCodPersona( rs.getInt(2) );
                        temp.getPersona().setDatoPersona( rs.getString(3) );
                        temp.getRelEmpEmpr().setEsActivo( rs.getInt(4) );
                        temp.getCargo().setDescripcion(rs.getString(5));

                        return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: EmpleadoDAO en obtenerEmpleados, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return  lstTemp;
    }

    /**
     *
     * Obtendra a un empleado por su codigo
     * @param codEmpleado
     * @return
     */
    public Empleado obtenerEmpleado( int codEmpleado ) {
        Empleado emp;
        try {
            emp =  this.jdbcTemplate.queryForObject("execute p_list_Empleado @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "A" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Empleado temp = new Empleado();

                    return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: EmpleadoDAO en obtenerEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            emp = new Empleado();
            this.jdbcTemplate = null;
        }

        return emp;
    }
}
