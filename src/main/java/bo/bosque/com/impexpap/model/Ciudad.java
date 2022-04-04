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
public class Ciudad implements Serializable {

    private int codCiudad;
    private int codPais;
    private String ciudad;
    private int audUsuario;

}
