package bo.bosque.com.impexpap.model;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class NivelJerarquico {

    private int codNivel;
    private int nivel;
    private float haberBasico;
    private float bonoProduccion;
    private Date fecha;
    private int audUsuario;

    /// Atributos Auxiliares
    private int activo;

    /** Cuántos cargos usan la escala; solo lo llena {@code p_list_NivelJerarquico 'P'}. */
    private int cargos;
}
