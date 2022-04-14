package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ChAccion;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.utils.Utiles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;


@Repository
public class ChAccionDAO  implements IChAccion {

    /******************
     * El Datasource
     *******************/
    @Autowired
    private JdbcTemplate jdbcTemplate;

    

    /**
     * Funcion q como su nombre lo indica son sus acciones a realizar 
     * @param mb
     * @param oper
     * @return Cero si NO hace lo que tiene que hacer
     */
//    public int abmAccion(AccionManagedBean mb, String oper) {
//
//        int resp = 0;
//        try {
//            con = conex.conectar();
//            ps = con.prepareStatement("execute dbo.p_abm_Accion ?,?,?,?,?,?,?,?,?");
//            ps.setEscapeProcessing(true);
//            ps.setQueryTimeout(180);
//            ps.setInt(1, mb.getCodAccion() );
//            ps.setInt(2, mb.getCodCheque());
//            //ps.setDate(3, mb.getFecha());
//            ps.setTimestamp(3, mb.getFecha());
//            ps.setString(4, mb.getEstado());
//            ps.setInt(5, mb.getCodEmpleado());
//            ps.setString(6, mb.getNroSAP());
//            ps.setString(7, mb.getObservacion());
//            ps.setInt(8, mb.getAudUsuarioI());
//            ps.setString(9, oper);
//            resp = ps.executeUpdate();
//        } catch (SQLException e) {
//            System.out.println("Error abmAccion insertando: " + e);
//            resp = 0;
//        } catch (Exception e) {
//            System.out.println("Error  abmAccion general: " + e);
//            e.printStackTrace();
//            resp = 0;
//        } finally {
//            try {
//                conex.desconectar(null);
//            } catch (Exception e) {
//                System.out.println("Error abmAccion cerrando: " + e);
//            }
//        }
//        return resp;
//    }

    
    /********
     * Funcion de devuelve el listado de accions de la DB
     * @param codCheque
     * @return LinkedList
     ********/
    public List<ChAccion> listAccions(int codCheque) {
        
    	List<ChAccion> lstTemp = new ArrayList<ChAccion>();
    	
    	  try {
              lstTemp =  this.jdbcTemplate.query("execute dbo.p_list_Accion  @ACCION=?, @codCheque=?",
                          new Object[] {  "L" , codCheque },
                          new int[] {  Types.VARCHAR , Types.INTEGER },
                          (rs, rowNum) -> {
                        	  ChAccion temp = new ChAccion();
                              temp.setNro( rowNum + 1 );
                              
                              temp.setCodAccion( rs.getInt( 1 ) );
                              temp.setCodCheque( rs.getInt( 2 ) );
                              temp.setFecha( rs.getTimestamp( 3 ) );
                              temp.setEstado( rs.getString( 4 ) );
                              temp.setCodEmpleado( rs.getInt( 5 ) );
                              temp.setNroSAP( rs.getString( 6 ) );
                              temp.setObservacion( rs.getString( 7 ) );
                              temp.setAudUsuarioI( rs.getInt( 8 ) );
                              temp.setAudFechaI( rs.getDate( 9 ) );
                              
                              temp.setFechaJ ( new Utiles().fechaSQLTIMESTAMP_a_fechaJ( rs.getTimestamp( 3 ) ) );
                              
                              return temp;
                      });

          }  catch (BadSqlGrammarException e) {
              System.out.println("Error: listAccions en obtainMenuXUser, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
              lstTemp = new ArrayList<ChAccion>();
              this.jdbcTemplate = null;
          }
    	
    	return lstTemp ;
    
    }
    
    
    /**
     * Funcion para busqueda
     * @param chequeBusc
     * @param fechaBusc
     * @param estadoBusc
     * @return List
     */
    public List <ChAccion> listAccions(int chequeBusc, Date fechaBusc, String estadoBusc  ) {
        
        List<ChAccion> lstTemp = new ArrayList<ChAccion>();
    	
  	  try {
  		  String chequeBuscString = "";
  		  String fechaBuscString = "";
  		  
//  		ps.setString(1, "A");
//        if( chequeBusc <= 0){
//            ps.setString(2, null);
//        }else{
//            ps.setInt(2, chequeBusc);    
//        }
//        if( fechaBusc== null || fechaBusc.toString().isEmpty() ){
//            ps.setString(3, null);
//        }else{
//            ps.setDate(3, fechaBusc);    
//        }
//        if( estadoBusc== null || estadoBusc.isEmpty() ){
//            ps.setString(4, null);
//        }else{
//            ps.setString(4, estadoBusc);    
//        }
  		  
	        if( chequeBusc <= 0){
	        	chequeBuscString = null;
	        }else{
	        	chequeBuscString = new Utiles().int_a_String( chequeBusc );
	        }
	        if( fechaBusc== null || fechaBusc.toString().isEmpty() ){
	        	fechaBuscString = null;
	        }else{
	        	fechaBuscString = new Utiles().fechaSQL_a_String(fechaBusc);
	        }
	        if( estadoBusc== null || estadoBusc.isEmpty() ){
	        	estadoBusc = null ;
	        }
  		  
            lstTemp =  this.jdbcTemplate.query("execute dbo.p_list_Accion   @ACCION=?, @codCheque=?, @fecha=?,  @estado=? ",
                        new Object[] {  "A" ,  chequeBuscString ,  fechaBuscString,  estadoBusc   },
                        new int[] {  Types.VARCHAR , Types.VARCHAR , Types.VARCHAR  , Types.VARCHAR  },
                        (rs, rowNum) -> {
                      	  ChAccion temp = new ChAccion();
                            temp.setNro( rowNum + 1 );
                      	/// ChAccion(      int nro, int codAccion, int codCheque, Timestamp fecha, String estado, String nombreEstado, int codEmpleado, String nroSAP, String observacion, int audUsuarioI, Date audFechaI)
                        //  AccionManagedBean( fila, rs.getInt(1),  rs.getInt(2), rs.getTimestamp(3), rs.getString(4), rs.getString(5),   rs.getInt(6), rs.getString(7), rs.getString(8), rs.getInt(9), rs.getDate(10));
                            
                            temp.setCodAccion( rs.getInt( 1 ) );
                            temp.setCodCheque( rs.getInt( 2 ) );
                            temp.setFecha( rs.getTimestamp( 3 ) );
                            temp.setEstado( rs.getString( 4 ) );
                            
                            temp.getTipoEstado().setCodTipos( rs.getString( 4 ) );
                            temp.getTipoEstado().setNombre( rs.getString( 5 )  );
                            
                            temp.setCodEmpleado( rs.getInt( 6 ) );
                            temp.setNroSAP( rs.getString( 7 ) );
                            temp.setObservacion( rs.getString( 8 ) );
                            temp.setAudUsuarioI( rs.getInt( 9 ) );
                            temp.setAudFechaI( rs.getDate( 10 ) );
                            
                            temp.setFechaJ ( new Utiles().fechaSQLTIMESTAMP_a_fechaJ( rs.getTimestamp( 3 ) ) );
                            
                            return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: listAccions en obtainMenuXUser, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<ChAccion>();
            this.jdbcTemplate = null;
        }
  	
    	return lstTemp ;
  	
    }
    
    
    /******************
     * Devolvera un registro dado
     * @param accionCodigo
     * @return Clase 
     ******************/
    public ChAccion registroAccion( int accionCodigo )  {
    	ChAccion accionTemp = new ChAccion();        
    	
        try {  
                  accionTemp =  this.jdbcTemplate.queryForObject("execute  p_list_Accion @ACCION=?, @codAccion=? ",
				                new Object[] {  "L" , accionCodigo },
		                          new int[] {  Types.VARCHAR , Types.INTEGER },
		                          (rs, rowNum) -> {
		                        	  ChAccion temp = new ChAccion();
		                              temp.setNro( rowNum + 1 );
		                              
		                              temp.setCodAccion( rs.getInt( 1 ) );
		                              temp.setCodCheque( rs.getInt( 2 ) );
		                              temp.setFecha( rs.getTimestamp( 3 ) );
		                              temp.setEstado( rs.getString( 4 ) );
		                              temp.setCodEmpleado( rs.getInt( 5 ) );
		                              temp.setNroSAP( rs.getString( 6 ) );
		                              temp.setObservacion( rs.getString( 7 ) );
		                              temp.setAudUsuarioI( rs.getInt( 8 ) );
		                              temp.setAudFechaI( rs.getDate( 9 ) );
		                              
		                              temp.setFechaJ ( new Utiles().fechaSQLTIMESTAMP_a_fechaJ( rs.getTimestamp( 3 ) ) );
		                              
		                              return temp;
		                      });
				
			}  catch (BadSqlGrammarException e) {
			    System.out.println("Error: registroAccion en obtainMenuXUser, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
			    accionTemp = new  ChAccion();
			    this.jdbcTemplate = null;
			}

        return accionTemp;
    }
    
    
//    /**
//     * Procedimietno para verificar que exite un registro solicitado
//     * @param chequeCodBusc
//     * @param fechaBusc
//     * @return 
//     */
//    public boolean existeRegistroAcc(int chequeCodBusc, Date fechaBusc) {
//        AccionManagedBean accionTemp = new AccionManagedBean();
//        boolean myFlag= false;
//        try {
//            con = conex.conectar();
//            ps = con.prepareStatement("execute p_list_Accion @ACCION=?, @codCheque=?, @fecha=?");
//            ps.setEscapeProcessing(true);
//            ps.setQueryTimeout(180);
//            ps.setString(1, "L");
//            ps.setInt(2, chequeCodBusc);
//            ps.setDate(3, fechaBusc);
//            rs = ps.executeQuery();
//            if (rs != null) {
//                while (rs.next()){
//                    
//                    accionTemp = new AccionManagedBean(rs.getInt(1), rs.getInt(2), rs.getTimestamp(3), rs.getString(4), rs.getInt(5), rs.getString(6), rs.getString(7), rs.getInt(8), rs.getDate(9));
//                }
//            }
//        }catch (SQLException e) {
//            System.out.println("Error  registroAccion recuperando: " + e);
//        } catch (Exception e) {
//            System.out.println("Error registroAccion general: " + e);
//            e.printStackTrace();
//        } finally {
//            try {
//                conex.desconectar(rs);
//            } catch (Exception e) {
//                System.out.println("Error registroAccion  cerrando: " + e);
//            }
//        }
//        
//        if(accionTemp.getCodCheque() !=0  ){
//            myFlag = true;
//        }
//        return myFlag;
//    }
//    
//    
//    /**
//     * Total de Registros realizados en una fecha dada
//     * @param fechaTraspaso
//     * @return 
//     */
//    public int totalRegTraspHechos(Date fechaTraspaso ){
//        int elTotal = 0;
//        try {
//            con = conex.conectar();
//            ps = con.prepareStatement("execute p_list_Accion @ACCION=?, @audFecha=? " );
//            ps.setEscapeProcessing(true);
//            ps.setQueryTimeout(180);
//            ps.setString(1, "B");
//            ps.setDate(2, fechaTraspaso);
//            rs = ps.executeQuery();
//            if (rs != null){
//                while (rs.next()){
//                    
//                    elTotal = rs.getInt(1);
//                }
//            }
//        }catch (SQLException e) {
//            System.out.println("Error  totalRegPendTrasp recuperando: " + e);
//        } catch (Exception e) {
//            System.out.println("Error totalRegPendTrasp general: " + e);
//            e.printStackTrace();
//        } finally {
//            try {
//                conex.desconectar(rs);
//            } catch (Exception e) {
//                System.out.println("Error totalRegPendTrasp  cerrando: " + e);
//            }
//        }
//        return elTotal;
//    }
//    
//    
//    /**
//     * Funcion para contar el total de registros que estan pendientes o los cuales no se realizaron el traspaso
//     * @param  codSucursal
//     * @return  int 0 si no encuentra
//     */
//    public int totalRegPendTrasp( int codSucursal ){
//        int elTotal = 0;
//        try {
//            con = conex.conectar();
//            ps = con.prepareStatement("execute p_list_Accion @ACCION=? , @codAccion=? ");
//            ps.setEscapeProcessing(true);
//            ps.setQueryTimeout(180);
//            ps.setString(1, "C");
//            ps.setInt(2, codSucursal );
//            rs = ps.executeQuery();
//            if (rs != null){
//                while (rs.next()){
//                    //accionTemp =new AccionManagedBean( rs.getInt(1), 1, null, null, null,  1, null, null , 1,  null );
//                    elTotal = rs.getInt(1);
//                }
//            }
//        }catch (SQLException e) {
//            System.out.println("Error  totalRegPendTrasp recuperando: " + e);
//        } catch (Exception e) {
//            System.out.println("Error totalRegPendTrasp general: " + e);
//            e.printStackTrace();
//        } finally {
//            try {
//                conex.desconectar(rs);
//            } catch (Exception e) {
//                System.out.println("Error totalRegPendTrasp  cerrando: " + e);
//            }
//        }
//        return elTotal;
//    }
//    
//    /**
//     * Funcion que devulve el total de traspasaso realizados en el dia
//     * @return 
//     */
//    public int totalRegTraspHechos(){
//        int elTotal = 0;
//        try {
//            con = conex.conectar();
//            ps = con.prepareStatement("execute p_list_Accion @ACCION=?");
//            ps.setEscapeProcessing(true);
//            ps.setQueryTimeout(180);
//            ps.setString(1, "D");
//            rs = ps.executeQuery();
//            if (rs != null){
//                while (rs.next()){
//                    elTotal = rs.getInt(1);
//                }
//            }
//        }catch (SQLException e) {
//            System.out.println("Error  totalRegTraspHechos recuperando: " + e);
//        } catch (Exception e) {
//            System.out.println("Error totalRegTraspHechos general: " + e);
//            e.printStackTrace();
//        } finally {
//            try {
//                conex.desconectar(rs);
//            } catch (Exception e) {
//                System.out.println("Error totalRegTraspHechos  cerrando: " + e);
//            }
//        }
//        return elTotal;
//    }
//    
//    
//    
//    /**
//     * Procedimiento para Realizar traspasos , Solo uno por dia o FECHA
//     * @param elUsuario
//     * @param codSucursal
//     * @return Cero si no Realizo la tarea dada
//     */
//    public int generarTraspaso( int elUsuario, int codSucursal) {
//
//        int resp = 0;
//        try {
//            con = conex.conectar();
//            ps = con.prepareStatement("execute dbo.p_list_Accion @ACCION=?, @audUsuario=?, @codAccion=?");
//            ps.setEscapeProcessing(true);
//            ps.setQueryTimeout(180);
//            ps.setString(1, "E" );
//            ps.setInt(2, elUsuario);
//            ps.setInt(3, codSucursal);
//            resp = ps.executeUpdate();
//        } catch (SQLException e) {
//            System.out.println("Error generarTraspaso insertando: " + e);
//            resp = 0;
//        } catch (Exception e) {
//            System.out.println("Error  generarTraspaso general: " + e);
//            e.printStackTrace();
//            resp = 0;
//        } finally {
//            try {
//                conex.desconectar(null);
//            } catch (Exception e) {
//                System.out.println("Error generarTraspaso cerrando: " + e);
//            }
//        }
//        System.out.println(" lo realizo ?  "+ resp );
//        
//        return resp;
//    }
//    
//    /**
//     * Contara el total de registros correspondientes al codigo de cheque
//     * @param codigoChk
//     * @return 
//     */
//    public int totalRegCheq( int codigoChk){
//        int elTotal = 0;
//        try {
//            con = conex.conectar();
//            ps = con.prepareStatement("execute p_list_Accion @ACCION=? , @codCheque=?");
//            ps.setEscapeProcessing(true);
//            ps.setQueryTimeout(180);
//            ps.setString(1, "F");
//            ps.setInt(2,codigoChk );
//            rs = ps.executeQuery();
//            if (rs != null){
//                while (rs.next()){
//                    elTotal = rs.getInt(1);
//                }
//            }
//        }catch (SQLException e) {
//            System.out.println("Error  totalRegCheq recuperando: " + e);
//        } catch (Exception e) {
//            System.out.println("Error totalRegCheq general: " + e);
//            e.printStackTrace();
//        } finally {
//            try {
//                conex.desconectar(rs);
//            } catch (Exception e) {
//                System.out.println("Error totalRegCheq  cerrando: " + e);
//            }
//        }
//        return elTotal;
//    }
//    
//	
//     /**
//     * Verificara si ya se realizo el traspaso del cheque 
//     * @param codigoChk
//     * @return  TRUE  si lo realizo  FALSE  caso contrario
//     */
//    public boolean verifTraspCheq( int codigoChk){
//        int elTotal = 0;
//        try {
//            con = conex.conectar();
//            ps = con.prepareStatement("execute p_list_Accion @ACCION=? , @codCheque=?");
//            ps.setEscapeProcessing(true);
//            ps.setQueryTimeout(180);
//            ps.setString(1, "G");
//            ps.setInt(2,codigoChk );
//            rs = ps.executeQuery();
//            if (rs != null){
//                while (rs.next()){
//                    elTotal = rs.getInt(1);
//                }
//            }
//        }catch (SQLException e) {
//            System.out.println("Error  verifTraspCheq recuperando: " + e);
//        } catch (Exception e) {
//            System.out.println("Error verifTraspCheq general: " + e);
//            e.printStackTrace();
//        } finally {
//            try {
//                conex.desconectar(rs);
//            } catch (Exception e) {
//                System.out.println("Error verifTraspCheq  cerrando: " + e);
//            }
//        }
//		
//		if( elTotal == 1 ){
//			return  true;
//		}else{
//			return  false;
//		}
//    }
//    
//    
//    /**
//     * Verificara que la cantidad de cheques que hayas salido a cobranzas sean la misma que los devueltos
//     * @param codigoChk
//     * @return  TRUE  si coinciden o se permite
//     */
//    public boolean verifCheqDevuelto( int codigoChk){
//        int elTotal = 0;
//        try {
//            con = conex.conectar();
//            ps = con.prepareStatement("execute p_list_Accion @ACCION=? , @codCheque=?");
//            ps.setEscapeProcessing(true);
//            ps.setQueryTimeout(180);
//            ps.setString(1, "H");
//            ps.setInt(2,codigoChk );
//            rs = ps.executeQuery();
//            if (rs != null){
//                while (rs.next()){
//                    elTotal = rs.getInt(1);
//                }
//            }
//        }catch (SQLException e) {
//            System.out.println("Error  verifCheqDevuelto  recuperando: " + e);
//        } catch (Exception e) {
//            System.out.println("Error verifCheqDevuelto  general: " + e);
//            e.printStackTrace();
//        } finally {
//            try {
//                conex.desconectar(rs);
//            } catch (Exception e) {
//                System.out.println("Error verifCheqDevuelto cerrando: " + e);
//            }
//        }
//		
//        if( elTotal == 1 ){
//                return  true;
//        }else{
//                return  false;
//        }
//    }

    
}
