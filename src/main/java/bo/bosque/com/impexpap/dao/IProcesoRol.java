package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.ConvocarBloqueDto;
import bo.bosque.com.impexpap.dto.GenerarRolDto;

/**
 * Los SPs de PROCESO del módulo — los que mueven muchas filas de una vez y no
 * responden a una tabla sola, así que no tienen ABM propio.
 *
 * <p>Cuatro de los siete no se alcanzan por ningún ABM y por eso están acá.
 * Los otros tres ya tienen puerta de entrada:
 * <ul>
 *   <li>{@code trs_sp_marcarSabadoEspecial} → {@code p_abm_trs_Sabado @ACCION='U'}</li>
 *   <li>{@code trs_sp_programar} → {@code p_abm_trs_Programacion @ACCION='I'}</li>
 *   <li>{@code trs_sp_corregirCelda} → {@code p_abm_trs_Asignacion}</li>
 * </ul>
 *
 * <p><b>No devuelven {@code @error}/{@code @errormsg}</b> como los ABM: cortan con
 * {@code RAISERROR}. El DAO lo traduce a {@code SpBusinessException} para que el
 * front reciba el mismo 400 con el mensaje del SP.
 */
public interface IProcesoRol {

    /**
     * Genera o regenera el rol entero: cabecera, ~52 sábados, participantes y celdas.
     *
     * <p>{@code REGENERAR} rehace <b>sólo</b> las celdas {@code origen='G'}; lo que
     * corrigió RR.HH. a mano ('M') y lo que programó un jefe ('P') queda intacto.
     * Hay que correrlo cada vez que entra o sale gente — nada lo dispara solo.
     *
     * @return el {@code idRol} generado o regenerado
     */
    long generarRol(GenerarRolDto dto);

    /**
     * Vuelve a leer {@code trh_diaNoLaborable} y sincroniza los feriados del rol,
     * en los dos sentidos: marca los que RR.HH. cargó después y desmarca los que
     * borraron.
     *
     * <p>Es el arreglo del caso PUENTE: el feriado cae viernes, RR.HH. declara además
     * el sábado, y el rol ya estaba generado. Sin esto la grilla sigue diciendo que
     * esa gente trabaja.
     *
     * @param soloInformar true = no escribe nada, sólo reporta qué cambiaría
     */
    void refrescarFeriados(long idRol, boolean soloInformar, Long audUsuario);

    /**
     * Convoca o excusa en bloque para el sábado de un evento: una persona, todo el
     * subárbol de un jefe, o todos los de un cargo.
     */
    void convocar(ConvocarBloqueDto dto);

    /**
     * Cruza el rol con las vacaciones, bajas y permisos de RR.HH.
     * ({@code trh_permiso}) y corrige las celdas.
     *
     * <p>Es el mismo caso que los feriados: un dato que vive en otro módulo,
     * que se carga DESPUÉS de generar el rol, y que nada avisa solo. La rotación
     * A/B sabe a quién le toca, pero no sabe quién está de vacaciones.
     *
     * <p>Va en los dos sentidos — marca a quien sacó vacaciones y devuelve a
     * trabajar a quien las anuló— y sólo toca celdas {@code origen='G'}: si
     * alguien decidió a mano que esa persona sí viene, el permiso no lo pisa.
     *
     * @param idSabado     0 = todo el rol; un id = sólo ese día
     * @param soloInformar true = no escribe, sólo devuelve qué cambiaría
     */
    void refrescarPermisos(long idRol, long idSabado, boolean soloInformar,
                           Long audUsuario);

    /**
     * Rehace las celdas de UN sábado sin tocar el resto del año. Respeta las capas
     * igual que {@code REGENERAR}: sólo pisa las celdas {@code origen='G'}.
     */
    void regenerarSabado(long idSabado, Long audUsuario);

    /**
     * Crea el rol de la gestión siguiente, en BORRADOR, heredando la configuración
     * del año que termina. Lo dispara el scheduler del 22 al 31 de diciembre.
     *
     * <p>Tres cosas que no se ven en la firma y hay que saber antes de llamarlo:
     * <ol>
     *   <li><b>Es IDEMPOTENTE y no falla al no hacer nada.</b> Si ya existe
     *       cualquier rol de ese año —lo haya creado el cron, RR.HH. con la varita o
     *       la otra instancia del backend— vuelve sin tocar nada y sin error. Lo
     *       mismo si no existe el rol del año anterior: no inventa el primer rol de
     *       la historia del módulo. La garantía dura es el UNIQUE de
     *       {@code trs_Rol}, no una bitácora del job.</li>
     *   <li><b>Sale en BORRADOR</b>, nunca publicado. Repartir los sábados de un año
     *       entero sin que ningún humano lo mire ya no sería comodidad. Publicar
     *       sigue siendo un acto explícito.</li>
     *   <li><b>El alcance se COPIA del año anterior</b>, no se asume global: si hay
     *       un rol por sucursal, se crea uno por sucursal. El job no toma ninguna
     *       decisión nueva — replica la que ya estaba tomada.</li>
     * </ol>
     *
     * <p>Y lo que hereda cada rol nuevo lo resuelve {@code trs_sp_generarRol} adentro,
     * no este wrapper: las metas, el {@code grupoRotacion} de cada persona y la
     * ventana de sábados de quien RR.HH. había sacado. Por eso un rol creado a mano
     * el 2 de enero sale idéntico al que habría creado el cron.
     *
     * @param anio el año a crear (el que viene), no el que termina
     */
    void generarGestionSiguiente(int anio, Long audUsuario);
}
