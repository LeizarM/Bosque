package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Zona implements Serializable {

    private int codZona;
    private int codCiudad;
    private String zona;
    private int audUsuario;
}
