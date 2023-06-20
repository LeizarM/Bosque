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
}
