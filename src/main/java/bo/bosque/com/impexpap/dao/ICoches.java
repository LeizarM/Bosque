// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ICoches.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CocheDelDia;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ICoches {
    /** [D] La lista de hoy de la sucursal de {@code codEmpleado}, compartida (archivo SQL 96). */
    List<CocheDelDia> listarDelDia(long idTarRuti, long codEmpleado, long audUsuario);

    /** [M] El SP rechaza un auto de otra sucursal o de una lista de otro día. */
    RespuestaSp marcarLlegada(long idCo, int llego, String obs, long codEmpleado, long audUsuario);
}
