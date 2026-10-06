// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IBitTareaRuti.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.BitacoraCumplimientoDto;
import bo.bosque.com.impexpap.dto.BitacoraFiltroRequest;
import bo.bosque.com.impexpap.dto.DiagnosticoGeneracionDto;
import bo.bosque.com.impexpap.dto.ResumenGeneracionDto;
import bo.bosque.com.impexpap.model.BitTareaRuti;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

public interface IBitTareaRuti {

    /**
     * Registrar, actualizar o eliminar una bitácora de tarea rutinaria.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(BitTareaRuti mb, String acc);

    /**
     * [L] Listado de bitácoras de tarea rutinaria con filtros opcionales (todos NULL = todas).
     */
    List<BitTareaRuti> listar(BitTareaRuti filtro);

    /**
     * [L] Carga UNA bitácora de tarea rutinaria por ID exacto (para load-before-update).
     * Solo envía @idBitTarea al SP; todo lo demás queda en DEFAULT NULL.
     */
    BitTareaRuti obtenerPorId(long idBitTarea);

    /**
     * [A] Abre la ocurrencia de hoy de un flujo "a requerimiento" y devuelve su
     * idBitTarea en {@code idGenerado}.
     *
     * <p>Caja Fuerte, Coches, Caja Chica y Cierre de Operaciones dejaron de ser
     * tareas rutinarias (el Job ya no las genera, ver
     * {@code tac_tareaRutinaria.esARequerimiento}) y viven como submódulos de la
     * vista 87. Como todas sus tablas — {@code tac_llegada},
     * {@code tac_cocheLlegadas}, {@code tac_cajaChica} — siguen colgando de una
     * ocurrencia, la ocurrencia se crea aquí, en el momento en que alguien entra
     * a hacer el trabajo. Así ni los procs ni los reportes ni el historial se
     * enteran del cambio.
     *
     * <p>Es idempotente: entrar dos veces el mismo día devuelve la misma
     * ocurrencia, no crea una segunda (el SP serializa con
     * {@code sp_getapplock} por empleado+tarea+fecha, que es lo que evita que
     * abrir desde el celular y la computadora a la vez cree dos).
     *
     * <p>El SP rechaza con error 31 cualquier tarea que NO esté marcada a
     * requerimiento, para que este camino no pueda fabricar ocurrencias en
     * paralelo a las que genera el Job.
     *
     * @param idTarRuti   la tarea del flujo (40 Caja Fuerte, 41 Coches,
     *                    42 Caja Chica, 2 Cierre de Operaciones)
     * @param codEmpleado de quién es la ocurrencia — del token, nunca del body
     * @param audUsuario  usuario que audita
     */
    RespuestaSp abrirFlujoARequerimiento(long idTarRuti, long codEmpleado, long audUsuario);

    /**
     * [O] Agrega una observación a una ocurrencia ya respondida (archivo SQL 75).
     *
     * <p>Es lo único que se puede hacer con una tarea hecha (Marcelo,
     * 2026-10-05: "una vez que realiza esas tareas que ya no la vuelva hacer.
     * Máximo agregar una observación"). Agrega, no reemplaza: el SP pone lo
     * nuevo al final, con fecha y hora. No toca {@code fueRealizado}.
     *
     * <p>No valida de quién es la ocurrencia: eso lo hace el controller.
     */
    RespuestaSp agregarObservacion(long idBitTarea, String obs, long audUsuario);

    /**
     * [B] Bitácora de cumplimiento: una fila por ocurrencia del rango, con el
     * cargo y la sucursal que la persona tenía ESE día (archivo SQL 55).
     *
     * <p>Una pendiente con fecha pasada sale como no realizada: la gente no
     * responde "No", deja de responder.
     *
     * @param filtro          rango obligatorio (hasta un año) y filtros opcionales
     * @param codEmpleadoJefe null = toda la empresa; si no, esa persona y su
     *                        subárbol. Lo decide el controller desde el token,
     *                        nunca el cliente.
     */
    List<BitacoraCumplimientoDto> bitacoraCumplimiento(BitacoraFiltroRequest filtro, Long codEmpleadoJefe);

    /**
     * [R] Las tareas que generó el Job en un día, en la sucursal de una
     * ocurrencia de Cierre de Operaciones (archivo SQL 60): el panel de la
     * revisión donde quien cierra ve si los demás hicieron las suyas. Mismas
     * columnas que {@link #bitacoraCumplimiento}.
     *
     * @param idBitTarea      la ocurrencia de Cierre; de ella sale la sucursal
     * @param fecha           el día que se revisa
     * @param todasSucursales true = toda la empresa; el controller exige chkSuc
     */
    List<BitacoraCumplimientoDto> tareasDelCierre(long idBitTarea, Date fecha, boolean todasSucursales);

    /**
     * [G] Cuántos candidatos cayeron en cada motivo, por corrida del generador
     * (lo escribe el generador desde el archivo SQL 54). Fechas nulas = sin
     * límite por ese lado.
     */
    List<ResumenGeneracionDto> resumenGeneracion(Date fechaIni, Date fechaFin);

    /**
     * [P] Por qué a una persona le llegó o no cada tarea de sus cargos en una
     * fecha.
     *
     * @param fecha           nula = hoy (el de SQL Server)
     * @param idTarRuti       nula = todas las tareas de sus cargos
     * @param codEmpleadoJefe null = sin restricción; si la persona no es de su
     *                        equipo, el SP devuelve una sola fila con filtro -2
     */
    List<DiagnosticoGeneracionDto> porQue(long codEmpleado, Date fecha, Long idTarRuti, Long codEmpleadoJefe);
}
