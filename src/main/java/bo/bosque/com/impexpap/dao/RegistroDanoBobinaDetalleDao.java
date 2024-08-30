package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RegistroDanoBobinaDetalle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

@Repository
public class RegistroDanoBobinaDetalleDao implements  IRegistroDanoBobinaDetalle {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Registra el detalle de un registro de daño de bobina
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarRegistroDanoBobinaDetalle(RegistroDanoBobinaDetalle mb, String acc) {

        int resp;

        try{
            resp =  this.jdbcTemplate.update("execute p_abm_tmme_RegistroDanoBobinaDetalle @idRegDet=?, @idReg=?, @idTd=?, @codArticulo=?, @descripcion=?, @pesoBobina=?, @diametro=?, @cmDanado=?, @ancho=?,  @cca=?, @ccb=?, @kilosDanadosReal=?, @ccc=?, @cma=?, @cmb=?, @kilosDanados=?, @precioUnitario=?, @subTotalUSD=?, @placa=?, @chofer=?, @numImportacion=?   ,@audUsuario=?, @ACCION=?",
                    ps ->{
                        ps.setEscapeProcessing(true);
                        ps.setQueryTimeout(180);
                        ps.setInt(1, mb.getIdRegDet());
                        ps.setInt(2, mb.getIdReg());
                        ps.setInt(3, mb.getIdTd());
                        ps.setString(4, mb.getCodArticulo());
                        ps.setString(5, mb.getDescripcion());
                        ps.setFloat(6, mb.getPesoBobina());
                        ps.setFloat(7, mb.getDiametro());
                        ps.setFloat(8, mb.getCmDanado());
                        ps.setFloat(9, mb.getAncho());
                        ps.setFloat(10, mb.getCca());
                        ps.setFloat(11, mb.getCcb());
                        ps.setFloat(12, mb.getKilosDanadosReal());
                        ps.setFloat(13, mb.getCcc());
                        ps.setFloat(14, mb.getCma());
                        ps.setFloat(15, mb.getCmb());
                        ps.setFloat(16, mb.getKilosDanados());
                        ps.setFloat(17, mb.getPrecioUnitario());
                        ps.setFloat(18, mb.getSubTotalUsd());
                        ps.setString(19, mb.getPlaca());
                        ps.setString(20, mb.getChofer());
                        ps.setString(21, mb.getNumImportacion());
                        ps.setInt(22, mb.getAudUsuario());
                        ps.setString(23, acc);


                    });
        }catch( BadSqlGrammarException e){
            System.out.println("Error: RegistroDanoBobinaDetalleDao en registrarRegistroDanoBobinaDetalle, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;


    }

    /**
     * Obtiene la lista de todos los detalles de registro de daño de bobina
     *
     * @return
     */
    @Override
    public List<RegistroDanoBobinaDetalle> lstRegistroDanoBobinaDetalle() {
        return null;
    }
}
