package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.PrestamoChofer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class PretamoChoferDao implements IPrestamoChofer {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Metodo para registrar un prestamo de chofer
     * @param mb
     * @param acc
     * @return
     */
    public boolean registrarPrestamoChofer( PrestamoChofer mb, String acc ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tpre_prestamo @idPrestamo =?, @idCoche  =?, @idSolicitud  =?, @codSucursal  =?, @fechaEntrega  =?, @codEmpEntregadoPor  =?, @kilometrajeEntrega =?, @kilometrajeRecepcion =?, @nivelCombustibleEntrega  =?, @nivelCombustibleRecepcion  =?, @estadoLateralesEntrega  =?, @estadoInteriorEntrega  =?, @estadoDelanteraEntrega  =?, @estadoTraseraEntrega  =?, @estadoCapoteEntrega  =?, @estadoLateralRecepcion  =?, @estadoInteriorRecepcion  =?, @estadoDelanteraRecepcion  =?, @estadoTraseraRecepcion = ?, @estadoCapoteRecepcion =?, @audUsuario =?, @ACCION=? ",
                    ps -> {

                        ps.setEscapeProcessing( true );
                        ps.setInt(1, mb.getIdPrestamo());
                        ps.setInt(2, mb.getIdCoche());
                        ps.setInt(3, mb.getIdSolicitud());
                        ps.setInt(4, mb.getCodSucursal());
                        ps.setDate(5, (Date) mb.getFechaEntrega());
                        ps.setInt(6, mb.getCodEmpEntregadoPor());
                        ps.setFloat(7, mb.getKilometrajeEntrega());
                        ps.setFloat(8, mb.getKilometrajeRecepcion());
                        ps.setInt(9, mb.getNivelCombustibleEntrega());
                        ps.setInt(10, mb.getNivelCombustibleRecepcion());
                        ps.setInt(11, mb.getEstadoLateralesEntrega());
                        ps.setInt(12, mb.getEstadoInteriorEntrega());
                        ps.setInt(13, mb.getEstadoDelanteraEntrega());
                        ps.setInt(14, mb.getEstadoTraseraEntrega());
                        ps.setInt(15, mb.getEstadoCapoteEntrega());
                        ps.setInt(16, mb.getEstadoLateralRecepcion());
                        ps.setInt(17, mb.getEstadoInteriorRecepcion());
                        ps.setInt(18, mb.getEstadoDelanteraRecepcion());
                        ps.setInt(19, mb.getEstadoTraseraRecepcion());
                        ps.setInt(20, mb.getEstadoCapoteRecepcion());
                        ps.setInt(21, mb.getAudUsuario());
                        ps.setString(22, acc);

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: PretamoChoferDao en registrarPrestamoChofer, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }

    /**
     * Metodo para listar las solicitudes de prestamo de chofer
     * @param codSucursal
     * @param codEmpleado
     * @return
     */
    public List<PrestamoChofer> lstSolicitudes( int codSucursal, int codEmpleado ) {


        List<PrestamoChofer> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tpre_prestamo  @codSucursal=?, @codEmpEntregadoPor=? ,@ACCION = ?",
                    new Object[]{ codSucursal, codEmpleado ,"A" },
                    new int[]{ Types.INTEGER, Types.INTEGER ,Types.VARCHAR },
                    (rs, rowCount) ->{
                        PrestamoChofer temp = new PrestamoChofer();

                        temp.setIdPrestamo( rs.getInt(1)  );
                        temp.setIdSolicitud( rs.getInt(2) );
                        temp.setFechaSolicitud(rs.getDate(3) );
                        temp.setMotivo( rs.getString(4) );
                        temp.setSolicitante( rs.getString(5) );
                        temp.setCargo( rs.getString(6) );
                        temp.setCoche( rs.getString(7) );
                        temp.setEstadoDisponibilidad( rs.getString(8) );
                        temp.setIdCoche( rs.getInt(9) );

                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: PretamoChoferDao en lstSolicitudes, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;


    }
}
