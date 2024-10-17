package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.MaterialSalida;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

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
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;

    }
}
