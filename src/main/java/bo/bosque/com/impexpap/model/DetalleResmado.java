package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor

public class DetalleResmado implements Serializable {

    private int idRetRes;
    private int idRes;
    private String codArticulo;
    private String descripcion;
    private float cantResma;
    private int audUsuario;

}