package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Anticipo;
import bo.bosque.com.impexpap.model.Multas;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IMultas {

    // El listado sí nos debe devolver la lista de entidades Multas para la grilla
    List<Multas> listarMultas(Multas m);

    // Para las acciones transaccionales (Generar, Modificar), devolvemos RespuestaSp
    RespuestaSp generarMultas(Multas m , String acc);
}
