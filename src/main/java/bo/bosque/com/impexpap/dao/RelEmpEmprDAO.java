package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RelEmplEmpr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RelEmpEmprDAO implements IRelEmpEmpr {

    /**
     * El DataSource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento para obtener el listado de relaciones laborales de un empleado
     * @param codEmpleado
     * @return
     */
    public List<RelEmplEmpr> obtenerRelacionesLaborales( int codEmpleado ) {

        List<RelEmplEmpr> lstTemp = new ArrayList<>();
        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_RelEmplEmpr @codEmpleado=?, @ACCION=?",
                    new Object[]{ codEmpleado, "A" },
                    new int[]{ Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) ->{
                        RelEmplEmpr temp = new RelEmplEmpr();

                        temp.setCodRelEmplEmpr(rs.getInt(1));
                        temp.setDatoFechasBeneficio(rs.getString(2));
                        return temp;
                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: RelEmpEmprDao en obtenerRelacionesLaborales, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return lstTemp;
    }
}
