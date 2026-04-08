package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CambiosTigo;
import bo.bosque.com.impexpap.model.ChipTigo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ICambiosTigo {
    // ABM: Registrar / Actualizar / Eliminar / Aplicar
    RespuestaSp abmCambioLinea(CambiosTigo mb, String acc);

    // LIST: Lista unificada de numeros asignados (empleados + externos)
    List<CambiosTigo> listarNumeros(CambiosTigo filtro);

    // LIST: Lista de cambios registrados por periodo/estado
    List<CambiosTigo> listarCambios(CambiosTigo filtro);

    // LIST: Lista de destinos posibles para dropdown de reasignacion
    List<CambiosTigo> listarDestinos(CambiosTigo filtro);

    //LISTA PERIODOS - PARA EL FILTRO DROPDOWN
    List <CambiosTigo> listarPeriodosCambio();
}
