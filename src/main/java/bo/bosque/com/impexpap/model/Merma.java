package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Merma implements Serializable {

    private int idMe;
    private int idLp;
    private String codArticulo;
    private String descripcion;
    private float peso;
    private int audUsuario;

}