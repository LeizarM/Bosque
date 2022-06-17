package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ExperienciaLaboral;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class ExperienciaLaboralDao implements IExperienciaLaboral {

    /**
     * El Datasoruce
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento que devolvera la experiencia laboral por empleado
     * @param codEmpleado
     * @return
     */
    public List<ExperienciaLaboral> obtenerExperienciaLaboral( int codEmpleado ) {

        List<ExperienciaLaboral> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_ExperienciaLaboral @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "L" },
                    new int[] {Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{
                        ExperienciaLaboral temp = new ExperienciaLaboral();
                        temp.setCodExperienciaLaboral(rs.getInt(1));
                        temp.setCodEmpleado(rs.getInt(2));
                        temp.setNombreEmpresa(rs.getString(3));
                        temp.setCargo(rs.getString(4));
                        temp.setDescripcion(rs.getString(5));
                        temp.setFechaInicio(rs.getDate(6));
                        temp.setFechaFin(rs.getDate(7));
                        temp.setNroReferencia(rs.getString(8));
                        temp.setAudUsuario(rs.getInt(9));
                        return temp;
                    }

            );
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: ExperienciaLaboralDao en obtenerExperienciaLaboral, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Procedimiento para registrar la experiencia laboral por empleado
     * @param exlab
     * @param acc
     * @return
     */
    public boolean registrarExpLaboral( ExperienciaLaboral exlab, String acc ) {
        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_ExperienciaLaboral @codExperienciaLaboral=?,@codEmpleado=? ,@nombreEmpresa=? ,@cargo=? ,@descripcion=? ,@fechaInicio=? ,@fechaFin=? ,@nroReferencia=? ,@audUsuarioI=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, exlab.getCodExperienciaLaboral() );
                        ps.setInt(2, exlab.getCodEmpleado() );
                        ps.setString(3, exlab.getNombreEmpresa() );
                        ps.setString(4, exlab.getCargo());
                        ps.setString(5, exlab.getDescripcion());
                        ps.setDate( 6, (Date) exlab.getFechaInicio());
                        ps.setDate( 7, (Date) exlab.getFechaFin());
                        ps.setString( 8, exlab.getNroReferencia() );
                        ps.setInt(9, exlab.getAudUsuario());
                        ps.setString(10, acc);
                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: ExperienciaLaboralDao en registrarExpLaboral, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp != 0;
    }
}
