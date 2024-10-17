package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.GrupoProduccion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GrupoProduccionDao implements  IGrupoProduccion {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para obtener los grupos de produccion por maquina
     *
     * @return
     */
    @Override
    public List<GrupoProduccion> obtenerGrupoProduccion() {

        List<GrupoProduccion> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tprod_Grupo @ACCION=?",
                    new Object[] { "L" },
                    new int[] { Types.VARCHAR },
                    (rs, rowCount)->{

                        GrupoProduccion temp = new GrupoProduccion();

                        temp.setIdGrupo(rs.getInt(1));
                        temp.setGrupo(rs.getString(2));
                        temp.setDescripcion(rs.getString(3));
                        temp.setAudUsuario(rs.getInt(4));

                        return temp;

                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: GrupoProduccionDao en obtenerGrupoProduccion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
