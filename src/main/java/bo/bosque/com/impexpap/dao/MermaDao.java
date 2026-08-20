package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Merma;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MermaDao implements IMerma {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Para registrar la merma
     * @param regMerma
     * @param acc
     * @return
     */
    public boolean registrarMerma( Merma regMerma, String acc ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tprod_merma  @idMe = ?, @idLp = ?, @codArticulo = ?, @descripcion = ?, @peso = ?, @audUsuario = ?, @ACCION = ?",
                    ps -> {

                        ps.setInt(1, regMerma.getIdMe());
                        ps.setInt(2, regMerma.getIdLp());
                        ps.setString(3, regMerma.getCodArticulo());
                        ps.setString(4, regMerma.getDescripcion());
                        ps.setFloat(5, regMerma.getPeso());
                        ps.setInt(6, regMerma.getAudUsuario());
                        ps.setString(7, acc);

                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: MermaDao en registrarMerma, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            resp = 0;
        }

        return resp!=0;
    }

    /**
     * Para obtener las mermas de un lote
     *
     * @param idLp
     * @return
     */
    @Override
    public List<Merma> obtenerMermaXLote( int idLp ) {

        List<Merma> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tprod_merma @idLp=?, @ACCION=?",
                    new Object[] { idLp, "A" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{

                        Merma temp = new Merma();

                        temp.setIdMe(rs.getInt(1));
                        temp.setIdLp(rs.getInt(2));
                        temp.setCodArticulo(rs.getString(3));
                        temp.setDescripcion(rs.getString(4));
                        temp.setPeso(rs.getFloat(5));

                        return temp;

                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: MermaDao en obtenerMermaXLote, DataAccessException->" + e.getMessage());
            lstTemp = new ArrayList<>();
        }
        return lstTemp;
    }
}
