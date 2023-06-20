package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Presentacion implements Serializable {

    private int idPresentacion;
    private String presentacion;
    private int estado;
    private int audUsuario;

}
