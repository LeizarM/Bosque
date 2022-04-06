package bo.bosque.com.impexpap.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Empresa {

    private int codEmpresa;
    private String nombreEmpresa;
    private int codPadre;
    private String sigla;
    private int audUsuario;
}
