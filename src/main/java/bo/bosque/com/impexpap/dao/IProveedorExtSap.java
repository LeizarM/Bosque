package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ProveedorExtSap;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Contrato de acceso a datos del catalogo <b>tpr_proveedorExtSap</b>.
 *
 * <p>Todo pasa por procedimientos almacenados: {@code p_abm_proveedorExtSap}
 * para las escrituras y {@code p_list_proveedorExtSap} para las consultas.
 */
public interface IProveedorExtSap {

    /**
     * Registra, actualiza o elimina un proveedor externo SAP.
     *
     * @param mb  datos del proveedor; en el alta {@code idProveedorSap} debe ir
     *            en null para que trabaje el IDENTITY
     * @param acc ACCION del proc: "I" insertar, "U" actualizar, "D" eliminar
     * @return respuesta del proc; en el alta trae el id nuevo en
     *         {@code getIdGenerado()}
     */
    RespuestaSp registrarProveedorExtSap(ProveedorExtSap mb, String acc);

    /**
     * Todos los proveedores externos SAP, sin filtro.
     */
    List<ProveedorExtSap> listarProveedores();

    /**
     * Proveedores externos SAP filtrados. Cada filtro en null significa
     * "sin filtro" (es como lo entiende el proc).
     *
     * @param idProveedorSap  filtro por PK
     * @param codProvExtSap   filtro por codigo SAP; es texto, no numero
     * @param proveedorExtSap filtro por nombre, coincidencia exacta
     * @param audUsuario      filtro por usuario de auditoria
     */
    List<ProveedorExtSap> listarProveedores(Long idProveedorSap, String codProvExtSap,
                                            String proveedorExtSap, Long audUsuario);

    /**
     * Un proveedor por su PK.
     *
     * @return la fila encontrada, o null si el id no existe
     */
    ProveedorExtSap obtenerPorId(long idProveedorSap);
}
