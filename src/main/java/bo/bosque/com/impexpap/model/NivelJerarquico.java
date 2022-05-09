package bo.bosque.com.impexpap.model;
import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class NivelJerarquico {

    private int codNivel;
    private int nivel;
    private float haberBasico;
    private float bonoProduccion;
    private Date fecha;
    private int audUsuario;
}
