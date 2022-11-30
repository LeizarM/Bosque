package bo.bosque.com.impexpap.dao;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import bo.bosque.com.impexpap.model.Dependiente;
import org.springframework.stereotype.Repository;


@Repository
public class DependienteDao implements IDependiente {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Obtendra la lista de dependientes de un empleado
     * @param codEmpleado
     * @return
     */
    @Override
    public List<Dependiente> obtenerDependientes( int codEmpleado ) {
        List<Dependiente> lstTemp = new ArrayList<>();
        try {
            lstTemp = this.jdbcTemplate.query("execute p_list_Dependiente @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "A" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)-> {
                        Dependiente dep = new Dependiente();
                        dep.setCodDependiente(rs.getInt(1));
                        dep.setCodPersona(rs.getInt(2));
                        dep.setEsActivo(rs.getString(3));
                        dep.setNombreCompleto(rs.getString(4));
                        dep.setDescripcion(rs.getString(5));
                        dep.setEdad(rs.getInt(6));
                        return dep;

                    });
        }catch (BadSqlGrammarException e) {
            System.out.println("Error: DependienteDao en obtenerDependientes, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
