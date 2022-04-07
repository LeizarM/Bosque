package bo.bosque.com.impexpap.model;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Empresa {

    private int codEmpresa;
    private String nombreEmpresa;
    private int codPadre;
    private String sigla;
    private int audUsuario;

    private Sucursal sucursal;
}
