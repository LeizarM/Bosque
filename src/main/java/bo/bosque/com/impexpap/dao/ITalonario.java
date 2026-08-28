package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Talonario;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

/** Talonarios (tmto_talonario). */
public interface ITalonario {

    /**
     * Registrar, actualizar o eliminar un talonario.
     *
     * El INSERT es de dos pasos en una sola transaccion: inserta el talonario
     * y su evento inicial codEstado=1 (Adquirido). Devuelve el id en
     * {@code idGenerado}.
     *
     * El UPDATE ignora a proposito costoBs, numeracionInicial, numeracionFinal
     * y codEmpresa: cambiar el rango de folios de un talonario que ya circulo
     * invalidaria su historial.
     *
     * El DELETE rebota si el talonario ya tiene movimientos.
     *
     * @param acc Accion ('I', 'U', 'D')
     */
    RespuestaSp registrarTalonario(Talonario mb, String acc);

    /** Un talonario por su id, con su estado ya calculado. */
    List<Talonario> obtenerTalonario(long codTalonario);

    /**
     * Listado con estado calculado. Todos los filtros son opcionales: pasar
     * null para no filtrar.
     * @param codEstadoActual 1 Adquirido, 2 Entregado, 3 Devuelto, 4 Cerrado
     */
    List<Talonario> listarTalonario(Long codTipoRecibo, Long codEmpresa,
                                    Long codGrupo, Integer codEstadoActual,
                                    Date fechaDesde, Date fechaHasta,
                                    Boolean incluirCerrados);

    /**
     * Talonarios listos para entregar o reentregar: nunca cerrados y que no
     * estan en poder de nadie. Filtro por grupo opcional.
     */
    List<Talonario> listarDisponibles(Long codGrupo);

    /**
     * Busca por nroTalonario, que es unico a nivel global (tmto_talonario_uq).
     * Devuelve lista vacia si no existe.
     *
     * Lo usa la simulacion del alta masiva para marcar los duplicados ANTES
     * de que el usuario confirme. El alta en si no lo necesita: el SP ya
     * valida la unicidad (error 23) y el lote es transaccional.
     */
    List<Talonario> buscarPorNroTalonario(String nroTalonario);
}
