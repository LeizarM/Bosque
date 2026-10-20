package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeEmpleadoElegibleDto;
import bo.bosque.com.impexpap.dto.TcpeLoteDisponibleDto;
import bo.bosque.com.impexpap.dto.TcpeLoteDto;
import bo.bosque.com.impexpap.dto.TcpeLoteFiltroDto;
import bo.bosque.com.impexpap.dto.TcpeLoteGuardarDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Lotes de examen ({@code tcpe_rolExamen}): {@code p_list_tcpe_Lote} y {@code p_abm_tcpe_Lote} (contratos 3.3 y 7). */
public interface ITcpeLote {

    /** {@code L}: lotes con el conteo de exámenes por estado y su ventana. */
    List<TcpeLoteDto> listar(TcpeLoteFiltroDto filtro);

    /** {@code D}: preguntas utilizables de la categoría (vacía sin categoría). */
    List<TcpeLoteDisponibleDto> disponibles(Long codClasificacion);

    /** {@code E}: empleados vigentes con CI y cargo; con {@code codRolExamen}, marca a los que ya están en ese lote. */
    List<TcpeEmpleadoElegibleDto> empleados(Long codRolExamen);

    /** {@code V} de un lote nuevo: lo mismo que {@code I} más los tres conteos, que se combinan (lote mixto). */
    RespuestaSp validar(TcpeLoteGuardarDto lote, int cantidad, int cantPersonas, int cantEmpleados);

    /** {@code V} para agregar: el SP toma modalidad, categoría y preguntas del lote y suma los que ya tiene. */
    RespuestaSp validarAgregar(long codRolExamen, int cantidad, int cantPersonas, int cantEmpleados);

    /** {@code I}: crea el lote vacío ({@code cantExamenGenerar = 0}). {@code idGenerado} = codRolExamen. */
    RespuestaSp crear(TcpeLoteGuardarDto dto, long audUsuario);

    /** {@code U}: lo nulo no se manda y el SP conserva el valor; qué se puede cambiar lo decide el SP. */
    RespuestaSp actualizar(TcpeLoteGuardarDto dto, long audUsuario);

    /** {@code D}: falla si algún examen se inició o tiene resultado. */
    RespuestaSp eliminar(Long codRolExamen, long audUsuario);
}
