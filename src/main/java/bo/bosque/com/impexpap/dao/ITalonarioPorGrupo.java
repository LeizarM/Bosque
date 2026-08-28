package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TalonarioPorGrupo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Asignacion de tipos a grupos (tmto_talonarioPorGrupo). */
public interface ITalonarioPorGrupo {

    /**
     * Asignar o quitar un tipo de un grupo.
     * Solo admite 'I' y 'D': la tabla tiene PK compuesta y no hay nada que
     * actualizar. Para mover un tipo de grupo, D + I.
     * @param acc Accion ('I', 'D')
     */
    RespuestaSp registrarTalonarioPorGrupo(TalonarioPorGrupo mb, String acc);

    /** Tipos asignados a un grupo. Con codGrupo null devuelve todas las asignaciones. */
    List<TalonarioPorGrupo> listarPorGrupo(Long codGrupo);

    /** Tipos que TODAVIA NO estan en el grupo, para poblar el combo de agregar. */
    List<TalonarioPorGrupo> listarTiposDisponibles(long codGrupo);
}
