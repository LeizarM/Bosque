package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.AfiliacionSeguro;

public interface IAfiliacionSeguro {
    /**
     * OBTENDRA LA AFILIACION AL SEGURO DE UN EMPLEADO
     * @param codEmpleado
     * @return
     */
    AfiliacionSeguro obtenerAfiliacionSeguro (int codEmpleado);

    /**
     * REGISTRARA UNA AFILIACION PARA UN EMPLEADO
     * @param afSeg
     * @param acc
     * @return
     */
    boolean afiliarSeguroEmpleado (AfiliacionSeguro afSeg, String acc);
}
