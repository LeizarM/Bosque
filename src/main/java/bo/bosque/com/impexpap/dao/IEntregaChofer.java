package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.EntregaChofer;

import java.util.Date;
import java.util.List;

public interface IEntregaChofer {

    /**
     * Registrar entrega de chofer
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarEntregaChofer( EntregaChofer mb, String acc );

    /**
     * Listar entregas por empleado
     * @param codEmpleado
     * @return
     */
    List<EntregaChofer> listarEntregasXEmpleado(  int codEmpleado );

    /**
     * Listar entregas por chofer
     * @param docDate
     * @return
     */
    List<EntregaChofer> listarEntregasXChofer(  String fechaEntrega, int codEmpleado);

    /**
     * Listar choferes
     * @return
     */
    List<EntregaChofer> lstChoferes();
}
