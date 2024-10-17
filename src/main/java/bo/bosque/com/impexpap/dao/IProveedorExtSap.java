package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ProveedorExtSap;

public interface IProveedorExtSap {

    /**
     * Para registrar el Proveedor Externo Sap
     * @param prodProveedorExtSap
     * @param acc
     * @return
     */
    boolean registrarProveedorExtSap( ProveedorExtSap prodProveedorExtSap, String acc );
}
