package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.model.TipoTelefono;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/** Mantenimiento de {@code trh_tipoTelefono} por {@code p_list_TipoTelefono} y {@code p_abm_TipoTelefono}. */
public interface ITipoTelefono {

    /** {@code p_list_TipoTelefono 'P'}: todos los tipos con cuántos teléfonos usa cada uno. */
    List<TipoTelefono> listar();

    /** {@code p_abm_TipoTelefono} 'I' (codTipoTel 0) o 'U'. Las reglas y el mensaje los pone el SP. */
    RespuestaSp registrar(TipoTelefono tipo);

    /** {@code p_abm_TipoTelefono} 'D'. El SP rechaza un tipo que usan teléfonos registrados. */
    RespuestaSp eliminar(long codTipoTel, int audUsuario);
}
