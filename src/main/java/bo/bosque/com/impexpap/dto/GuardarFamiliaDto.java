package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.Porcentaje;
import bo.bosque.com.impexpap.model.Producto;
import lombok.*;

import java.util.List;

/**
 * Lo que manda la ficha de una familia al registrarla o actualizarla: la fila de
 * {@code tpr_producto} y, como en el dialogo "Registro de familias" del JSF, la grilla
 * "Porcentaje por familia" (un porcentaje por lista de precios activa).
 *
 * <p>Extiende {@link Producto} para que el JSON de la ficha siga siendo plano. Nunca se
 * pasa tal cual a {@code SpHelper.ejecutarAbm}: Jackson mandaria {@code porcentajes} como
 * parametro de {@code p_abm_producto} y el EXEC fallaria. El controlador arma un
 * {@link Producto} limpio.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class GuardarFamiliaDto extends Producto {

    /**
     * De cada fila se usan {@code idClasificacion} y {@code porcen}. El {@code idPorcen} lo
     * resuelve el servidor contra la base, no se acepta del cuerpo. Null = no se tocan los
     * porcentajes.
     */
    private List<Porcentaje> porcentajes;
}
