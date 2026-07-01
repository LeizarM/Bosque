package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DiaNoLaborable;
import bo.bosque.com.impexpap.model.SolicitudPermiso;

import java.util.List;

public interface IDiaNoLaborable {
    /** Listar solicitudes de vacion pendientes individuales*/
    List<DiaNoLaborable> obtenerFeriadosPorEmpleado(DiaNoLaborable filtro);
}
