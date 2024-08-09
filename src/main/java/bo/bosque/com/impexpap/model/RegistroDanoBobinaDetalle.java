package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegistroDanoBobinaDetalle implements Serializable {


    private String idRegDet;
    private int idReg;
    private int idTd;
    private String codArticulo;
    private String descripcion;
    private float pesoBobina;
    private float diametro;
    private float cmDanado;
    private float ancho;
    private float cca;
    private float ccb;
    private float kilosDanadosReal;
    private float ccc;
    private float cma;
    private float cmb;
    private float kilosDanados;
    private float precioUnitario;
    private float subtotalUsd;
    private String placa;
    private String chofer;
    private int audUsuario;

}