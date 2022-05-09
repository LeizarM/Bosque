package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Sucursal;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SucursalDao implements  ISucursal {

    /**
     * El Datasource
     */
    JdbcTemplate jdbcTemplate;

    /**
     * Obtendra las sucursales por Empresa
     * @param codEmpresa
     * @return
     */
    public List<Sucursal> obtenerSucursalesXEmpresa(int codEmpresa) {
        List<Sucursal> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_sucursal @codEmpresa=?, @ACCION=?",
                    new Object[]{ codEmpresa, "L" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{
                        Sucursal temp = new Sucursal();

                        temp.setCodSucursal(rs.getInt(1));
                        temp.setNombre(rs.getString(2));
                        temp.setCodEmpresa(rs.getInt(3));
                        temp.getEmpresa().setNombre(rs.getString(4));
                        temp.setCodCiudad(rs.getInt(5));
                        temp.setNombreCiudad(rs.getString(6));
                        temp.setAudUsuarioI(rs.getInt(7));

                        return temp;

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: SucursalDao en obtenerSucursalesXEmpresa, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return lstTemp;
    }
}
