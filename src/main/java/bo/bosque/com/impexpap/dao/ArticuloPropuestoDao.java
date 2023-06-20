package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ArticuloPropuesto;
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
public class ArticuloPropuestoDao implements IArticuloPropuesto {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el Articulo propuesto
     *
     * @param articuloPropuesto
     * @param acc
     * @return
     */
    @Override
    public boolean registrarArticuloPropuesto(ArticuloPropuesto articuloPropuesto, String acc) {

        int resp;
        try {
            resp = this.jdbcTemplate.update("execute p_abm_ArticuloProp @idArticulo=?, @idPropuesta=?, @codArticulo=?, @codigoFamilia=?, @datoArticulo=?, @stock=?, @utm=?, @audUsuario=?, @ACCION=?",
                    ps ->{
                        ps.setInt(1, articuloPropuesto.getIdArticulo());
                        ps.setInt(2, articuloPropuesto.getIdPropuesta());
                        ps.setString(3, articuloPropuesto.getCodArticulo());
                        ps.setInt(4, articuloPropuesto.getCodigoFamilia());
                        ps.setString(5, articuloPropuesto.getDatoArticulo());
                        ps.setFloat(6, articuloPropuesto.getStock());
                        ps.setFloat(7,articuloPropuesto.getUtm());
                        ps.setInt(8, articuloPropuesto.getAudUsuario());
                        ps.setString(9, acc);
                    });

        }catch (BadSqlGrammarException e){
            System.out.println("Error: ArticuloPropuestoDao en registrarArticuloPropuesto, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }


    /**
     * Metodo para listar los articulos por familia
     * @param codCad
     * @return
     */
    public List<ArticuloPropuesto> listarArticulosXFamilia(String codCad) {
        List<ArticuloPropuesto> lstTemp = new ArrayList<ArticuloPropuesto>();
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_ArticuloProp  @codCad=? ,@ACCION=?",
                    new Object[] { codCad, "E" },
                    new int[] { Types.VARCHAR, Types.VARCHAR },
                    (rs, rowNum) -> {
                        ArticuloPropuesto temp = new ArticuloPropuesto();

                        temp.setFila( rowNum + 1);
                        temp.setFilaCod( rs.getInt(1));
                        temp.setCodArticulo( rs.getString(2 ) );
                        temp.setCodigoFamilia(rs.getInt(3));
                        temp.setDatoArticulo(rs.getString(4));
                        temp.setStock( rs.getFloat(5) );
                        temp.setUtm(rs.getFloat(6));

                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: listarArticulosXFamilia en ArticuloPropuestoDao, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }


}
