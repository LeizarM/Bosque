package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ItemSap;

import java.util.List;

public interface IItemSap {

    /**
     * Catalogo de items SAP que se pueden cortar, ya depurado por el SP
     * (excluye los tipos que no son papel) y ordenado por descripcion.
     * @return
     */
    List<ItemSap> obtenerItemsSap();

}
