package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DiaNoLaborableAdmin;
import bo.bosque.com.impexpap.model.DiaNoLaborableAdminSucursal;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IDiaNoLaborableAdmin {

    /**
     * Registra, actualiza o elimina un Dia No Laborable (cabecera + sucursales,
     * atomico dentro del SP).
     * @param mb  Objeto con los datos del dia no laborable
     * @param acc Accion ('I', 'U', 'D')
     * @return RespuestaSp con error, errormsg e idGenerado
     */
    RespuestaSp registrarDiaNoLaborable(DiaNoLaborableAdmin mb, String acc);

    /**
     * Obtiene el listado (grilla) de Dias No Laborables.
     * @param idDiaNoLaborable ID a buscar (0 = sin filtro por id)
     * @param gestion Anio a filtrar por YEAR(fecha) (0 = sin filtro por gestion)
     * @return Lista de DiaNoLaborableAdmin
     */
    List<DiaNoLaborableAdmin> obtenerDiasNoLaborables(long idDiaNoLaborable, int gestion);

    /**
     * Obtiene la matriz de sucursales para el modal ABM, marcando "seleccionado"
     * segun el idDiaNoLaborable recibido.
     * @param idDiaNoLaborable ID del dia no laborable (0 = registro nuevo, todas en 0)
     * @return Lista de DiaNoLaborableAdminSucursal
     */
    List<DiaNoLaborableAdminSucursal> obtenerSucursales(long idDiaNoLaborable);
}
