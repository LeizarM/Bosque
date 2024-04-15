package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.MaterialIngreso;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;

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
            resp = this.jdbcTemplate.update("execute p_abm_tprod_materialIngreso  @idMi = ?, @idLp = ?, @codArticulo = ?, @descripcion = ?, @pesoKilos = ?, @balanza = ?, @audUsuario = ?, @ACCION=?",
                    ps -> {

                        ps.setInt(1, regMatIng.getIdMi());
                        ps.setInt(2, regMatIng.getIdLp());
                        ps.setString(3, regMatIng.getCodArticulo());
                        ps.setString(4, regMatIng.getDescripcion());
                        ps.setFloat(5, regMatIng.getPesoKilos());
                        ps.setFloat(6, regMatIng.getBalanza());
                        ps.setInt(7, regMatIng.getAudUsuario());
                        ps.setString(8, acc);

                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: MaterialIngresoDao en registrarMaterialIngreso, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;

    }
}
