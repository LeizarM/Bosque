package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import bo.bosque.com.impexpap.utils.Tipos;
import bo.bosque.com.impexpap.utils.Utiles;

import java.io.Serializable;
import java.sql.Date;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ChCheque  implements Serializable {
	

    private int nro=0;
    private int  codCheque=0;
    private String  nroCheque="";
    private String  codCliente="";
    private Date  fechaCheque;
    private Date  fechaCobrar;
    //private float  monto;
    private double  monto=0;
    private String  codTipoMoneda ="";
    private String  codTipoCheque ="";
    private String  codTipoEstado ="";
    private String  ordenDe ="";
    private String  reciboManual ="0";
    private String  nroTalonario  ="0";
    private int  codBanco = 0;
    private int codEmpleado = 0;
    
    private int  audUsuarioI =0;
    private Date  audFechaI;
    
    private  Empleado  emb = new Empleado();
    private  ChBanco bmb = new ChBanco();
    private  java.util.Date fecCobrarJ;
    private  java.util.Date fecChequeJ;
    private  ChAccion accmb = new  ChAccion();
    
    private  Tipos tipoMoneda = new Tipos();
    private  Tipos tipoCheque = new Tipos();
    private  Tipos tipoEstado = new Tipos();
    private  Tipos cliente = new Tipos();
    private  Utiles esUtil = new Utiles();
    private boolean seleccionado= false;
    
    private int codSucursal=0;    
    private int nroRecibo=0;    
    


//    public ChequeManagedBean(  int  codCheque,  String  nroCheque, String codCliente, String aNombreDe,  Date  fechaCheque, Date  fechaCobrar,  float  monto, String  codTipoMoneda  , String  codTipoCheque, String codTipoEstado, int  codBanco,  int codEmpleado, String reciboManual, int codSucursal, int nroRecibo, String nroTalonario,  int  audUsuarioI,  Date  audFechaI ) {
//        this.codCheque = codCheque;
//        this.nroCheque = nroCheque;
//        this.codCliente = codCliente;
//        this.fechaCheque = fechaCheque;
//        this.fechaCobrar = fechaCobrar;
//        this.ordenDe = aNombreDe;
//        this.monto = monto;
//        this.codTipoMoneda = codTipoMoneda;
//        this.codTipoCheque = codTipoCheque;
//        this.codTipoEstado = codTipoEstado;
//        this.codBanco = codBanco;
//        this.codEmpleado = codEmpleado;
//        this.reciboManual = reciboManual;
//        this.codSucursal = codSucursal;
//        this.nroRecibo = nroRecibo;
//        this.nroTalonario = nroTalonario;
//        
//        this.audUsuarioI = audUsuarioI;
//        this.audFechaI = audFechaI;
//        
//        
//        this.fecCobrarJ = esUtil.fechaSQL_a_Java(this.fechaCobrar);
//        this.fecChequeJ = esUtil.fechaSQL_a_Java(this.fechaCheque);
//    }
//     
//    public ChequeManagedBean( int nro,  int  codCheque, Timestamp fechaIngreso,  String  nroCheque, String codCliente, String nombreCliente, String aNombreDe,  Date fechaCheque,  Date fechaCobrar, float monto, String  codTipoMoneda, String nombreTipoMoneda, String  codTipoCheque, String nombreTipoCheque, String codTipoEstado, String nombreTipoEstado,  int codBanco, String nombreBanco, int codEmpleado, String datosEmpleado,   String reciboManual, String observacion, int codSucursal, int nroRecibo, String nroTalonario, int  audUsuarioI,  Date  audFechaI ) {
//        this.nro = nro;
//        this.codCheque = codCheque;
//        this.nroCheque = nroCheque;
//        this.codCliente = codCliente;
//        this.ordenDe = aNombreDe;
//        this.fechaCheque =fechaCheque;
//        this.fechaCobrar = fechaCobrar;
//        this.monto = monto;
//        this.codTipoEstado = codTipoEstado;
//        this.codTipoMoneda = codTipoMoneda;
//        this.codTipoCheque = codTipoCheque;
//        this.codBanco = codBanco;
//        this.codEmpleado = codEmpleado;
//        this.reciboManual = reciboManual;
//        this.nroTalonario = nroTalonario;
//        
//        this.audUsuarioI = audUsuarioI;
//        this.audFechaI = audFechaI;
//        
//        this.emb.setCodEmpleado(codEmpleado);
//        //this.emb.getPmb().setDatosPersona(datosEmpleado);
//        
//        this.tipoMoneda.setCodTipos(codTipoMoneda);
//        this.tipoMoneda.setNombre(nombreTipoMoneda);
//        
//        this.tipoCheque.setCodTipos(codTipoCheque);
//        this.tipoCheque.setNombre(nombreTipoCheque);
//        
//        this.tipoEstado.setCodTipos(codTipoEstado);
//        this.tipoEstado.setNombre(nombreTipoEstado);
//        
//        this.cliente.setCodTipos(codCliente);
//        this.cliente.setNombre(nombreCliente);
//        
//        this.bmb.setCodBanco(codBanco);
//        this.bmb.setNombre(nombreBanco);
//        
//        this.accmb.setCodCheque(codCheque);
//        this.accmb.setFecha(fechaIngreso);
//        this.accmb.setFechaJ( esUtil.fechaSQLTIMESTAMP_a_fechaJ(fechaIngreso));
//        this.accmb.setObservacion(observacion);
//        
//        this.fecChequeJ = esUtil.fechaSQL_a_Java(this.fechaCheque);
//        this.fecCobrarJ = esUtil.fechaSQL_a_Java(this.fechaCobrar);
//        
//        this.codSucursal = codSucursal;
//        this.nroRecibo = nroRecibo;
//    }
//    
    
}
