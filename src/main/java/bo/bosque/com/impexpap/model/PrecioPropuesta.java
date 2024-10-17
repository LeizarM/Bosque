package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrecioPropuesta implements Serializable {

    private int idPrecioPropuesta;
    private int idPropuesta;
    private int idPrecio;
    private int codigoFamilia;
    private float precioActual;
    private float precioPropuesto;
    private float porcentaje;
    private int codSucursal;
    private int listNum;
    private String nombrePrecio;
    private int vpp;
    private int audUsuario;

}
