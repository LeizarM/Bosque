package bo.bosque.com.impexpap.model;


import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Empresa implements Serializable {

    private int codEmpresa;
    private String nombre;
    private int codPadre;
    private String sigla;
    private int audUsuario;

    /** Solo los llena {@code p_list_Empresa 'P'}: nombre de la empresa padre, sucursales y cargos. */
    private String padre;
    private int sucursales;
    private int cargos;

}
