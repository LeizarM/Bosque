// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IDependientesJefe.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DependienteCargo;
import bo.bosque.com.impexpap.utils.ListadoConEstado;

public interface IDependientesJefe {

    /**
     * Lista los cargos dependientes del cargo vigente del usuario (jefe de
     * área/gerente), validando en el propio SP que el cargo tenga
     * codNivel &lt;= nivelMaximoJefe (umbral resuelto por el SP desde
     * dbo.tac_configuracion — no se pasa desde Java).
     *
     * @param codUsuario      Usuario que hace la consulta (se resuelve su cargo vigente)
     * @param profundidad     "T" = todo el subárbol, "D" = solo reportes directos
     * @param alcanceSucursal "A" = todas las sucursales, "M" = solo la sucursal del jefe
     * @return filas + {@code autorizado}/{@code error}/{@code errormsg} — revisar
     *         {@code isAutorizado()} antes de usar las filas (una lista vacía
     *         puede significar "no autorizado" o "autorizado sin dependientes")
     */
    ListadoConEstado<DependienteCargo> listarDependientes(
            long codUsuario, String profundidad, String alcanceSucursal);
}
