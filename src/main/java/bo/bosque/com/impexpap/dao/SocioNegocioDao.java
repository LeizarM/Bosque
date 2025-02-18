package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SocioNegocio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SocioNegocioDao implements  ISocionegocio {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para obtener Socio Negocio
     *
     * @param codEmpresa
     * @return
     */
    @Override
    public List<SocioNegocio> obtenerSocioNegocio(int codEmpresa) {


        List<SocioNegocio> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_SocioNegocio  @codEmpresa=?, @ACCION=?",
                    new Object[]{ codEmpresa, "A" },
                    new int[]{ Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) ->{
                        SocioNegocio temp = new SocioNegocio();

                        temp.setCodCliente( rs.getString(1) );
                        temp.setNombreCompleto(rs.getString(2) );
                        temp.setRazonSocial( rs.getString(3) );


                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: SocioNegocioDao en obtenerSocioNegocio, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;



    }
}
