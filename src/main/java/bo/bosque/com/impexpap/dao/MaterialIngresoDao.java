package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.MaterialIngreso;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MaterialIngresoDao implements IMaterialIngreso {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Metodo para registrar el material de entrada
     *
     * @param regMatIng
     * @param acc
     * @return
     */
    public boolean registrarMaterialIngreso( MaterialIngreso regMatIng, String acc ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tprod_materialIngreso  @idMi = ?, @idLp = ?, @codArticulo = ?, @descripcion = ?, @pesoKilos = ?, @balanza = ?, @numImportacion = ? ,@audUsuario = ?, @ACCION=?",
                    ps -> {

                        ps.setInt(1, regMatIng.getIdMi());
                        ps.setInt(2, regMatIng.getIdLp());
                        ps.setString(3, regMatIng.getCodArticulo());
                        ps.setString(4, regMatIng.getDescripcion());
                        ps.setFloat(5, regMatIng.getPesoKilos());
                        ps.setFloat(6, regMatIng.getBalanza());
                        ps.setString(7, regMatIng.getNumImportacion());
                        ps.setInt(8, regMatIng.getAudUsuario());
                        ps.setString(9, acc);

                    });

        }catch ( DataAccessException e ){
            System.out.println("Error: MaterialIngresoDao en registrarMaterialIngreso, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            resp = 0;
        }

        return resp!=0;

    }

    /**
     * Para obtener el material de ingreso de un lote
     *
     * @param idLp
     * @return
     */
    @Override
    public List<MaterialIngreso> obtenerMaterialIngresoXLote( int idLp ) {

        List<MaterialIngreso> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tprod_materialIngreso @idLp=?, @ACCION=?",
                    new Object[] { idLp, "A" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{

                        MaterialIngreso temp = new MaterialIngreso();

                        temp.setIdMi(rs.getInt(1));
                        temp.setIdLp(rs.getInt(2));
                        temp.setCodArticulo(rs.getString(3));
                        temp.setDescripcion(rs.getString(4));
                        temp.setPesoKilos(rs.getFloat(5));
                        temp.setBalanza(rs.getFloat(6));
                        temp.setNumImportacion(rs.getString(7));

                        return temp;

                    });
        }catch ( DataAccessException e ){
            System.out.println("Error: MaterialIngresoDao en obtenerMaterialIngresoXLote, DataAccessException->" + e.getMessage());
            lstTemp = new ArrayList<>();
        }
        return lstTemp;
    }
}
