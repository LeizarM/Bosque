package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Asignacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * La celda: una persona × un sábado. Es la tabla grande del módulo — una fila
 * por cada cuadrito ocupado de la matriz del Excel.
 *
 * <p>OJO con lo que NO hay: si a alguien le toca LIBRE ese sábado, NO hay fila.
 * El libre es la ausencia de la fila, no un estado. Por eso el rol de 29
 * personas × 52 sábados no da 1.508 filas por definición — da sólo las ocupadas.
 *
 * <p>No hay un SP que devuelva la matriz pivoteada: las columnas serían
 * dinámicas (s01…s53) y cambian de año a año. La grilla se arma en el front
 * agrupando el listado [L] por {@code idParticipante} y {@code fecha}.
 *
 * <p>SPs: {@code p_abm_trs_Asignacion} · {@code p_list_trs_Asignacion}.
 */
public interface IAsignacion {

    /**
     * Corrige UNA celda a mano. Delega en {@code trs_sp_corregirCelda}, que
     * escribe con {@code origen='M'} (manual) — y ese origen es lo que hace que
     * la corrección SOBREVIVA a una regeneración, porque regenerar sólo borra
     * las celdas 'G'.
     *
     * <p>'I' y 'U' son equivalentes: si esa persona ya tiene celda en ese sábado
     * la pisa, porque el UNIQUE (idParticipante, idSabado) no deja tener dos.
     * 'D' libera la celda — borra la fila, que es como se representa el libre.
     *
     * @param asignacion Datos de la celda
     * @param acc        'I' | 'U' | 'D'
     */
    RespuestaSp registrarAsignacion(Asignacion asignacion, String acc);

    /**
     * [L] Las celdas en plano, con el nombre y la fecha ya resueltos.
     * Es la fuente de la grilla y también del export.
     * @param idRol    0 = todos
     * @param idSabado 0 = todos los sábados del rol
     */
    List<Asignacion> obtenerAsignaciones(long idRol, long idSabado);

    /** [L] Una celda por su id. */
    Asignacion obtenerAsignacionPorId(long idAsignacion);

    /**
     * [M] La MEZCLA por capa y estado: cuántas celdas puso el generador ('G'),
     * cuántas un jefe ('P') y cuántas una corrección manual ('M').
     *
     * <p>Al leerla, cuidado: 'G' quiere decir "la escribió el generador", NO
     * "no la decidió nadie" — las celdas del evento y de la convocatoria también
     * son G, porque se derivan de esas tablas y se rehacen en cada regeneración.
     *
     * <p>Son agregados: de la fila sólo vienen {@code origen}, {@code queEs},
     * {@code estado}, {@code nombre} y {@code celdas}.
     */
    List<Asignacion> obtenerMezclaPorOrigen(long idRol);

    /**
     * [A] Cuánto resuelve el sistema solo: una sola fila con {@code celdasTotales},
     * {@code conDecisionHumana} y {@code generadasSolas}. Es la métrica de que el
     * default está bien calibrado — si "conDecisionHumana" crece, la regla por
     * defecto no representa cómo se trabaja de verdad.
     */
    Asignacion obtenerAutomatizacion(long idRol);
}
