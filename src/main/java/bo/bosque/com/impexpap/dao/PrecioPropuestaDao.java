package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.PrecioPropuesta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class PrecioPropuestaDao implements IPrecioPropuesta {


    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el precio propuesta
     *
     * @param precioPropuesta
     * @param acc
     * @return
     */
    @Override
    public boolean registrarPrecioPropuesta(PrecioPropuesta precioPropuesta, String acc) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_precioPropuesta @idPrePropuesto=?, @idPropuesta=?, @idPrecio=?, @codigoFamilia=?,@precioActual=?, @precioPropuesto=?, @porcentaje=?, @codSucursal=?, @listNum=?, @nombrePrecio=?, @vpp=? ,@audUsuario=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, precioPropuesta.getIdPrecioPropuesta());
                        ps.setInt(2, precioPropuesta.getIdPropuesta());
                        ps.setInt(3, precioPropuesta.getIdPrecio());
                        ps.setInt(4, precioPropuesta.getCodigoFamilia());
                        ps.setFloat(5, precioPropuesta.getPrecioActual());
                        ps.setFloat(6, precioPropuesta.getPrecioPropuesto());
                        ps.setFloat(7, precioPropuesta.getPorcentaje());
                        ps.setInt(8, precioPropuesta.getCodSucursal());
                        ps.setInt(9,precioPropuesta.getListNum());
                        ps.setString(10, precioPropuesta.getNombrePrecio());
                        ps.setInt(11, precioPropuesta.getVpp());
                        ps.setInt(12, precioPropuesta.getAudUsuario());
                        ps.setString(13, acc);
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: PrecioPropuestaDao en registrarPrecioPropuesta, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }
}
