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
                    new Object[]{ codCiudad>0?codCiudad:null, "L" },
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
    /**
     * Procedimiento para registrar zona
     * @param
     * @param
     * @return
     */
    public boolean registrarZona(Zona zona, String acc ){

        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_Zona @codZona=?, @codCiudad=?, @zona=?, @audUsuarioI=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, zona.getCodZona() );
                        ps.setInt(2, zona.getCodCiudad() );
                        ps.setString(3, zona.getZona() );
                        ps.setInt(4, zona.getAudUsuario() );
                        ps.setString(5, acc);
                        //ps.executeUpdate();
                    });


        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: PaisDao en registrarPais, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp != 0;
    }
}
