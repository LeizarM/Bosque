package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MaterialSalida implements Serializable {

    private int idMs;
    private int idLp;
    private String codArticulo;
    private String descripcion;
    private int nroPaleta;
    private float pesoResma;
    private float pesoPaleta;
    private float pesoMaterial;
    private int cantidadResma;
    private int cantidadHojas;
    private int audUsuario;

}