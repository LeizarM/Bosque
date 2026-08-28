package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TalonarioGrupo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Grupos de tipos de talonario (tmto_talonarioGrupo). */
public interface ITalonarioGrupo {

    /**
     * Registrar, actualizar o eliminar un grupo.
     * El DELETE rebota si el grupo tiene tipos asignados.
     * @param acc Accion ('I', 'U', 'D')
     */
    RespuestaSp registrarTalonarioGrupo(TalonarioGrupo mb, String acc);

    /** Un grupo por su id. Lista vacia si no existe. */
    List<TalonarioGrupo> obtenerTalonarioGrupo(long codGrupo);

    /** Todos los grupos, con su conteo de tipos y talonarios. */
    List<TalonarioGrupo> listarTalonarioGrupo();
}
