package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.Vista;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Repository
public class VistaDaoImpl implements IVistaDao {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para obtener el menu por usuario
     * @param codUsuario
     * @return List<Vista>
     */
    public List<Vista> obtainMenuXUser(int codUsuario ) {
        List<Vista> lstTemp;
        try {
            lstTemp =  this.jdbcTemplate.query("execute dbo.p_list_VistaUsuario  @codUsuario=?, @ACCION=?",
                        new Object[] {  codUsuario, "K" },
                        new int[] { Types.INTEGER, Types.VARCHAR },
                        (rs, rowNum) -> {
                            Vista temp = new Vista();

                            temp.setFila( rs.getInt(1 ) )
                                    .setCodVista( rs.getInt(2 ) )
                                    .setCodVistaPadre( rs.getInt(3 ) )
                                    .setDireccion( rs.getString(4 ) )
                                    .setTitulo( rs.getString(5 ) )
                                    .setEsRaiz( rs.getInt(6 ) )
                                    .setTieneHijo( rs.getInt(7 ) )
                                    .setPath( rs.getString(8 ) )
                                    .setLabel( temp.getTitulo() );
                            return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: VistaDaoImpl en obtainMenuXUser, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<Vista>();
            this.jdbcTemplate = null;
        }

        return  lstTemp;
    }

    /**
     * Procedimiento para obtener las rutas de las paginas por usuario
     * Solo de las rutas de los hijos
     * @param codUsuario
     * @return
     */
    public List<Vista> obtainRoutes(int codUsuario) {
        List<Vista> lstTemp;
        try {
            lstTemp =  this.jdbcTemplate.query("execute dbo.p_list_VistaUsuario  @codUsuario=?, @ACCION=?",
                    new Object[] {  codUsuario, "M" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Vista temp = new Vista();
                        temp.setDireccion( rs.getString(1 ) );
                        return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: VistaDaoImpl en obtainRoutes, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<Vista>();
            this.jdbcTemplate = null;
        }

        return  lstTemp;
    }
}
