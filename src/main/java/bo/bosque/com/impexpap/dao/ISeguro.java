package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Seguro;
import bo.bosque.com.impexpap.utils.Tipos;

import java.util.List;

public interface ISeguro {
 /**
  * obtendra la lista de aseguradoras
  * @return
  */
 List<Seguro> obtenerSeguros ();

 /**
  * registrara/editara/eliminara una aseguradora
  * @param seg
  * @param acc
  * @return
  */
 boolean registroAseguradora (Seguro seg, String acc);
 /**
  * Procedimiento para obtener el tipo de seguro
  * @return
  */
 List<Tipos>listTipoSeguro();
}
