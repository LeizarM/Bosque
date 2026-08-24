package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Vendedor;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IVendedor {

    /** Alta, cambio o baja logica. acc: I, U, D. */
    RespuestaSp registrarVendedor(Vendedor mb, String acc);

    /** Vendedores activos con sus codigos por empresa. 0 devuelve todos. */
    List<Vendedor> obtenerVendedores(long idVendedor);

    /** Vendedores de una empresa SAP. bd 0 devuelve todas. */
    List<Vendedor> obtenerVendedoresPorEmpresa(int bd);

    /** Incluye los dados de baja. */
    List<Vendedor> obtenerVendedoresTodos();
}
