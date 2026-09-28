package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ArticuloPrecioDisponible;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


 @Repository
public class ArticuloPrecioDisponibleDao implements  IArticuloPrecioDisponible {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para obtener los articulos de IPX y ESPP
     *
     * @return
     */
    @Override
    public List<ArticuloPrecioDisponible> obtenerArticulosIPXyESPP( int codCiudad ) {


        List<ArticuloPrecioDisponible> lstTemp = new ArrayList<ArticuloPrecioDisponible>();
        try {
            lstTemp = this.jdbcTemplate.query("execute p_list_articuloPrecioDisponible  @codCiudad = ? ,@ACCION=?",
                    new Object[]{ codCiudad, "E" },
                    new int[]{Types.INTEGER, Types.VARCHAR},
                    // Por nombre, no por posición: si el SP agrega o reordena
                    // columnas, el mapeo no se corre.
                    (rs, rowNum) -> {
                        ArticuloPrecioDisponible temp = new ArticuloPrecioDisponible();

                        temp.setCodArticulo(rs.getString("codArticulo"));
                        temp.setDatoArt(rs.getString("datoArt"));
                        temp.setListaPrecio(rs.getInt("listaPrecio"));
                        temp.setPrecio(rs.getFloat("precio"));
                        temp.setMoneda(rs.getString("moneda"));
                        temp.setCodigoFamilia(rs.getInt("codigoFamilia"));
                        temp.setDisponible(rs.getInt("disponible"));
                        temp.setUnidadMedida(rs.getString("unidadMedida"));
                        temp.setCodCiudad(rs.getInt("codCiudad"));
                        temp.setCodGrpFamiliaSap(rs.getInt("codGrpFamiliaSap"));
                        temp.setRuta(rs.getString("ruta"));
                        temp.setDb(rs.getString("db"));
                        temp.setCondicionPrecio(rs.getString("condicionPrecio"));
                        temp.setUtm(rs.getFloat("utm"));
                        // IPX + ESP juntas (y todas las ciudades si no se envía codCiudad).
                        temp.setDisponibleTotal(rs.getInt("disponibleTotal"));

                        return temp;
                    });
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: obtenerArticulosIPXyESPP en ArticuloPrecioDisponibleDao, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
        }
        return lstTemp;
    }

     /**
      * Para obtener los almacenes por item y su disponibilidad
      * @param codArticulo
      * @param codCiudad
      * @return
      */
     @Override
     public List<ArticuloPrecioDisponible> obtenerAlmacenXItem( String codArticulo, int codCiudad ) {


         List<ArticuloPrecioDisponible> lstTemp = new ArrayList<ArticuloPrecioDisponible>();
         try {
             lstTemp = this.jdbcTemplate.query("execute p_list_articuloPrecioDisponible @codArticulo=?, @codCiudad=? ,@ACCION=?",
                     new Object[]{ codArticulo, codCiudad ,"F" },
                     new int[]{ Types.VARCHAR, Types.INTEGER, Types.VARCHAR},
                     (rs, rowNum) -> {
                         ArticuloPrecioDisponible temp = new ArticuloPrecioDisponible();

                         temp.setCodArticulo(rs.getString(1));
                         temp.setDatoArt(rs.getString(2));
                         temp.setWhsCode(rs.getString(3));
                         temp.setWhsName(rs.getString(4));
                         temp.setCodCiudad(rs.getInt(5));
                         temp.setDisponible(rs.getInt(6));
                         temp.setDb(rs.getString(7));
                         temp.setCiudad(rs.getString(8));

                         return temp;
                     });
         } catch (BadSqlGrammarException e) {
             System.out.println("Error: obtenerAlmacenXItem en ArticuloPrecioDisponibleDao, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
             lstTemp = new ArrayList<>();
         }
         return lstTemp;
     }

}
