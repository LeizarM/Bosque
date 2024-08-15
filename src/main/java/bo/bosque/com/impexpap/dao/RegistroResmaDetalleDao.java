package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RegistroResmaDetalle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

@Repository
public class RegistroResmaDetalleDao implements IRegistroResmaDetalle {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Metodo para registrar un registro de resma detalle
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarRegistroResmaDetalle(RegistroResmaDetalle mb, String acc) {

        int resp;

        try{
            resp =  this.jdbcTemplate.update("execute p_abm_tmme_RegistroResmaDetalle @idRMD=?, @idMer=?, @idTd=?, @codArticulo=?, @descripcion=?, @cantidad=?, @porcentaje=?, @precioUnitario=?,  @subTotalUSD=?, @placa=?, @chofer=?  ,@audUsuario=?, @ACCION=?",
                    ps ->{
                        ps.setEscapeProcessing(true);
                        ps.setQueryTimeout(180);
                        ps.setInt(1, mb.getIdRMD() );
                        ps.setInt(2, mb.getIdMer() );
                        ps.setInt(3, mb.getIdTd() );
                        ps.setString(4, mb.getCodArticulo() );
                        ps.setString(5, mb.getDescripcion() );
                        ps.setInt(6, mb.getCantidad() );
                        ps.setFloat(7, mb.getPorcentaje() );
                        ps.setFloat(8, mb.getPrecioUnitario() );
                        ps.setFloat(9, mb.getSubtotalUsd() );
                        ps.setString(10, mb.getPlaca() );
                        ps.setString(11, mb.getChofer() );
                        ps.setInt(12, mb.getAudUsuario() );
                        ps.setString(13, acc);
                    });
        }catch( BadSqlGrammarException e){
            System.out.println("Error: RegistroResmaDetalleDao en registrarRegistroResmaDetalle, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }

    /**
     * Metodo para listar los registros de resma detalle
     *
     * @return
     */
    @Override
    public List<RegistroResmaDetalle> lstRegistroResmaDetalle() {
        return null;
    }
}
