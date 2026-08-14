package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.AbonoDias;

import java.util.List;

/**
 * ABM de {@code trh_abonoDias} — días libres que se le abonan a alguien fuera del ciclo de
 * vacación, individualmente o en lote.
 *
 * <p>SPs: {@code p_abm_AbonoDias} para escribir, {@code p_list_AbonoDias} para leer.
 *
 * <h3>El SP no valida NADA</h3>
 * Igual que su gemelo de vacación asignada: sin envelope de error, sin id generado, sin control de
 * duplicado y sin transacción. Todo eso está acá.
 *
 * <h3>Umbral distinto al de vacación asignada</h3>
 * Acá los días son {@code > 0} ESTRICTO. Un abono de cero días no existe (el mínimo real medido es
 * 0,5) y no significaría nada; en cambio una vacación asignada de cero días sí significa algo.
 */
public interface IAbonoDias {

    /**
     * [L] Los abonos del empleado en su relación vigente, del más nuevo al más viejo.
     *
     * <p>Se usa la ACCION 'L' y no la 'A' a propósito: la 'A' trae el nombre pero con
     * {@code INNER JOIN} a {@code tb_relEmplEmpr}, y {@code codRelEmplEmpr} es nullable — un abono
     * cargado sin relación desaparecería del listado y parecería que no se guardó.
     *
     * @param codRelEmplEmpr la relación por la que filtrar; {@code null} (o {@code 0}) y la
     *                       resuelve el DAO contra la base, igual que en el alta. El cliente no
     *                       está obligado a haber consultado la ficha antes de ver un historial.
     */
    List<AbonoDias> historial(long codEmpleado, Long codRelEmplEmpr);

    /** [L] Una fila por su id. {@code null} si no existe. */
    AbonoDias porId(long codAbonoDias);

    /**
     * [I] Alta individual.
     *
     * <p><b>Valida en Java:</b> empleado &gt; 0, días &gt; 0 estricto y dentro del tope de
     * cordura, motivo de 2 a 50 caracteres (el largo de la columna: uno más y el SP reventaría con
     * un truncamiento que el usuario leería como "se rompió el sistema"), fecha obligatoria y no
     * más de un año en el futuro, y que el empleado tenga UNA relación laboral activa. La relación
     * la resuelve el DAO contra la base: no se acepta la que venga en el body, porque mandar la
     * equivocada le movería el saldo a otra persona-relación.
     *
     * <p><b>Duplicado.</b> Un abono idéntico (misma persona, misma relación, misma fecha, mismos
     * días, mismo motivo) se rechaza SIEMPRE con <b>409</b>, confirmado o no — en 184 filas
     * históricas no hay ni uno, así que es el doble toque del teléfono y no un caso real. Mismo
     * día con otro monto u otro motivo sí existe y es legítimo: eso responde <b>400 pidiendo
     * confirmación</b> y pasa con {@code confirmado = true}.
     *
     * <p><b>El SP no devuelve el id generado</b>: se relee por (empleado, relación, fecha) y se
     * devuelve la fila que quedó.
     *
     * @param audUsuarioI quién lo registra, sacado del token y nunca del body
     */
    AbonoDias alta(AbonoDias a, long audUsuarioI, boolean confirmado);

    /**
     * [U] Edición. <b>Sólo días y motivo.</b>
     *
     * <p>El SP pisa {@code codEmpleado} y {@code codRelEmplEmpr} en el UPDATE, así que se relee la
     * fila y se reenvía completa; la fecha tampoco se edita (el legacy deshabilita el calendario
     * cuando el registro ya existe).
     */
    AbonoDias edicion(long codAbonoDias, double diasAbonados, String motivo, long audUsuarioI);

    /**
     * [D] Baja. DELETE físico, archivado por el trigger {@code dad_abonoDias} en
     * {@code trh_abonoDiasEliminado}.
     *
     * <p><b>Esto es lo que el legacy quiso hacer y no hace:</b> su {@code eliminarAbonDia()} llama
     * a {@code registrar()}, que con el id cargado resuelve a ACCION 'U' y dispara un UPDATE con
     * todo en NULL. No corrompe nada sólo porque la FK y el NOT NULL de {@code fecha} lo hacen
     * fallar entero, y porque el botón está oculto. Acá va con ACCION 'D', que es lo que
     * corresponde.
     *
     * @param audUsuarioI quién borra, del token. <b>Se escribe en la fila con una 'U' previa</b>:
     *                    la ACCION 'D' no lo recibe, así que sin ese paso el trigger
     *                    {@code dad_abonoDias} archivaría la fila con el usuario que la CREÓ y la
     *                    bitácora diría que la borró alguien que no fue.
     * @return la fila que se borró
     */
    AbonoDias baja(long codAbonoDias, long codEmpleado, long audUsuarioI);

    // ══════════════════════════════════════════════════════════════════════
    // CARGA COLECTIVA
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Qué pasaría si se cargara el abono a esta lista de gente: una fila por persona, con
     * {@code aplica} y, para quien se saltea, el {@code motivoOmision}.
     *
     * <p><b>No escribe nada.</b> Es el dry-run que alimenta la pantalla de confirmación, el mismo
     * papel que cumple {@code trs_sp_puenteVacacion @ACCION='S'} antes del puente. El legacy tenía
     * un segundo paso de confirmación y es la única barrera que existe hoy antes de escribir N
     * filas: se conserva.
     *
     * <p>Se saltea a quien no tenga exactamente una relación laboral activa y a quien ya tenga un
     * abono ese día — igual que el puente, que se saltea a quien ya tiene el permiso, porque sin
     * eso correr la carga dos veces le duplica los días a todos.
     */
    List<AbonoDias> simularGrupal(AbonoDias cabecera, List<Long> codEmpleados);

    /**
     * Lo aplica. <b>Todo o nada.</b>
     *
     * <p>El legacy insertaba en un bucle sin transacción, contaba las fallas y dejaba media carga
     * escrita ("Problema(s) al registrar N empleado(s)"). Acá va con {@code @Transactional}: si
     * falla la persona 40 de 85 no queda ninguna fila. <b>El mensaje de error tiene que decir eso
     * y no el del legacy</b>, que anunciaba éxitos parciales.
     *
     * <p>Los que la simulación marca como omitidos NO son un error: se saltean y no cuentan. Si
     * no queda nadie a quien aplicar, no se escribe nada y se explica por qué.
     *
     * @return cuántas filas se insertaron
     */
    int aplicarGrupal(AbonoDias cabecera, List<Long> codEmpleados, long audUsuarioI);
}
