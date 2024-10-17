package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPropuesto implements Serializable {

    private int idArticulo;
    private int idPropuesta;
    private String codArticulo;
    private int codigoFamilia;
    private String datoArticulo;
    private float stock;
    private float utm;
    private int audUsuario;

    private int fila;
    private int filaCod;
    private String codCad;

    private String titulo;
    private String obs;

    private String nombreSucursal;
    private int listNumPap; //El primary key de la tabla sap(PAPIRUS) para cada numero de lista de precio
    private int listNumIpx; // El primary key de la tabla sap (IMPEXPAP) para cada  numero de lista de precio
    private int vpp;
    private float precio;//precio actual
    private float precioCalc;
    private float precioUnitUsdPap;
    private float precioUnitBsPap;
    private float precioUnitUsdIpx;
    private float precioUnitBsIpx;
    private float precioPropuesto;
    private String monedaUSD;
    private String monedaBS;
    private String familia;
    private String proveedor;
    private float costo;
}
