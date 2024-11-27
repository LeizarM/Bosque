package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.EstadoChofer;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class EstadoChoferDao implements  IEstadoChofer {


    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Override
    public List<EstadoChofer> listarEstadoChofer() {

        List<EstadoChofer> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tpre_estado @ACCION = ?",
                    new Object[]{ "L" },
                    new int[]{  Types.VARCHAR },
                    (rs, rowCount) ->{
                        EstadoChofer temp = new EstadoChofer();

                        temp.setIdEst( rs.getInt(1) );
                        temp.setEstado(rs.getString(2) );
                        temp.setAudUsuario( rs.getInt(3) );

                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EstadoChoferDao en listarEstadoChofer, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
