package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.PostergacionDto;
import bo.bosque.com.impexpap.model.ChPostergacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/** Acceso a {@code tch_chPostergacion} por {@code p_list_ChPostergacion} y {@code p_abm_ChPostergacion}. */
public interface IChPostergacion {

    /** {@code p_list_ChPostergacion} 'L' por {@code codCheque}: por codigo de postergacion, numeradas 1..n. */
    List<PostergacionDto> listarPorCheque(int codCheque);

    /** {@code p_list_ChPostergacion} 'L' por {@code codPostergacion}: la postergacion o {@code null} si no existe. */
    ChPostergacion obtener(int codPostergacion);

    /**
     * {@code p_abm_ChPostergacion} 'I'. {@code idGenerado} = el nuevo {@code codPostergacion}. El procedimiento guarda
     * {@code nombreArchivo = ''} sin importar lo que se mande (como el legacy). La rama 'U' no se usa desde ninguna pantalla.
     */
    RespuestaSp registrar(ChPostergacion postergacion);

    /** {@code p_abm_ChPostergacion} 'D' por {@code codPostergacion}. No toca el archivo PDF. */
    RespuestaSp eliminar(int codPostergacion, int audUsuario);
}
