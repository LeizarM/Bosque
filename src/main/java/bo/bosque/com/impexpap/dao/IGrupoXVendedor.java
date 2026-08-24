package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.GrupoXVendedor;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IGrupoXVendedor {

    /** Asignar, modificar o cerrar la vigencia. acc: I, U, D. */
    RespuestaSp registrarGrupoXVendedor(GrupoXVendedor mb, String acc);

    /** Una asignacion puntual. */
    List<GrupoXVendedor> obtenerPorId(long idGrpVen);

    /** Todas las asignaciones de un vendedor, vigentes o no. */
    List<GrupoXVendedor> obtenerPorVendedor(long idVendedor);

    /** Todas las asignaciones vigentes a la fecha. */
    List<GrupoXVendedor> obtenerVigentes();
}
