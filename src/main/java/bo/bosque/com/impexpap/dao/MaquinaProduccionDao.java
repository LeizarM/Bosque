package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.MaquinaProduccion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MaquinaProduccionDao implements IMaquinaProduccion {

    @Autowired
    JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para listar las maquinas de producción
     *
     * @return
     */
    @Override
    public List<MaquinaProduccion> obtenerMaquina() {


            List<MaquinaProduccion> lstTemp =  new ArrayList<>();

            try{
                lstTemp = this.jdbcTemplate.query("execute p_list_tprod_Maquina @ACCION=?",
                        new Object[]{ "L" },
                        new int[]{  Types.VARCHAR },
                        (rs, rowCount) ->{

                            MaquinaProduccion temp = new MaquinaProduccion();

                            temp.setIdMa(rs.getInt(1));
                            temp.setDescripcion(rs.getString(2));
                            temp.setNumero(rs.getInt(3) );
                            temp.setAudUsuario(rs.getInt(4));

                            return temp;
                        });

            }catch ( BadSqlGrammarException e){
                System.out.println("Error: MaquinaProduccionDao en obtenerMaquina, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
                lstTemp = new ArrayList<>();
                this.jdbcTemplate = null;
            }
            return lstTemp;
        }


}
