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

    /** Solo los llena {@code p_list_Ciudad 'P'}: nombre del país, zonas y sucursales de la ciudad. */
    private String pais;
    private int zonas;
    private int sucursales;

}
