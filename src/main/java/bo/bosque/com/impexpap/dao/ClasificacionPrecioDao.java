package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ClasificacionPrecio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class ClasificacionPrecioDao implements IClasificacionPrecio {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Para registrar el clasificacion precio
     *
     * @param clasificacionPrecio
     * @param acc
     * @return
     */
    @Override
    public boolean registrarClasificacionPrecio(ClasificacionPrecio clasificacionPrecio, String acc) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_clasificacionPrecio @idClasificacion=?, @idSucursal=?, @listNum=? ,@nombrePrecio=? , @vpp=?, @estado=?,@audUsuario=?,  @ACCION=?",
                   ps -> {
                        ps.setInt(1,clasificacionPrecio.getIdClasificacion());
                        ps.setInt(2,clasificacionPrecio.getCodSucursal());
                        ps.setInt(3, clasificacionPrecio.getListNum());
                        ps.setString(4, clasificacionPrecio.getNombrePrecio());
                        ps.setInt(5, clasificacionPrecio.getVpp());
                        ps.setInt(6, clasificacionPrecio.getEstado());
                        ps.setInt(7, clasificacionPrecio.getAudUsuario());
                        ps.setString(8, acc);
                   });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: ClasificacionPrecioDao en registrarClasificacionPrecio, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }
}
