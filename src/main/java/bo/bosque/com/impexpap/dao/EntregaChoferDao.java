package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.EntregaChofer;
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
public class EntregaChoferDao implements IEntregaChofer {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Registrar entrega de chofer
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarEntregaChofer(EntregaChofer mb, String acc) {

        System.out.println("***************************************");
        System.out.println(mb.toString());

        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_trch_Entregas  @idEntrega = ?, @docEntry = ?, @docNum = ?, @factura = ?, @docDate = ?, @docTime  =? , @cardCode = ?, @cardName = ?, @addressEntregaFac = ?, @addressEntregaMat = ?, @vendedor = ?, @u_Chofer = ?, @itemCode = ?, @dscription = ?, @whsCode = ?, @quantity = ?, @openQty = ?, @db = ?, @valido = ?, @fueEntregado = ?, @fechaEntrega = ?, @latitud =? , @longitud = ?, @direccionEntrega = ?, @obs = ?, @audUsuario = ?, @ACCION = ?",
                    ps ->{
                        ps.setInt(1,mb.getIdEntrega());
                        ps.setInt(2,mb.getDocEntry());
                        ps.setInt(3,mb.getDocNum());
                        ps.setInt(4,mb.getFactura());
                        ps.setString(5,  mb.getDocDate());
                        ps.setString(6, mb.getDocTime());
                        ps.setString(7, mb.getCardCode());
                        ps.setString(8, mb.getCardName());
                        ps.setString(9, mb.getAddressEntregaFac());
                        ps.setString(10, mb.getAddressEntregaMat());
                        ps.setString(11, mb.getVendedor());
                        ps.setInt(12, mb.getUChofer());
                        ps.setString(13, mb.getItemCode());
                        ps.setString(14, mb.getDscription());
                        ps.setString(15, mb.getWhsCode());
                        ps.setFloat(16, mb.getQuantity());
                        ps.setFloat(17, mb.getOpenQty());
                        ps.setString(18, mb.getDb());
                        ps.setString(19, mb.getValido());
                        ps.setInt(20, mb.getFueEntregado());
                        ps.setString(21,  mb.getFechaEntrega());
                        ps.setFloat(22, mb.getLatitud());
                        ps.setFloat(23, mb.getLongitud());
                        ps.setString(24, mb.getDireccionEntrega());
                        ps.setString(25, mb.getObs());
                        ps.setInt(26, mb.getAudUsuario());
                        ps.setString(27, acc);
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: EntregaChoferDao en registrarEntregaChofer, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }

    /**
     * Listar entregas por empleado
     *
     * @param codEmpleado
     * @return
     */
    @Override
    public List<EntregaChofer> listarEntregasXEmpleado( int codEmpleado ) {

        List<EntregaChofer> lstTemp = new ArrayList<>();

        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_trch_Entregas @u_Chofer=?, @ACCION = ?",
                    new Object[] { codEmpleado, "A" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        EntregaChofer temp = new EntregaChofer();

                        temp.setIdEntrega( rs.getInt(1 ) );
                        temp.setDocEntry( rs.getInt(2 ) );
                        temp.setDocNum( rs.getInt(3 ) );
                        temp.setFactura( rs.getInt(4 ) );
                        temp.setDocDate( rs.getString(5 ) );
                        temp.setDocTime( rs.getString(6 ) );
                        temp.setCardCode( rs.getString(7 ) );
                        temp.setCardName( rs.getString(8 ) );
                        temp.setAddressEntregaFac( rs.getString(9 ) );
                        temp.setAddressEntregaMat( rs.getString(10 ) );
                        temp.setVendedor( rs.getString(11 ) );
                        temp.setUChofer( rs.getInt(12 ) );
                        temp.setItemCode( rs.getString(13 ) );
                        temp.setDscription( rs.getString(14 ) );
                        temp.setWhsCode( rs.getString(15 ) );
                        temp.setQuantity( rs.getInt(16 ) );
                        temp.setOpenQty( rs.getInt(17 ) );
                        temp.setDb( rs.getString(18 ) );
                        temp.setValido( rs.getString(19 ) );
                        temp.setFueEntregado( rs.getInt(20 ) );
                        temp.setFechaEntrega( rs.getString(21 ) );
                        temp.setLatitud( rs.getFloat(22 ) );
                        temp.setLongitud( rs.getFloat(23 ) );
                        temp.setDireccionEntrega( rs.getString(24 ) );
                        temp.setObs( rs.getString(25 ) );
                        temp.setTipo( rs.getString(26 ) );;
                        temp.setAudUsuario( rs.getInt(27 ) );


                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EntregaChoferDao en listarEntregasXEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<EntregaChofer>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Listar entregas por chofer
     *
     *
     */
    @Override
    public List<EntregaChofer> listarEntregasXChofer(String fechaEntrega, int codEmpleado ) {
        List<EntregaChofer> lstTemp = new ArrayList<>();

        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_trch_Entregas @fechaEntrega=?, @u_Chofer=? ,@ACCION = ?",
                    new Object[] { fechaEntrega, codEmpleado  ,"B" },
                    new int[] { Types.VARCHAR, Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        EntregaChofer temp = new EntregaChofer();

                        temp.setDocEntry( rs.getInt(1 ) );
                        temp.setFactura( rs.getInt(2 ) );
                        temp.setCardCode( rs.getString(3 ) );
                        temp.setCardName( rs.getString(4 ) );
                        temp.setFechaNota( rs.getString(5 ) );
                        temp.setFechaEntrega( rs.getString(6 ) );
                        temp.setDiferenciaMinutos( rs.getInt(7 ) );
                        temp.setDireccionEntrega( rs.getString(8 ) );
                        temp.setAddressEntregaFac( rs.getString(9 ) );
                        temp.setAddressEntregaMat( rs.getString(10 ) );
                        temp.setVendedor( rs.getString(11 ) );
                        temp.setDb( rs.getString(12 ) );
                        temp.setUChofer( rs.getInt(13 ) );
                        temp.setNombreCompleto( rs.getString(14 ) );
                        temp.setLatitud(rs.getFloat(15));
                        temp.setLongitud(rs.getFloat(16));
                        temp.setObs( rs.getString(17 ) );
                        temp.setDocNumF(rs.getInt(18 ) );
                        temp.setPeso( rs.getFloat(19 ) );
                        temp.setCochePlaca( rs.getString(20 ) );
                        temp.setPrioridad( rs.getString(21 ) );
                        temp.setTipo( rs.getString(22 ) );


                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EntregaChoferDao en listarEntregasXChofer, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<EntregaChofer>();
            this.jdbcTemplate = null;
        }
        return lstTemp;

    }


    /**
     * Listara a todos los choferes
     *
     */
    @Override
    public List<EntregaChofer> lstChoferes() {
        List<EntregaChofer> lstTemp = new ArrayList<>();

        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_trch_Entregas  @ACCION = ?",
                    new Object[] {  "D" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        EntregaChofer temp = new EntregaChofer();

                        temp.setCodEmpleado( rs.getInt(1 ) );
                        temp.setNombreCompleto( rs.getString(2 ) );
                        temp.setCargo( rs.getString(3 ) );
                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EntregaChoferDao en lstChoferes, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<EntregaChofer>();
            this.jdbcTemplate = null;
        }
        return lstTemp;

    }

}
