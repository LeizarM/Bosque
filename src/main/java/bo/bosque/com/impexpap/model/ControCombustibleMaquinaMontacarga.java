package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ControCombustibleMaquinaMontacarga implements Serializable {


    private long idCM;
    private int idMaquinaVehiculoOrigen;
    private int idMaquinaVehiculoDestino;
    private int codSucursalMaqVehiOrigen;
    private int codSucursalMaqVehiDestino;
    private String codigoOrigen;
    private String codigoDestino;
    private Date fecha;
    private float litrosIngreso;
    private float litrosSalida;
    private float saldoLitros;
    private int codEmpleado;
    private String codAlmacen;
    private String obs;
    private String tipoTransaccion;
    private int audUsuario;


    /**
     * Varibles de apoyo
     **/

    private String whsCode;
    private String whsName; // nombre de almacen
    private String maquina;
    private String nombreCompleto;
    private String nombreMaquinaOrigen;
    private String nombreMaquinaDestino;
    private String nombreSucursal;
    private Date fechaInicio;
    private Date fechaFin;

}