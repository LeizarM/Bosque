package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SolicitudChofer;

import java.util.List;

public interface ISolicitudChofer {


    boolean registrarSolicitudChofer( SolicitudChofer mb, String acc );

    List<SolicitudChofer> lstSolicitudesXEmpleado( int codEmpleado );

    List<SolicitudChofer> lstCoches();

}
