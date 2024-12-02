package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.PrestamoChofer;

import java.util.List;

public interface IPrestamoChofer {


    boolean registrarPrestamoChofer(PrestamoChofer mb, String acc );

    List<PrestamoChofer> lstSolicitudes(int codSucursal, int codEmpleado );

}
