package bo.bosque.com.impexpap.dao;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.model.ChBanco;


@Repository
public class ChBancoDAO implements IChBanco {

    /******************
     * El Datasource
     *******************/
    @Autowired
    private JdbcTemplate jdbcTemplate;

    
    
    /*******************************************************
     *  Funcion de devuelve el listado de bancos de la DB
     * @return LinkedList
     ********************************************************/
    public List<ChBanco> listBancos() {
    	List<ChBanco> lstTemp = new ArrayList<ChBanco>();
    	
    	  try {
              lstTemp =  this.jdbcTemplate.query("execute dbo.p_list_Banco  @ACCION=?",
                          new Object[] {  "L" },
                          new int[] {  Types.VARCHAR },
                          (rs, rowNum) -> {
                        	  ChBanco temp = new ChBanco();

                              temp.setFila( rowNum +1 );
                              temp.setCodBanco( rs.getInt(1 ) );
                              temp.setNombre( rs.getString(2) );
                              
                              return temp;
                      });

          }  catch (BadSqlGrammarException e) {
              System.out.println("Error: BancoDao en obtainMenuXUser, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
              lstTemp = new ArrayList<ChBanco>();
              this.jdbcTemplate = null;
          }
    	  
    	return lstTemp ;
    }
    

    
}
