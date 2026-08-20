package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ItemSap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ItemSapDao implements IItemSap {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Catalogo de items que se pueden cortar.
     *
     * La ACCION 'A' ya excluye los tipos que no son papel y ordena por
     * descripcion, asi que aqui no se filtra ni se ordena nada.
     *
     * @return
     */
    @Override
    public List<ItemSap> obtenerItemsSap() {

        List<ItemSap> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_ItemSAP @ACCION=?",
                    new Object[] { "A" },
                    new int[] { Types.VARCHAR },
                    (rs, rowCount)->{

                        ItemSap t = new ItemSap();

                        t.setCodItem(rs.getString(1));
                        t.setDatoItem(rs.getString(2));
                        t.setCantidadDisponible(rs.getDouble(3));
                        t.setCodTipo(rs.getInt(4));
                        t.setDatoTipo(rs.getString(5));
                        t.setCodFabricante(rs.getInt(6));
                        t.setDatoFabricante(rs.getString(7));
                        t.setGramaje(rs.getDouble(8));
                        t.setLargo(rs.getDouble(9));
                        t.setAncho(rs.getDouble(10));
                        t.setUtm(rs.getDouble(11));
                        t.setCantHojas(rs.getDouble(12));
                        t.setEmpaque(rs.getString(13));
                        t.setFormato(rs.getString(14));

                        return t;

                    });
        }catch ( DataAccessException e ){
            System.out.println("Error: ItemSapDao en obtenerItemsSap ->" + e.getMessage());
            lstTemp = new ArrayList<>();
        }
        return lstTemp;
    }
}
