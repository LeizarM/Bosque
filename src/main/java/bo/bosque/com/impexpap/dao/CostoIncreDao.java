package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CostoIncre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CostoIncreDao implements  ICostoIncre {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Para registrar el costo incremento o costo de flete de transporte
     *
     * @param costoIncre
     * @param acc
     * @return
     */
    @Override
    public boolean registrarCostoIncre(CostoIncre costoIncre, String acc) {

        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_costoIncre @idIncre=?, @codSucursal=?, @idPropuesta=?, @valor=?,@audUsuario=?,  @ACCION=?",
                    ps ->{
                        ps.setInt(1, costoIncre.getIdIncre());
                        ps.setInt(2, costoIncre.getCodSucursal());
                        ps.setInt(3, costoIncre.getIdPropuesta());
                        ps.setInt(4, costoIncre.getValor());
                        ps.setInt(5, costoIncre.getAudUsuario());
                        ps.setString(6, acc);
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: CostoIncreDao en registrarCostoIncre, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }

    /**
     * Listara costo de flete de transporte
     * @return
     */
    public List<CostoIncre> costoTransporteCiudad() {

        List<CostoIncre> lstTemp = new ArrayList<>();

        try{
            lstTemp =  this.jdbcTemplate.query("execute p_list_costoIncre @ACCION=?",
                        new Object[] { "C" },
                        new int[] {Types.VARCHAR},
                        ( rs, rowNum ) -> {

                            CostoIncre temp = new CostoIncre();

                            temp.setCodSucursal(rs.getInt(1));
                            temp.setCodCiudad(rs.getInt(2));
                            temp.setNombreSucursal(rs.getString(3));
                            temp.setNombreCiudad(rs.getString(4));
                            temp.setValor(rs.getInt(5));


                            return temp;
                        });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: costoTransporteCiudad en CostoIncreDao, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return lstTemp;
    }
}
