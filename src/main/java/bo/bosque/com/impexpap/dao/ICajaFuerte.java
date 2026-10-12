// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ICajaFuerte.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.RegistrarCajaFuerteRequest;
import bo.bosque.com.impexpap.utils.RespuestaSp;

public interface ICajaFuerte {
    /** [R] La sucursal sale del cargo vigente de {@code codEmpleado} (archivo SQL 96). */
    RespuestaSp registrar(RegistrarCajaFuerteRequest req, long idTarRuti, long codEmpleado, long audUsuario);
}
