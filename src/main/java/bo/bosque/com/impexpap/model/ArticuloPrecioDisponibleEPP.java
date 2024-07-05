package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPrecioDisponibleEPP implements Serializable {

    private String codArticulo;
    private String datoArt;
    private int listaPrecio;
    private float precio;
    private String moneda;
    private float gramaje;
    private int codigoFamilia;
    private int disponible;
    private String unidadMedida;
    private int codCiudad;
    private int codGrpFamiliaSap;
    private String ruta;
    private int audUsuario;

}