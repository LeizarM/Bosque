// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ICoches.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CocheDelDia;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ICoches {
    List<CocheDelDia> listarDelDia(long idTarRuti, long idBitTarea, long audUsuario);

    RespuestaSp marcarLlegada(long idCo, int llego, String obs, long audUsuario);
}
