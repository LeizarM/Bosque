package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.model.ChAccion;

public interface IChAccion {

	/********
     * Funcion de devuelve el listado de accions de la DB
     * @param codCheque
     * @return LinkedList
     ********/
    List<ChAccion> listAccions(int codCheque) ;
    
}
