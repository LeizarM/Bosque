package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CambiosTigo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CambiosTigoDao implements ICambiosTigo {
    private final SpHelper spHelper;

    public CambiosTigoDao(SpHelper spHelper){this.spHelper = spHelper;}

    /**
     * ABM de cambios de lineas corporativas Tigo.
     * Acciones: I=Insertar, U=Actualizar, D=Eliminar, A=Aplicar periodo
     *
     * @param mb  Objeto con los datos del cambio
     * @param acc Accion ('I', 'U', 'D', 'A')
     * @return RespuestaSp con error, errormsg e idGenerado
     */
    @Override
    public RespuestaSp abmCambioLinea(CambiosTigo mb, String acc) {
        return this.spHelper.ejecutarAbm("p_abm_tTigoCambiosLinea", mb, acc);
    }

    /**
     * Lista unificada de numeros asignados (empleados + externos).
     * Filtrable por tipoSocio y search.
     * Accion: L
     *
     * @param filtro Objeto con tipoSocio y/o search como filtros
     * @return Lista de numeros asignados
     */
    @Override
    public List<CambiosTigo> listarNumeros(CambiosTigo filtro) {
        return spHelper.ejecutarListado(
                "p_list_tTigoCambiosLinea",
                filtro,
                "L",
                CambiosTigo.class
        );
    }

    /**
     * Lista de cambios registrados.
     * Filtrable por periodoCobrado, estado, codEmpleado o codCambio.
     * Accion: LC
     *
     * @param filtro Objeto con los filtros deseados
     * @return Lista de cambios registrados
     */
    @Override
    public List<CambiosTigo> listarCambios(CambiosTigo filtro) {
        return spHelper.ejecutarListado(
                "p_list_tTigoCambiosLinea",
                filtro,
                "LC",
                CambiosTigo.class
        );
    }

    /**
     * Lista de destinos posibles para el dropdown de reasignacion.
     * Incluye externos (tTigoSocios) + empleados con corporativo
     * + empleados sin corporativo.
     * Filtrable por tipoSocio y search.
     * Accion: D
     *
     * @param filtro Objeto con tipoSocio y/o search como filtros
     * @return Lista de destinos posibles
     */
    @Override
    public List<CambiosTigo> listarDestinos(CambiosTigo filtro) {
        return spHelper.ejecutarListado(
                "p_list_tTigoCambiosLinea",
                filtro,
                "D",
                CambiosTigo.class
        );
    }
    /**
     * LISTAR PERIODOS PARA EL FILTRO cambios linea tigo
     * @return Lista de periodos
     */
    @Override
    public List<CambiosTigo> listarPeriodosCambio() {
        // Usamos un objeto vacío solo para disparar la acción 'A'
        return spHelper.ejecutarListado(
                "p_list_tTigoCambiosLinea",
                new CambiosTigo(),
                "A",
                CambiosTigo.class
        );
    }

}
