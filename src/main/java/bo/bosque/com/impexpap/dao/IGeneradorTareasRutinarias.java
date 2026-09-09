package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.utils.RespuestaSp;

public interface IGeneradorTareasRutinarias {

    /**
     * Corre p_generar_tac_bitTareaRuti — genera las ocurrencias del día para
     * TODOS los cargos activos, según la frecuencia vigente de cada tarea
     * rutinaria (lee tac_tareaRutinaria.idFrec en vivo, así que si una tarea
     * cambió de frecuencia ya usa la nueva sin nada adicional). Pensado para
     * llamarse una vez al día desde
     * {@code DatabaseTaskScheduler#generarOcurrenciasTareasRutinarias()}, no
     * desde la UI.
     *
     * <p>No genera los flujos marcados como "a requerimiento"
     * ({@code tac_tareaRutinaria.esARequerimiento = 1}): Caja Fuerte, Coches,
     * Caja Chica y Cierre de Operaciones dejaron de ser tareas rutinarias y
     * viven como submódulos de la vista 87, donde la ocurrencia se crea recién
     * cuando alguien entra a hacer el trabajo.</p>
     *
     * @return idGenerado = cantidad de filas insertadas
     */
    RespuestaSp generarOcurrencias();
}
