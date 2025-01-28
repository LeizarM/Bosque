package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntregaChofer implements Serializable {

    private int idEntrega;
    private int docEntry;
    private int docNum;
    private int docNumF;
    private int factura;
    private String docDate;
    private String docTime;
    private String cardCode;
    private String cardName;
    private String addressEntregaFac;
    private String addressEntregaMat;
    private String vendedor;
    private int uChofer;
    private String itemCode;
    private String dscription;
    private String whsCode;
    private int quantity;
    private int openQty;
    private String db;
    private String valido;
    private float peso;
    private String cochePlaca;
    private String prioridad;
    private String tipo;
    private String obsF;
    private int fueEntregado;
    private String fechaEntrega;
    private float latitud;
    private float longitud;
    private String direccionEntrega;
    private String obs;
    private int codSucursalChofer;
    private int codCiudadChofer;
    private int audUsuario;

    private String fechaNota;
    private String nombreCompleto;
    private int diferenciaMinutos;
    private int codEmpleado;
    private int codSucursal;
    private String cargo;
    private int flag;
    private int ord;
    private String fechaEntregaCad;
    private int rutaDiaria;
    private Date fechaInicio;
    private Date fechaFin;
    private String fechaInicioRutaCad;
    private String fechaFinRutaCad;
    private String estatusRuta;
}