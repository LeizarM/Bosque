package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Precio;
import bo.bosque.com.impexpap.model.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PrecioDao implements  IPrecio {


    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Para registrar el precio
     * @param precio
     * @param acc
     * @return
     */
    @Override
    public boolean registrarPrecio(Precio precio, String acc) {
        return false;
    }

    /**
     * Para Listar las precios en toneladas actuales para poder calcular
     * @param codigoFamilia
     * @return
     */
    public List<Precio> listPrecioToneladasActuales( int codigoFamilia ) {

        List<Precio> lstTemp = new ArrayList<>();
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_precio @codigoFamilia=?, @ACCION=?",
                    new Object[] { codigoFamilia, "D" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Precio temp = new Precio();

                        temp.setFila( rowNum + 1 );
                        temp.setCodigoFamilia( rs.getInt(1));
                        temp.setIdPresentacion( rs.getInt(2 ) );
                        temp.setNombreProveedor( rs.getString(3));
                        temp.setNombreFamilia( rs.getString(4));
                        temp.setFormato( rs.getString(5));
                        temp.setGramaje( rs.getString(6));
                        temp.setColor( rs.getString(7));
                        temp.setIdSucursal(rs.getInt(8));
                        temp.setNombreSucursal(rs.getString(9));
                        temp.setIdClasificacion(rs.getInt(10));
                        temp.setNombrePrecio(rs.getString(11));
                        temp.setIdPrecio( rs.getInt(12));
                        temp.setVpp( rs.getInt(13));
                        temp.setPorcentaje( rs.getFloat(14) );
                        temp.setListNum(  rs.getInt(15));
                        temp.setPrecio( rs.getFloat(16));
                        temp.setPrecioNew(rs.getFloat(17));
                        temp.setIva( rs.getFloat(18));
                        temp.setIt( rs.getFloat(19));
                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: listPrecioToneladasActuales en PrecioDao, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;

    }


}
