package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Pais implements Serializable {

    private int codPais;
    private String pais;
    private int audUsuario;

    /** Solo los llena {@code p_list_Pais 'P'}: ciudades del país y personas con esa nacionalidad. */
    private int ciudades;
    private int personas;
}
