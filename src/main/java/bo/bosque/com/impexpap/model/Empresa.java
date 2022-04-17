package bo.bosque.com.impexpap.model;


import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Empresa implements Serializable {

    private int codEmpresa;
    private String nombreEmpresa;
    private int codPadre;
    private String sigla;
    private int audUsuario;

}
