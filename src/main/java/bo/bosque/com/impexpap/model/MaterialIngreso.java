package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MaterialIngreso implements Serializable {

    private int idMi;
    private int idLp;
    private String codArticulo;
    private String descripcion;
    private float pesoKilos;
    private float balanza;
    private String numImportacion;
    private int audUsuario;


}