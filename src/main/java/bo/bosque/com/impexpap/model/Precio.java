package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Precio implements Serializable {

    private int idPrecio;
    private int codigoFamilia;
    private int idClasificacion;
    private float precio;
    private int audUsuario;

    private float precioNew;
    private int idSucursal;
    private String  nombrePrecio;
    private int vpp;
    private String  nombreSucursal;
    private int fila;
    private float porcentaje;
    private int listNum;
    private int idPresentacion;
    private String nombreProveedor;
    private String formato;
    private String nombreFamilia;
    private String gramaje;
    private String color;
    private int idPropuesto;
    private int idPrecioPropuesto;
    private float iva;
    private float it;
}
