package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Tipo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso al catalogo <b>tpr_tipo</b> por procedimiento almacenado.
 *
 * <p>Reemplaza a la version con jdbcTemplate crudo, cuyos defectos NO se
 * replican aqui: se tragaba la excepcion y devolvia un boolean sin motivo del
 * fallo, no tenia metodos de listado (la pantalla no tenia de donde leer), y
 * pasaba idTipo/estado/audUsuario con {@code setInt} sobre columnas bigint que
 * ademas admiten NULL.
 */
@Repository
public class TipoDao implements ITipo {

    private final SpHelper spHelper;

    public TipoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * ACCION 'I', 'U' o 'D' de {@code p_abm_tipo}.
     *
     * <p>Usa el overload de modelo: los cinco campos de {@link Tipo} son
     * exactamente los parametros de entrada del SP. En el alta el SP devuelve
     * el nuevo idTipo en {@code @idGenerado}.
     *
     * @return RespuestaSp; si el SP marca error de negocio, SpHelper lanza
     *         SpBusinessException y la maneja el GlobalExceptionHandler
     */
    @Override
    public RespuestaSp registrarTipo(Tipo mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tipo", mb, acc);
    }

    /**
     * ACCION 'L' de {@code p_list_tipo}: idTipo, tipo, estado, audUsuario y
     * audFecha (esta ultima se agrega al final del SELECT en tpr_Tipo.sql).
     *
     * <p>Va con el overload de Map y no con el de modelo: el SP trata
     * "&#64;idTipo IS NULL" como "sin filtro" y el overload de modelo conserva
     * los Number en 0, con lo que un "listame todo" terminaria filtrando por
     * idTipo = 0 y devolviendo cero filas.
     *
     * @param idTipo 0 = todos los tipos
     */
    @Override
    public List<Tipo> obtenerTipos(long idTipo) {
        Map<String, Object> filtro = new HashMap<>();
        if (idTipo > 0) filtro.put("idTipo", idTipo);
        return spHelper.ejecutarListado("p_list_tipo", filtro, "L", Tipo.class);
    }

    /**
     * ACCION 'A' de {@code p_list_tipo}: solo los tipos con estado = 1.
     * El resultset trae unicamente idTipo y tipo; el resto del model queda nulo.
     */
    @Override
    public List<Tipo> obtenerTiposActivos() {
        return spHelper.ejecutarListado("p_list_tipo", new HashMap<String, Object>(), "A", Tipo.class);
    }
}
