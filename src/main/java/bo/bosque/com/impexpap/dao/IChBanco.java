package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.model.ChBanco;



public interface IChBanco {

	/*******************************************************
     *  Funcion de devuelve el listado de bancos de la DB
     * @return LinkedList
     ********************************************************/
    List<ChBanco> listBancos() ;
    
}
