package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Grupo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IGrupo {

    /** Alta, cambio o baja logica de un grupo. acc: I, U, D. */
    RespuestaSp registrarGrupo(Grupo mb, String acc);

    /** Grupos activos. idGrupo 0 devuelve todos. */
    List<Grupo> obtenerGrupos(long idGrupo);

    /** Grupos que el vendedor todavia no tiene asignados y vigentes. */
    List<Grupo> obtenerGruposAsignables(long idVendedor);

    /** Incluye los dados de baja. */
    List<Grupo> obtenerGruposTodos();
}
