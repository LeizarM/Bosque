package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CcrSolicitud;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CcrSolicitudDao implements ICcrSolicitud {

    /** Los 17 parametros de p_abm_CcrSolicitud, el ultimo de salida. */
    private static final String LLAMADA_ABM =
            "{CALL dbo.p_abm_CcrSolicitud (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Solicitudes de corte de un rango de fechas.
     * Con ambas fechas en null el SP devuelve todas.
     *
     * @param fechaIni
     * @param fechaFin
     * @return
     */
    @Override
    public List<CcrSolicitud> obtenerSolicitudes( java.util.Date fechaIni, java.util.Date fechaFin ) {

        List<CcrSolicitud> lstTemp = new ArrayList<>();

        Utiles utiles = new Utiles();
        Date desde = utiles.fechaJ_a_Sql( fechaIni );
        Date hasta = utiles.fechaJ_a_Sql( fechaFin );

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_CcrSolicitud @fechaIni=?, @fechaFin=?, @ACCION=?",
                    new Object[] { desde, hasta, "A" },
                    new int[] { Types.DATE, Types.DATE, Types.VARCHAR },
                    (rs, rowCount)->{

                        CcrSolicitud temp = new CcrSolicitud();

                        temp.setIdSolicitud(rs.getLong(1));
                        temp.setCodEmpresa(rs.getLong(2));
                        temp.setNumeracion(rs.getInt(3));
                        temp.setTipoSolicitud(rs.getString(4));
                        temp.setFechaSistema(rs.getTimestamp(5));
                        temp.setFechaSolicitud(rs.getDate(6));
                        temp.setIdSolicitante(rs.getLong(7));
                        temp.setDatoSolicitante(rs.getString(8));
                        temp.setEstado(rs.getString(9));
                        temp.setObservacion(rs.getString(10));
                        temp.setTotalToneladas(rs.getDouble(11));
                        temp.setSapObservacion(rs.getString(12));
                        temp.setSapToneladas(rs.getDouble(13));
                        temp.setDatoNroSolicitud(rs.getString(14));
                        temp.setDatoEmpresa(rs.getString(15));
                        temp.setDatoEstado(rs.getString(16));
                        temp.setFechaSistemaString(rs.getString(17));
                        temp.setFechaSolicitudString(rs.getString(18));
                        temp.setDatoTipoSolicitud(rs.getString(19));

                        return temp;

                    });
        }catch ( DataAccessException e ){
            System.out.println("Error: CcrSolicitudDao en obtenerSolicitudes ->" + e.getMessage());
            lstTemp = new ArrayList<>();
        }
        return lstTemp;
    }


    /**
     * Registra la cabecera y devuelve el idSolicitud generado.
     *
     * El SP devuelve el id por el parametro de salida @RETORNA, no por un
     * SELECT, asi que hace falta un CallableStatement: con jdbcTemplate.update
     * el id se pierde y el detalle no tendria a que colgarse.
     *
     * La numeracion la calcula el SP —correlativo por empresa, tipo y anio—,
     * asi que lo que se mande aqui en ese campo se ignora.
     *
     * @param mb
     * @return el id generado, o 0 si fallo
     */
    @Override
    public long registrarSolicitud( CcrSolicitud mb ) {

        final Utiles utiles = new Utiles();

        CallableStatementCreator creador = con -> {
            CallableStatement c = con.prepareCall( LLAMADA_ABM );
            c.setEscapeProcessing( true );
            c.setQueryTimeout( 180 );
            return c;
        };

        CallableStatementCallback<Long> accion = c -> {
            c.setLong(1, mb.getIdSolicitud());
            c.setLong(2, mb.getCodEmpresa());
            c.setInt(3, mb.getNumeracion());
            c.setString(4, mb.getTipoSolicitud());
            c.setDate(5, utiles.fechaJ_a_Sql(mb.getFechaSistema()));
            c.setDate(6, utiles.fechaJ_a_Sql(mb.getFechaSolicitud()));
            c.setLong(7, mb.getIdSolicitante());
            c.setString(8, mb.getDatoSolicitante());
            c.setString(9, mb.getEstado());
            c.setString(10, mb.getObservacion());
            c.setDouble(11, mb.getTotalToneladas());
            c.setString(12, mb.getSapObservacion());
            c.setDouble(13, mb.getSapToneladas());
            c.setLong(14, mb.getAudUsuario());
            c.setNull(15, Types.TIMESTAMP);
            c.setString(16, "I");
            c.registerOutParameter(17, Types.INTEGER);
            c.execute();
            return (long) c.getInt(17);
        };

        try {
            Long resp = this.jdbcTemplate.execute( creador, accion );
            return resp == null ? 0L : resp;
        } catch ( DataAccessException e ) {
            System.out.println("Error: CcrSolicitudDao en registrarSolicitud ->" + e.getMessage());
            return 0L;
        }
    }


    /**
     * Cancela una solicitud. El SP fuerza estado 'CNC' y guarda el motivo.
     *
     * @param mb con idSolicitud, observacion y audUsuario
     * @return
     */
    @Override
    public boolean cancelarSolicitud( CcrSolicitud mb ) {
        return this.ejecutarAbm( mb.getIdSolicitud(), mb.getObservacion(), mb.getAudUsuario(), "B" );
    }


    /**
     * Borra la cabecera. Solo para deshacer un alta que quedo a medias.
     *
     * @param idSolicitud
     * @return
     */
    @Override
    public boolean eliminarSolicitud( long idSolicitud ) {
        return this.ejecutarAbm( idSolicitud, null, 0L, "D" );
    }


    /**
     * Las acciones que solo necesitan el id: cancelar y borrar.
     *
     * El resto de los parametros van en null con su tipo declarado; el SP los
     * ignora en estas dos ramas, pero un CallableStatement exige que los 17
     * esten seteados.
     */
    private boolean ejecutarAbm( long idSolicitud, String observacion, long audUsuario, String accion ) {

        CallableStatementCreator creador = con -> {
            CallableStatement c = con.prepareCall( LLAMADA_ABM );
            c.setEscapeProcessing( true );
            return c;
        };

        CallableStatementCallback<Long> cuerpo = c -> {
            c.setLong(1, idSolicitud);
            c.setNull(2, Types.BIGINT);      // codEmpresa
            c.setNull(3, Types.INTEGER);     // numeracion
            c.setNull(4, Types.VARCHAR);     // tipoSolicitud
            c.setNull(5, Types.TIMESTAMP);   // fechaSistema
            c.setNull(6, Types.DATE);        // fechaSolicitud
            c.setNull(7, Types.BIGINT);      // idSolicitante
            c.setNull(8, Types.VARCHAR);     // datoSolicitante
            c.setNull(9, Types.VARCHAR);     // estado
            c.setString(10, observacion);
            c.setNull(11, Types.NUMERIC);    // totalToneladas
            c.setNull(12, Types.VARCHAR);    // sapObservacion
            c.setNull(13, Types.DECIMAL);    // sapToneladas
            c.setLong(14, audUsuario);
            c.setNull(15, Types.TIMESTAMP);  // audFecha
            c.setString(16, accion);
            c.registerOutParameter(17, Types.INTEGER);
            c.execute();
            return (long) c.getInt(17);
        };

        try {
            Long resp = this.jdbcTemplate.execute( creador, cuerpo );
            return resp != null && resp > 0;
        } catch ( DataAccessException e ) {
            System.out.println("Error: CcrSolicitudDao en ejecutarAbm(" + accion + ") ->" + e.getMessage());
            return false;
        }
    }
}
