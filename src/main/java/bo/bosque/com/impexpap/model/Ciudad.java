package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Ciudad implements Serializable {

    private int codCiudad;
    private int codPais;
    private String ciudad;
    private int audUsuario;

}
