package bo.bosque.com.impexpap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Zona implements Serializable {

    private int codZona;
    private int codCiudad;
    private String zona;
    private int audUsuario;
}
