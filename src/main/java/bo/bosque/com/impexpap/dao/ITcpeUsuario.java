package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeUsuario;

/**
 * CRUD de {@code tcpe_usuario}: {@code p_list_tcpe_Usuario} y {@code p_abm_tcpe_Usuario}.
 * El comportamiento común esta en {@link ITcpeCrud}; esta interfaz existe por la regla N:N del módulo.
 */
public interface ITcpeUsuario extends ITcpeCrud<TcpeUsuario> {
}
