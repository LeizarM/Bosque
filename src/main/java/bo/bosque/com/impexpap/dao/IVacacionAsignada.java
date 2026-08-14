package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.VacacionAsignada;

import java.util.Date;
import java.util.List;

/**
 * ABM de {@code trh_vacacionAsignada} — los días que la empresa le reconoce a alguien por cada
 * aniversario de su relación laboral.
 *
 * <p>SPs: {@code p_abm_vacacionAsignada} para escribir, {@code p_list_vacacionAsignada} para leer.
 *
 * <h3>El SP no valida NADA</h3>
 * Acepta días negativos, fechas de 1900, ACCIONes que no existen (no hace nada y no falla) y no
 * mira duplicados. No devuelve envelope de error ni id generado: lo único que llega es el
 * "algo pasó / no pasó" del rowcount. <b>Toda regla de negocio se valida en Java, antes.</b>
 *
 * <h3>Hay un proceso automático escribiendo en la misma tabla</h3>
 * 1.155 de las 1.424 filas las puso un job ({@code audUsuarioI = -1}, día 1 de cada mes a las
 * 07:00), con fechas ya cargadas hasta agosto de 2026. Esta pantalla no es la dueña de la tabla:
 * después de escribir hay que releer, y el historial no se cachea.
 */
public interface IVacacionAsignada {

    /**
     * [B] La grilla del empleado: una fila por aniversario de su relación laboral hasta
     * {@code fechaCorte}, mezclando las filas REALES con las SINTÉTICAS
     * ({@code codVacacionAsignada = 0}, {@link VacacionAsignada#isSintetico()}) de los
     * aniversarios que todavía no tienen registro.
     *
     * <p>De acá sale el "Nuevo" de la UI: no hay un botón suelto de alta, se da de alta SOBRE un
     * aniversario sintético y la fecha la pone el servidor.
     *
     * <p>Devuelve vacío si el empleado no tiene fecha de inicio de beneficio
     * ({@code tb_empleado.codRelBeneficios}): sin ella el SP no puede generar ningún aniversario.
     *
     * @param codRelEmplEmpr la relación por la que filtrar; {@code null} (o {@code 0}) y la
     *                       resuelve el DAO contra la base. El cliente no está obligado a haber
     *                       consultado la ficha de saldo antes de abrir el historial.
     * @param fechaCorte hasta dónde llegar; {@code null} = hoy. <b>No es opcional para el SP</b>:
     *                   con NULL devolvería la grilla vacía sin avisar, así que se sustituye acá.
     */
    List<VacacionAsignada> historial(long codEmpleado, Long codRelEmplEmpr, Date fechaCorte);

    /**
     * [D] Las vacaciones asignadas REALES del empleado entre dos fechas — el botón "Buscar Vac
     * Ganadas" de la nómina de permisos.
     *
     * <p>Distinta de {@link #historial}: aquélla es el desglose por aniversario con filas
     * sintéticas para los años sin cargar; ésta es lo que hay en la tabla, ordenado por fecha
     * descendente. Las dos fechas son opcionales.
     */
    List<VacacionAsignada> buscarPorRango(long codEmpleado, Long codRelEmplEmpr, Date desde, Date hasta);

    /** [L] Una fila por su id, para releer después de escribir. {@code null} si no existe. */
    VacacionAsignada porId(long codVacacionAsignada);

    /**
     * [I] Alta sobre un aniversario.
     *
     * <p><b>Valida en Java, en este orden</b> (el mismo del legacy, cortando en el primer fallo):
     * empleado &gt; 0, días &ge; 0 (<b>cero es válido</b>: "este año no le corresponde nada"),
     * motivo de 2 a 300 caracteres, y que {@code fecha} sea realmente uno de los aniversarios que
     * devuelve {@link #historial} y que todavía esté libre.
     *
     * <p><b>{@code codRelEmplEmpr} del modelo se IGNORA:</b> la relación la resuelve el DAO contra
     * la base, igual que en el abono. Aceptarla del cliente permitía insertar la vacación en la
     * relación de otra persona —el alta no comprobaba que fuera del empleado— y mover el saldo de
     * dos persona-relación a la vez.
     *
     * <p><b>Duplicado, en dos niveles.</b> Una fila idéntica cargada hace segundos es el doble
     * toque del teléfono y se rechaza con <b>409, aunque venga confirmada</b> (el segundo toque
     * manda el mismo cuerpo, confirmación incluida). Una fila vieja o distinta en ese mismo
     * aniversario no se bloquea —el histórico tiene 215 grupos legítimamente repetidos, casi todos
     * del job—: se responde <b>400 pidiendo confirmación</b> y pasa con {@code confirmado = true}.
     *
     * <p><b>El SP no devuelve el id generado</b>, así que se relee por (empleado, relación, fecha)
     * y se devuelve la fila que quedó, que además es la única forma de saber que quedó.
     *
     * @param audUsuarioI quién lo registra, sacado del token y nunca del body
     * @return la fila tal como quedó en la base
     */
    VacacionAsignada alta(VacacionAsignada v, long audUsuarioI, boolean confirmado);

    /**
     * [U] Edición. <b>Sólo días y motivo.</b>
     *
     * <p>El SP no actualiza {@code codEmpleado} y deja {@code codRelEmplEmpr} comentado a
     * propósito; la fecha tampoco se edita (en el legacy el campo va {@code disabled}), así que se
     * conserva la de la fila existente. Lo que llegue en esos campos se ignora.
     *
     * @return la fila releída después del UPDATE
     */
    VacacionAsignada edicion(long codVacacionAsignada, double diasAsignados, String motivo,
                             long audUsuarioI);

    /**
     * [D] Baja. DELETE físico, recuperable: el trigger {@code dad_vacacionAsignada} copia la fila
     * a {@code trh_vacacionAsignadaEliminado} con la fecha de borrado.
     *
     * <p>{@code codEmpleado} no es decorativo: el SP borra por id sin mirar de quién es la fila,
     * así que acá se comprueba la pertenencia antes de llamarlo.
     *
     * @param audUsuarioI quién borra, del token. <b>Se escribe en la fila con una 'U' previa</b>:
     *                    la ACCION 'D' no lo recibe, así que sin ese paso el trigger la archivaría
     *                    con el usuario que la CREÓ (o con el {@code -1} del job mensual) y la
     *                    bitácora atribuiría el borrado a quien no fue.
     * @return la fila que se borró, para poder decir en pantalla qué desapareció
     */
    VacacionAsignada baja(long codVacacionAsignada, long codEmpleado, long audUsuarioI);
}
