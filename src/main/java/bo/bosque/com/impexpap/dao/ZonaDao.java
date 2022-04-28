package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Persona;
import bo.bosque.com.impexpap.model.Zona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ZonaDao implements  IZona {


    /**
     * El Datasource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para obtener la zona por ciudad
     * @param codCiudad
     * @return
     */
    public List<Zona> obtenerZonaXCiudad( int codCiudad ) {
        List<Zona> lstTemp = new ArrayList<>();
        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_Zona @codCiudad=?, @ACCION=?",
                    new Object[]{ codCiudad, "L" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{
                        Zona temp = new Zona();
                        temp.setCodZona(rs.getInt(1));
                        temp.setCodCiudad(rs.getInt(2));
                        temp.setZona(rs.getString(3));
                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: ZonaDao en obtenerZonaXCiudad, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
