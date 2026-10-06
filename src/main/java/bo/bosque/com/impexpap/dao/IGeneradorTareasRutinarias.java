package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.utils.RespuestaSp;

public interface IGeneradorTareasRutinarias {

    /**
     * Corre p_generar_tac_bitTareaRuti — genera las ocurrencias del día para
     * TODOS los cargos activos, según la frecuencia vigente de cada tarea
     * rutinaria (lee tac_tareaRutinaria.idFrec en vivo, así que si una tarea
     * cambió de frecuencia ya usa la nueva sin nada adicional). La llama el
     * Job una vez al día
     * ({@code DatabaseTaskScheduler#generarOcurrenciasTareasRutinarias()}) y,
     * desde el 2026-10-05, también
     * {@link bo.bosque.com.impexpap.commons.GeneracionAlInstante} cada vez que
     * se asigna o se edita una tarea, para que aparezca sin esperar al Job. Es
     * idempotente: correrla de más no duplica nada.
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
