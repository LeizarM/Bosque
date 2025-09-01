package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Ciudad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CiudadDao implements  ICiudad{

    /**
     * El Datasource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento para obtener las ciudades por pais
     * @param codPais
     * @return
     */
    public List<Ciudad> obtenerCiudadesXPais( int codPais ) {
        List<Ciudad> lstTemp = new ArrayList<>();

        try{
            lstTemp =  this.jdbcTemplate.query("execute p_list_Ciudad @codPais=?, @ACCION=?",
                    new Object[]{ codPais, "L"},
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) -> {
                        Ciudad temp = new Ciudad();
                        temp.setCodCiudad( rs.getInt(1));
                        temp.setCodPais( rs.getInt(2) );
                        temp.setCiudad( rs.getString(3) );
                        return temp;
                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: CiudadDao en obtenerCiudadesXPais, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
    /**
     * Procedimiento para registrar ciudad
     * @param
     * @param
     * @return
     */
    public boolean registrarCiudad(Ciudad ciudad, String acc ){

        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_Ciudad @codCiudad=?, @codPais=?, @ciudad=?, @audUsuarioI=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, ciudad.getCodCiudad() );
                        ps.setInt(2, ciudad.getCodPais() );
                        ps.setString(3, ciudad.getCiudad() );
                        ps.setInt(4, ciudad.getAudUsuario() );
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
