package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.OpcionDto;
import bo.bosque.com.impexpap.model.AsigFunModXEmpl;
import bo.bosque.com.impexpap.model.FuncionesXModulo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Funciones por módulo ({@code tb_funcionesXModulo}) y su asignación a empleados
 * ({@code tb_asigFunModXEmpl}). Reglas y mensajes los ponen sus {@code p_abm_}.
 */
public interface IFuncionesModulo {

    /** {@code p_list_FuncionesXModulo 'M'}: los módulos que pueden tener funciones. */
    List<OpcionDto> modulos();

    /** {@code p_list_FuncionesXModulo 'P'}; {@code codModulo} 0 trae todas. */
    List<FuncionesXModulo> listarFunciones(long codModulo);

    RespuestaSp registrarFuncion(FuncionesXModulo funcion);

    RespuestaSp eliminarFuncion(long codFuncMod, long audUsuario);

    /** {@code p_list_AsigFunModXEmpl 'P'}; cada filtro en 0 (o estado vacío) no filtra. */
    List<AsigFunModXEmpl> listarAsignaciones(long codModulo, long codFuncMod, long codSucursal, String estado);

    /** {@code p_list_AsigFunModXEmpl 'S'}: sucursales activas con su empresa. */
    List<OpcionDto> sucursales();

    /** {@code p_list_AsigFunModXEmpl 'E'}: empleados con relación vigente de una sucursal. */
    List<OpcionDto> empleadosDeSucursal(long codSucursal);

    /** {@code p_abm_AsigFunModXEmpl} 'I' (codAsignacion 0) o 'U' (solo el estado). */
    RespuestaSp registrarAsignacion(AsigFunModXEmpl asignacion);
}
