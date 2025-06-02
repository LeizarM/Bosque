package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MaquinaMontacarga implements Serializable {

    private int idMaquina;
    private String codigo;
    private String marca;
    private String clase;
    private int anio;
    private String color;
    private int codSucursal;
    private int estado;
    private int audUsuario;

    private String nombreSucursal;
    private String maquinaOVehiculo;

}