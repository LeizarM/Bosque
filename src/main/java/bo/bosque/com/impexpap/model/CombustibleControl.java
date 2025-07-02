package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CombustibleControl implements Serializable {

    private long idC;
    private int idCoche;
    private Date fecha;
    private String estacionServicio;
    private String nroFactura;
    private float importe;
    private float kilometraje;
    private int codEmpleado;
    private float diferencia;
    private int codSucursalCoche;
    private String obs;
    private float litros;
    private String tipoCombustible;
    private int idCM;
    private int audUsuario;


    /**
     * Varibales de apoyo
     */
    private String coche;
    private float kilometrajeAnterior;
    private int esMenor;
    private String nombreCompleto;


}