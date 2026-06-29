package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Bono;
import bo.bosque.com.impexpap.model.BonoEmpleado;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IBono {
    List<Bono> listarBono(Bono b);
    List<BonoEmpleado> listarBonoEmpleado(BonoEmpleado be);
    RespuestaSp abmBono(Bono b, String acc);
}
