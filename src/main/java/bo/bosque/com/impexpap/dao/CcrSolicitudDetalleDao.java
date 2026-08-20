package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CcrSolicitudDetalle;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CcrSolicitudDetalleDao implements ICcrSolicitudDetalle {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Los items de una solicitud, con lo que devolvio SAP.
     *
     * @param idSolicitud
     * @return
     */
    @Override
    public List<CcrSolicitudDetalle> obtenerDetalleXSolicitud( long idSolicitud ) {

        List<CcrSolicitudDetalle> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_CcrSolicitudDetalle @ACCION=?, @idSolicitud=?",
                    new Object[] { "A", idSolicitud },
                    new int[] { Types.VARCHAR, Types.BIGINT },
                    (rs, rowCount)->{

                        CcrSolicitudDetalle t = new CcrSolicitudDetalle();

                        t.setIdSolicitudDetalle(rs.getLong(1));
                        t.setIdSolicitud(rs.getLong(2));

                        t.setCodigoSAPBase(rs.getString(3));
                        t.setDatoSAPBase(rs.getString(4));
                        t.setStockDisponibleSAPBase(rs.getDouble(5));
                        t.setCodTipoItemSAPBase(rs.getInt(6));
                        t.setDatoTipoItemSAPBase(rs.getString(7));
                        t.setCodFabricanteSAPBase(rs.getInt(8));
                        t.setDatoFabricanteSAPBase(rs.getString(9));
                        t.setGramajeSAPBase(rs.getDouble(10));
                        t.setLargoSAPBase(rs.getDouble(11));
                        t.setAnchoSAPBase(rs.getDouble(12));
                        t.setUtmSAPBase(rs.getDouble(13));
                        t.setEmpaqueSAPBase(rs.getString(14));

                        t.setCodigoSAPSalida(rs.getString(15));
                        t.setDatoSAPSalida(rs.getString(16));
                        t.setCodTipoItemSAPSalida(rs.getInt(17));
                        t.setDatoTipoItemSAPSalida(rs.getString(18));
                        t.setCodFabricanteSAPSalida(rs.getInt(19));
                        t.setDatoFabricanteSAPSalida(rs.getString(20));
                        t.setGramajeSAPSalida(rs.getDouble(21));
                        t.setLargoSAPSalida(rs.getDouble(22));
                        t.setAnchoSAPSalida(rs.getDouble(23));
                        t.setUtmSAPSalida(rs.getDouble(24));
                        t.setCantHojasSAPSalida(rs.getDouble(25));

                        t.setCantPaquetesSolicitados(rs.getDouble(26));
                        t.setCantToneladasSolicitados(rs.getDouble(27));
                        t.setEmpaqueSAPSalida(rs.getString(28));
                        t.setFechaEntrega(rs.getDate(29));

                        t.setAnchoSalidaEsp(rs.getDouble(30));
                        t.setLargoSalidaEsp(rs.getDouble(31));
                        t.setCantHojasSalidaEsp(rs.getInt(32));
                        t.setNroCortes(rs.getInt(33));

                        t.setSapDocEntry(rs.getLong(34));
                        t.setSapDocNum(rs.getLong(35));
                        t.setSapItemCode(rs.getString(36));
                        t.setSapProdName(rs.getString(37));
                        t.setSapEstado(rs.getString(38));
                        t.setSapPlannedQty(rs.getDouble(39));
                        t.setSapComments(rs.getString(40));

                        t.setDatoFecCierreSistem(rs.getString(41));
                        t.setDatoFecCierreStr(rs.getString(42));
                        t.setDatoFecInicioStr(rs.getString(43));
                        t.setSapTipoCorte(rs.getString(44));
                        t.setSapU_nroCorte(rs.getLong(45));
                        t.setSapCodEmpresa(rs.getInt(46));
                        t.setSapDatoEmpresa(rs.getString(47));
                        t.setDatoFechaEntregaStr(rs.getString(48));

                        return t;

                    });
        }catch ( DataAccessException e ){
            System.out.println("Error: CcrSolicitudDetalleDao en obtenerDetalleXSolicitud ->" + e.getMessage());
            lstTemp = new ArrayList<>();
        }
        return lstTemp;
    }


    /**
     * Registra un item de la solicitud.
     *
     * Los campos sap* no se mandan: los llena SAP cuando toma la solicitud, no
     * quien la carga.
     *
     * @param d
     * @return
     */
    @Override
    public boolean registrarDetalle( CcrSolicitudDetalle d ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update(
                    "execute p_abm_CcrSolicitudDetalle @idSolicitudDetalle=?, @idSolicitud=?" +
                    ", @codigoSAPBase=?, @datoSAPBase=?, @stockDisponibleSAPBase=?, @codTipoItemSAPBase=?" +
                    ", @datoTipoItemSAPBase=?, @codFabricanteSAPBase=?, @datoFabricanteSAPBase=?" +
                    ", @gramajeSAPBase=?, @largoSAPBase=?, @anchoSAPBase=?, @utmSAPBase=?, @empaqueSAPBase=?" +
                    ", @codigoSAPSalida=?, @datoSAPSalida=?, @codTipoItemSAPSalida=?, @datoTipoItemSAPSalida=?" +
                    ", @codFabricanteSAPSalida=?, @datoFabricanteSAPSalida=?, @gramajeSAPSalida=?" +
                    ", @largoSAPSalida=?, @anchoSAPSalida=?, @utmSAPSalida=?, @cantHojasSAPSalida=?" +
                    ", @cantPaquetesSolicitados=?, @cantToneladasSolicitados=?, @empaqueSAPSalida=?" +
                    ", @fechaEntrega=?, @anchoSalidaEsp=?, @largoSalidaEsp=?, @cantHojasSalidaEsp=?" +
                    ", @nroCortes=?, @audUsuario=?, @ACCION=?",
                    ps -> {
                        Utiles utiles = new Utiles();

                        ps.setEscapeProcessing( true );
                        ps.setLong(1, d.getIdSolicitudDetalle());
                        ps.setLong(2, d.getIdSolicitud());

                        ps.setString(3, d.getCodigoSAPBase());
                        ps.setString(4, d.getDatoSAPBase());
                        ps.setDouble(5, d.getStockDisponibleSAPBase());
                        ps.setInt(6, d.getCodTipoItemSAPBase());
                        ps.setString(7, d.getDatoTipoItemSAPBase());
                        ps.setInt(8, d.getCodFabricanteSAPBase());
                        ps.setString(9, d.getDatoFabricanteSAPBase());
                        ps.setDouble(10, d.getGramajeSAPBase());
                        ps.setDouble(11, d.getLargoSAPBase());
                        ps.setDouble(12, d.getAnchoSAPBase());
                        ps.setDouble(13, d.getUtmSAPBase());
                        ps.setString(14, d.getEmpaqueSAPBase());

                        ps.setString(15, d.getCodigoSAPSalida());
                        ps.setString(16, d.getDatoSAPSalida());
                        ps.setInt(17, d.getCodTipoItemSAPSalida());
                        ps.setString(18, d.getDatoTipoItemSAPSalida());
                        ps.setInt(19, d.getCodFabricanteSAPSalida());
                        ps.setString(20, d.getDatoFabricanteSAPSalida());
                        ps.setDouble(21, d.getGramajeSAPSalida());
                        ps.setDouble(22, d.getLargoSAPSalida());
                        ps.setDouble(23, d.getAnchoSAPSalida());
                        ps.setDouble(24, d.getUtmSAPSalida());
                        ps.setDouble(25, d.getCantHojasSAPSalida());

                        ps.setDouble(26, d.getCantPaquetesSolicitados());
                        ps.setDouble(27, d.getCantToneladasSolicitados());
                        ps.setString(28, d.getEmpaqueSAPSalida());
                        ps.setDate(29, utiles.fechaJ_a_Sql(d.getFechaEntrega()));

                        ps.setDouble(30, d.getAnchoSalidaEsp());
                        ps.setDouble(31, d.getLargoSalidaEsp());
                        ps.setInt(32, d.getCantHojasSalidaEsp());
                        ps.setInt(33, d.getNroCortes());

                        ps.setLong(34, d.getAudUsuario());
                        ps.setString(35, d.getIdSolicitudDetalle() == 0 ? "I" : "U");
                    });

        }catch ( DataAccessException e ){
            System.out.println("Error: CcrSolicitudDetalleDao en registrarDetalle ->" + e.getMessage());
            resp = 0;
        }

        return resp != 0;
    }


    /**
     * Borra todos los items de una solicitud.
     *
     * @param idSolicitud
     * @return
     */
    @Override
    public boolean eliminarDetalleXSolicitud( long idSolicitud ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_CcrSolicitudDetalle @idSolicitud=?, @ACCION=?",
                    ps -> {
                        ps.setEscapeProcessing( true );
                        ps.setLong(1, idSolicitud);
                        ps.setString(2, "E");
                    });
        }catch ( DataAccessException e ){
            System.out.println("Error: CcrSolicitudDetalleDao en eliminarDetalleXSolicitud ->" + e.getMessage());
            resp = 0;
        }

        return resp != 0;
    }
}
