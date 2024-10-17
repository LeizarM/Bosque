package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Tipo implements Serializable {

    private int idTipo;
    private String tipo;
    private int estado;
    private int audUsuario;

}
