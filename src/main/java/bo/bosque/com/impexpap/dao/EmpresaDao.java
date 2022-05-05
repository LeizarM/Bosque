package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Empresa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class EmpresaDao implements IEmpresa {

    /**
     * El Datasource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento para obtener las empresas registradas
     * @return
     */
    public List<Empresa> obtenerEmpresas() {
        List<Empresa> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_empresa @ACCION=?",
                    new Object[]{ "A" },
                    new int[]{ Types.VARCHAR },
                    (rs, rowCount) ->{
                        Empresa temp = new Empresa();
                        temp.setCodEmpresa(rs.getInt(1));
                        temp.setNombre(rs.getString(2));
                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EmpresaDao en obtenerEmpresas, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
