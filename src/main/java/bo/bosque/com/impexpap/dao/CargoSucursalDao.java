package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CargoSucursal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CargoSucursalDao implements ICargoSucursal {

    /**
     * El DataSource de la conexion
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento que obtendra los cargos por sucursal
     * @param codSucursal
     * @return
     */
    public List<CargoSucursal> obtenerSucursalesXEmpresa( int codSucursal ) {

        List<CargoSucursal> lstTemp = new ArrayList<>();

        try{

            lstTemp = this.jdbcTemplate.query("execute p_list_Cargo_sucursal @codSucursal=?, @ACCION=?",
                    new Object[]{  codSucursal, "A" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{
                        CargoSucursal temp = new CargoSucursal();

                        temp.setCodCargoSucursal(rs.getInt(1));
                        temp.setCodSucursal(rs.getInt(2));
                        temp.setCodCargo(rs.getInt(3));
                        temp.getSucursal().getEmpresa().setCodEmpresa(rs.getInt(4));
                        temp.getSucursal().setNombre(rs.getString(5));
                        temp.setDatoCargo(rs.getString(6));
                        temp.getSucursal().getEmpresa().setNombre(rs.getString(7));
                        return temp;
                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: CargoSucursalDao en obtenerSucursalesXEmpresa, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return lstTemp;
    }
}
