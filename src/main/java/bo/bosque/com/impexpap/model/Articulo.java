package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Articulo implements Serializable {

    private String codArticulo; // esta es una prueba
    private int codigoFamilia;
    private String datoArt;
    private String datoArtExt;
    private float stock;
    private float utm;
    private String unidadMedida;
    private String gramajeSap;
    private int audUsuario;


    /**
     * Constructores
     */
    public Articulo(String codArticulo, int codigoFamilia, String datoArt, String datoArtExt, float stock, float utm, String unidadMedida, String gramajeSap, int audUsuario) {
        this.codArticulo = codArticulo;
        this.codigoFamilia = codigoFamilia;
        this.datoArt = datoArt;
        this.datoArtExt = datoArtExt;
        this.stock = stock;
        this.utm = utm;
        this.unidadMedida = unidadMedida;
        this.gramajeSap = gramajeSap;
        this.audUsuario = audUsuario;
    }
}
