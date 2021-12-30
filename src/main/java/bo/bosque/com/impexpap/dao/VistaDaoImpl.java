package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.Vista;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

@Repository
public class VistaDaoImpl implements IVistaDao {

    /**
     * El Datasource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para obtener el menu por usuario
     * @param codUsuario
     * @return
     */
    public List<Vista> obtainMenuXUser(int codUsuario ) {
        List<Vista> lstTemp;
        try {
            lstTemp =  this.jdbcTemplate.query("execute dbo.p_list_VistaUsuario  @codUsuario=?, @ACCION=?",  new Object[] { codUsuario, "K" }, (rs, rowNum) -> {
                Vista temp = new Vista();

                temp.setFila(rs.getInt(1 ));
                temp.setCodVista(rs.getInt(2 ));
                temp.setCodVistaPadre(rs.getInt(3 ));
                temp.setDireccion(rs.getString(4 ));
                temp.setTitulo(rs.getString(5 ));
                temp.setEsRaiz(rs.getInt(6 ));
                temp.setLabel(rs.getString(7 ));
                return temp;

            });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: VistaDaoImpl en obtainMenuXUser, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = null;
            this.jdbcTemplate = null;
        }

        return  lstTemp;
    }
}
