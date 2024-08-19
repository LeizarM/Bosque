package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Empresa;
import bo.bosque.com.impexpap.model.RegistroFacturas;
import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Repository
public class RegistroFacturasDao implements  IRegistroFacturas {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Para obtener los as factura de IPX y ESPP
     *
     * @param fecha
     * @return
     */
    @Override
    public List<RegistroFacturas> obtenerFacturas(Date fecha) {


        return null;


    }

    /**
     * Para registrar un nuevo registro de factura
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarRegistroFacturas(RegistroFacturas mb, String acc) {
        int resp;

        try{
            resp =  this.jdbcTemplate.update("execute p_abm_tfsc_Factura @idFac=?, @idTF=?, @codEmpresa=?, @fecha=?, @numFact=?, @proveedor=?, @nit=?,  @monto=?, @descripcion=?, @cuf=?, @nroAutorizacion=?, @codControl=?, @nitEmpresa=?, @fechaSistema=?  ,@audUsuario=?, @ACCION=?",
                    ps ->{
                        ps.setEscapeProcessing(true);
                        ps.setQueryTimeout(180);
                        ps.setInt(1, mb.getIdFac());
                        ps.setInt(2, mb.getIdTf());
                        ps.setInt(3, mb.getCodEmpresa() );
                        ps.setDate(4, (java.sql.Date) mb.getFecha());
                        ps.setInt(5, mb.getNumFact() );
                        ps.setString(6, mb.getProveedor() );
                        ps.setString(7, mb.getNit() );
                        ps.setFloat(8, mb.getMonto() );
                        ps.setString(9, mb.getDescripcion() );
                        ps.setString(10, mb.getCuf() );
                        ps.setString(11, mb.getNroAutorizacion() );
                        ps.setString(12, mb.getCodControl() );
                        ps.setString(13, mb.getNitEmpresa() );
                        ps.setDate(14, (java.sql.Date) mb.getFechaSistema() );
                        ps.setInt(15, mb.getAudUsuario() );
                        ps.setString(16, acc);
                    });
        }catch( BadSqlGrammarException e){
            System.out.println("Error: RegistroFacturasDao en registrarRegistroFacturas, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }



    /**
     * Obtendra una lista de tipos de factura
     * @return
     */
    public List<Tipos> lstTipoFactura() {
        return new Tipos().lstTipoFactura();
    }

    /**
     * Listara las empresas para el registro de facturas
     * @return
     */
    @Override
    public List<RegistroFacturas> lstEmpresas() {
        List<RegistroFacturas> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tfsc_Factura @ACCION = ?",
                    new Object[]{ "A" },
                    new int[]{ Types.VARCHAR },
                    (rs, rowCount) ->{
                        RegistroFacturas temp = new RegistroFacturas();
                        temp.setCodEmpresa(rs.getInt(1));
                        temp.setNombreEmpresa(rs.getString(2));
                        temp.setNitEmpresa(rs.getString(3));
                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: RegistroFacturasDao en lstEmpresas, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Listara las facturas registradas para una fecha especifica
     *
     * @param fechaSistema
     * @return
     */
    @Override
    public List<RegistroFacturas> lstFacturasRegistradas( Date fechaSistema ) {
        List<RegistroFacturas> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tfsc_Factura @fechaSistema=?, @ACCION = ?",
                    new Object[]{ fechaSistema, "B" },
                    new int[]{ Types.DATE, Types.VARCHAR },
                    (rs, rowCount) ->{
                        RegistroFacturas temp = new RegistroFacturas();

                        temp.setIdFac( rs.getInt(1) );
                        temp.setDescripcionTf(rs.getString(2));
                        temp.setNombreEmpresa(rs.getString(3));
                        temp.setFecha(rs.getDate(4));
                        temp.setNumFact(rs.getInt(5));
                        temp.setProveedor(rs.getString(6));
                        temp.setNit(rs.getString(7));
                        temp.setMonto(rs.getFloat(8));
                        temp.setDescripcion(rs.getString(9));
                        temp.setCuf(rs.getString(10));
                        temp.setNroAutorizacion(rs.getString(11));
                        temp.setCodControl(rs.getString(12));
                        temp.setNitEmpresa(rs.getString(13));
                        temp.setDireccion(rs.getString(14));
                        temp.setQrCadena(rs.getString(15));


                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: RegistroFacturasDao en lstFacturasRegistradas, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
