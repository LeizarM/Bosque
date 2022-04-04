package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.sql.Timestamp;
import java.sql.Date;
import bo.bosque.com.impexpap.utils.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ChAccion  implements Serializable {

	private int codAccion;
    private int codCheque;
    private Timestamp fecha;
    private String estado;
    private int codEmpleado;
    private String nroSAP;
    private String observacion;
    private int audUsuarioI;
    private Date audFechaI;

    private Utiles esUtil = new Utiles();
    private java.util.Date fechaJ ;
    private Tipos tipoEstado = new Tipos();
    private boolean finalizo= false;

    private int nro;
    

    private boolean permitirCambioFecha = true;
    private boolean permitirCambioEstado= true;
    private boolean permitirFinalizacion= true;
    private boolean permitirIngresoDatos= true;
    

    public ChAccion(int codAccion, int codCheque, Timestamp fecha, String estado, int codEmpleado, String nroSAP, String observacion, int audUsuarioI, Date audFechaI){
        this.codAccion = codAccion;
        this.codCheque = codCheque;
        this.fecha = fecha;
        this.estado = estado;
        this.codEmpleado = codEmpleado;
        this.nroSAP = nroSAP;
        this.observacion = observacion;
        this.audUsuarioI = audUsuarioI;
        this.audFechaI = audFechaI;
        //this.fechaJ = esUtil.fechaSQL_a_Java(this.fecha);
        this.fechaJ = esUtil.fechaSQLTIMESTAMP_a_fechaJ(this.fecha);
    }

    public ChAccion(int nro, int codAccion, int codCheque, Timestamp fecha, String estado, String nombreEstado, int codEmpleado, String nroSAP, String observacion, int audUsuarioI, Date audFechaI) {
        this.nro = nro;
        this.codAccion = codAccion;
        this.codCheque = codCheque;
        this.fecha = fecha;
        this.estado = estado;
        this.codEmpleado = codEmpleado;
        this.nroSAP = nroSAP;
        this.observacion = observacion;
        this.audUsuarioI = audUsuarioI;
        this.audFechaI = audFechaI;

        this.tipoEstado.setCodTipos(estado);
        this.tipoEstado.setNombre(nombreEstado);

        //this.fechaJ = esUtil.fechaSQL_a_Java(this.fecha);
        this.fechaJ = esUtil.fechaSQLTIMESTAMP_a_fechaJ(this.fecha);
    }

}
