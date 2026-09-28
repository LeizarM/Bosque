package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Color;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso al catalogo tpr_color por procedimiento almacenado.
 *
 * <p>Reemplaza a la version con jdbcTemplate crudo, que tenia dos defectos que
 * NO se replican aqui: se tragaba la excepcion y devolvia un boolean sin motivo
 * del fallo, y armaba el EXEC con cinco '?' para cuatro parametros nombrados
 * (el &#64;ACCION quedaba fuera del texto SQL), asi que el alta nunca corria.
 */
@Repository
public class ColorDao implements IColor {

    private final SpHelper spHelper;

    public ColorDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * ACCION 'I', 'U' o 'D' de p_abm_color.
     *
     * <p>Usa el overload de modelo: los cinco campos de Color son exactamente
     * los parametros de entrada del SP. En el alta el SP devuelve el nuevo
     * idColor en &#64;idGenerado.
     *
     * @return RespuestaSp; si el SP marca error de negocio, SpHelper lanza
     *         SpBusinessException y lo maneja el GlobalExceptionHandler
     */
    @Override
    public RespuestaSp registrarColor(Color mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_color", mb, acc);
    }

    /**
     * ACCION 'L' de p_list_color: idColor, color, estado, audUsuario y audFecha.
     *
     * <p>Va con el overload de Map y no con el de modelo: el SP trata
     * "&#64;idColor IS NULL" como "sin filtro" y el overload de modelo conserva
     * los Number en 0, con lo que un "listame todo" terminaria filtrando por
     * idColor = 0 y devolviendo cero filas.
     *
     * @param idColor 0 = todos los colores
     */
    @Override
    public List<Color> obtenerColores(long idColor) {
        Map<String, Object> filtro = new HashMap<>();
        if (idColor > 0) filtro.put("idColor", idColor);
        return spHelper.ejecutarListado("p_list_color", filtro, "L", Color.class);
    }

    /**
     * ACCION 'A' de p_list_color: solo los colores con estado = 1.
     * El resultset trae unicamente idColor y color; el resto del model queda nulo.
     */
    @Override
    public List<Color> obtenerColoresActivos() {
        return spHelper.ejecutarListado("p_list_color", new HashMap<String, Object>(), "A", Color.class);
    }
}
