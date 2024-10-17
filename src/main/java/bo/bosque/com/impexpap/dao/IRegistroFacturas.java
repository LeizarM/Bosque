package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ArticuloPrecioDisponible;
import bo.bosque.com.impexpap.model.RegistroFacturas;
import bo.bosque.com.impexpap.model.RegistroResma;
import bo.bosque.com.impexpap.utils.Tipos;

import java.util.Date;
import java.util.List;

public interface IRegistroFacturas {


    /**
     * Para obtener los as factura de IPX y ESPP
     * @return
     */
    List<RegistroFacturas> obtenerFacturas(Date fecha );


    /**
     * Para registrar un nuevo registro de factura
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarRegistroFacturas(RegistroFacturas mb, String acc );

    /**
     * Obtendra los tipos de facturas
     * @return
     */
    List<Tipos> lstTipoFactura ();

    /**
     * Listara las empresas para el registro de facturas
     * @return
     */
    List<RegistroFacturas> lstEmpresas();

    /**
     * Listara las facturas registradas para una fecha especifica
     * @param fecha
     * @return
     */
    List<RegistroFacturas> lstFacturasRegistradas( Date fechaSistema );
}
