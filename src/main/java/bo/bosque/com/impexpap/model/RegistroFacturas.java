package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegistroFacturas implements Serializable {

    private int idFac;
    private int idTf;
    private int codEmpresa;
    private Date fecha;
    private int numFact;
    private String proveedor;
    private String nit;
    private float monto;
    private String descripcion;
    private String cuf;
    private String nroAutorizacion;
    private String codControl;
    private String nitEmpresa;
    private Date fechaSistema;
    private int audUsuario;


    //variables adicionales
    private String nombreEmpresa;
    private String descripcionTf;
    private String direccion;
    private String qrCadena;


}