package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CostoIncreCiudadDto;
import bo.bosque.com.impexpap.dto.CostoIncreSucursalDto;
import bo.bosque.com.impexpap.model.CostoIncre;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a tpr_costoIncre por procedimiento almacenado, via SpHelper.
 *
 * <p>Reemplaza al DAO legacy que armaba el EXEC a mano con jdbcTemplate, leia el
 * resultset por indice de columna y se tragaba los errores devolviendo un
 * boolean. Ahora las escrituras devuelven {@link RespuestaSp} (con el id
 * generado y el mensaje de negocio del SP) y las lecturas se mapean por NOMBRE
 * de columna con BeanPropertyRowMapper.
 *
 * <p>Todas las lecturas arman el Map de parametros a mano en vez de mandar el
 * modelo: p_list_costoIncre usa "@parametro IS NULL" como "sin filtro", asi que
 * un 0 que Jackson mande de relleno filtraria de verdad y devolveria cero filas.
 */
@Repository
public class CostoIncreDao implements ICostoIncre {

    /** Nombre del SP de altas, bajas y modificaciones. */
    private static final String SP_ABM = "p_abm_costoIncre";

    /** Nombre del SP de listados. */
    private static final String SP_LIST = "p_list_costoIncre";

    private final SpHelper spHelper;

    public CostoIncreDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Invoca p_abm_costoIncre con ACCION 'I'.
     *
     * @return RespuestaSp; getIdGenerado() trae el idIncre nuevo (SCOPE_IDENTITY).
     */
    @Override
    public RespuestaSp insertar(CostoIncre costoIncre) {
        return this.spHelper.ejecutarAbm(SP_ABM, costoIncre, "I");
    }

    /**
     * Invoca p_abm_costoIncre con ACCION 'U'. El SP actualiza valor, audUsuario
     * y audFecha de la fila identificada por idIncre.
     *
     * @return RespuestaSp; getIdGenerado() queda en 0 porque no hay alta.
     */
    @Override
    public RespuestaSp actualizar(CostoIncre costoIncre) {
        return this.spHelper.ejecutarAbm(SP_ABM, costoIncre, "U");
    }

    /**
     * Invoca p_abm_costoIncre con ACCION 'D'.
     *
     * <p>Usa el overload de Map para mandar unicamente @idIncre: el resto de los
     * parametros queda en su DEFAULT NULL y el SP no los mira en esta rama.
     *
     * @return RespuestaSp con error 0 si borro, o el error de negocio del SP.
     */
    @Override
    public RespuestaSp eliminar(Long idIncre) {
        Map<String, Object> params = new HashMap<>();
        params.put("idIncre", idIncre);
        return this.spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    /**
     * Invoca p_list_costoIncre con ACCION 'L'.
     *
     * @return filas planas de tpr_costoIncre mapeadas a {@link CostoIncre}.
     */
    @Override
    public List<CostoIncre> listar(Long idIncre) {
        Map<String, Object> filtro = new HashMap<>();
        if (idIncre != null) {
            filtro.put("idIncre", idIncre);
        }
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "L", CostoIncre.class);
    }

    /**
     * Invoca p_list_costoIncre con ACCION 'B'.
     *
     * @return costos cargados con el nombre de la sucursal; el idIncre llega en
     *         null porque esa rama no lo selecciona.
     */
    @Override
    public List<CostoIncreSucursalDto> listarCostoPorSucursal() {
        return this.spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "B",
                CostoIncreSucursalDto.class);
    }

    /**
     * Invoca p_list_costoIncre con ACCION 'C'.
     *
     * @return sucursales con su ciudad y costo en 0, para la carga inicial de
     *         los fletes de una propuesta.
     */
    @Override
    public List<CostoIncreCiudadDto> listarSucursalesParaCarga() {
        return this.spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "C",
                CostoIncreCiudadDto.class);
    }

    /**
     * Invoca p_list_costoIncre con ACCION 'D'.
     *
     * @return costos de flete de la propuesta, con nombre de sucursal e idIncre
     *         para poder editarlos despues.
     */
    @Override
    public List<CostoIncreSucursalDto> listarPorPropuesta(Long idPropuesta) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idPropuesta", idPropuesta);
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "D", CostoIncreSucursalDto.class);
    }
}
