package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SolicitudChofer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.ArrayList;
import java.sql.SQLException;
import java.util.List;


@Repository
public class SolicitudChoferDao implements ISolicitudChofer {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Registra una solicitud de chofer
     * @param mb
     * @param acc
     * @return
     */
    public boolean registrarSolicitudChofer( SolicitudChofer mb, String acc ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tpre_solicitud @idSolicitud=?, @fechaSolicitud=?, @motivo=?, @codEmpSoli=?, @cargo=?, @estado=?,@idCocheSol=?, @idES=?, @requiereChofer=? ,@audUsuario =?, @ACCION=? ",
                    ps -> {

                        ps.setEscapeProcessing( true );
                        ps.setInt(1, mb.getIdSolicitud());
                        ps.setDate(2, (java.sql.Date) mb.getFechaSolicitud());
                        ps.setString(3, mb.getMotivo());
                        ps.setInt(4, mb.getCodEmpSoli());
                        ps.setString(5, mb.getCargo());
                        ps.setInt(6, mb.getEstado());
                        ps.setInt(7, mb.getIdCocheSol());
                        ps.setInt(8, mb.getIdES());
                        ps.setInt(9, mb.getRequiereChofer());
                        ps.setInt(10, mb.getAudUsuario());
                        ps.setString(11, acc);

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: SolicitudChoferDao en registrarSolicitudChofer, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;



    }

    @Override
    public List<SolicitudChofer> lstSolicitudesXEmpleado( int codEmpleado ) {
        List<SolicitudChofer> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tpre_solicitud @codEmpSoli=?, @ACCION = ?",
                    new Object[]{ codEmpleado, "A" },
                    new int[]{ Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) ->{
                        SolicitudChofer temp = new SolicitudChofer();

                        temp.setIdSolicitud( rs.getInt(1) );
                        temp.setFechaSolicitudCad(rs.getString(2) );
                        temp.setMotivo( rs.getString(3) );
                        temp.setCargo( rs.getString(4) );
                        temp.setEstadoCad( rs.getString(5) );
                        temp.setIdCocheSol( rs.getInt(6) );

                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: SolicitudChoferDao en lstSolicitudesXEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    @Override
    public List<SolicitudChofer> lstCoches() {
        List<SolicitudChofer> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tpre_solicitud  @ACCION = ?",
                    new Object[]{  "B" },
                    new int[]{ Types.VARCHAR },
                    (rs, rowCount) ->{
                        SolicitudChofer temp = new SolicitudChofer();

                        temp.setIdCocheSol( rs.getInt(1) );
                        temp.setCodSucursal(rs.getInt(2) );
                        temp.setCoche( rs.getString(3) );


                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: SolicitudChoferDao en lstCoches, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
