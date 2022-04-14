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

}
