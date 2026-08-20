package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.MaterialSalida;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MaterialSalidaDao implements IMaterialSalida {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;



    /**
     * Metodo para registrar material ingreso
     *
     * @param regMatSal
     * @param acc
     * @return
     */
    @Override
    public boolean registrarMaterialSalida(MaterialSalida regMatSal, String acc) {


        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tprod_materialSalida  @idMs = ?, @idLp = ?, @codArticulo = ?, @descripcion = ?, @nroPaleta = ?, @pesoResma = ?, @pesoPaleta = ?" +
                                                ", @pesoMaterial = ?, @cantidadResma = ?, @cantidadHojas = ?, @audUsuario = ?, @ACCION = ?",
                    ps -> {

                        ps.setInt(1, regMatSal.getIdMs());
                        ps.setInt(2, regMatSal.getIdLp());
                        ps.setString(3, regMatSal.getCodArticulo());
                        ps.setString(4, regMatSal.getDescripcion());
                        ps.setInt(5, regMatSal.getNroPaleta());
                        ps.setFloat(6, regMatSal.getPesoResma());
                        ps.setFloat(7, regMatSal.getPesoPaleta());
                        ps.setFloat(8, regMatSal.getPesoMaterial());
                        ps.setInt(9, regMatSal.getCantidadResma());
                        ps.setFloat(10, regMatSal.getCantidadHojas());
                        ps.setInt(11, regMatSal.getAudUsuario());
                        ps.setString(12, acc);

                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: MaterialSalidaDao en registrarMaterialSalida, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            resp = 0;
        }

        return resp!=0;

    }

    /**
     * Para obtener el material de salida de un lote
     *
     * @param idLp
     * @return
     */
    @Override
    public List<MaterialSalida> obtenerMaterialSalidaXLote( int idLp ) {

        List<MaterialSalida> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tprod_materialSalida @idLp=?, @ACCION=?",
                    new Object[] { idLp, "A" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{

                        MaterialSalida temp = new MaterialSalida();

                        temp.setIdMs(rs.getInt(1));
                        temp.setIdLp(rs.getInt(2));
                        temp.setCodArticulo(rs.getString(3));
                        temp.setDescripcion(rs.getString(4));
                        temp.setNroPaleta(rs.getInt(5));
                        temp.setPesoResma(rs.getFloat(6));
                        temp.setPesoPaleta(rs.getFloat(7));
                        temp.setPesoMaterial(rs.getFloat(8));
                        temp.setCantidadResma(rs.getInt(9));
                        temp.setCantidadHojas(rs.getInt(10));

                        return temp;

                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: MaterialSalidaDao en obtenerMaterialSalidaXLote, DataAccessException->" + e.getMessage());
            lstTemp = new ArrayList<>();
        }
        return lstTemp;
    }
}
