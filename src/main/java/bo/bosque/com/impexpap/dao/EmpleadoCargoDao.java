package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.EmpleadoCargo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;

@Repository
public class EmpleadoCargoDao implements IEmpleadoCargo{

    /**
     * El DataSource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento para el registro de informacion
     * @param empCar
     * @param acc
     * @return
     */
    public boolean registrarEmpleadoCargo(EmpleadoCargo empCar, String acc) {

        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_EmpleadoCargo @codEmpleado=?, @codCargoSucursal=?, @codCargoSucPlanilla=?, @fechaInicio=?, @audUsuarioI=?, @ACCION=?",
                    ps->{
                        ps.setInt( 1, empCar.getCodEmpleado() );
                        ps.setInt( 2, empCar.getCodCargoSucursal());
                        ps.setInt( 3, empCar.getCodCargoSucPlanilla());
                        ps.setDate( 4, (Date) empCar.getFechaInicio());
                        ps.setInt( 5, empCar.getAudUsuario());
                        ps.setString(6, acc);
                        ps.executeUpdate();
                    });


        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: EmpleadoCargoDao en registrarEmpleadoCargo, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }
}
