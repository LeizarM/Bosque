package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.AfiliacionSeguro;
import bo.bosque.com.impexpap.model.Empleado;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Types;

@Repository
public class AfiliacionSeguroDao implements IAfiliacionSeguro {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * obtendra la afiliacion del seguro de un empleado
     */
    public AfiliacionSeguro obtenerAfiliacionSeguro (int codEmpleado){
        AfiliacionSeguro afSeg= new AfiliacionSeguro();
        try{
            afSeg= this.jdbcTemplate.queryForObject("execute p_list_afiliacion @codEmpleado=?,@ACCION=?",
                    new Object[]{codEmpleado,"B"},
                    new int []{Types.INTEGER,Types.VARCHAR},
                    (rs, rowNum) -> {
                        AfiliacionSeguro temp= new AfiliacionSeguro();
                        temp.setCodAfiliacion(rs.getInt(1));
                        temp.setCodEmpleado(rs.getInt(2));
                        temp.setCodPersona(rs.getInt(3));
                        temp.setCodSeguro(rs.getInt(4));
                        temp.setNombreCompleto(rs.getString(5));
                        temp.setFechaAfiliacion(rs.getDate(6));
                        temp.setFechaBaja(rs.getDate(7));
                        temp.setNroAfiliacion(rs.getString(8));
                        temp.getSeguro().setNombre(rs.getString(9));
                        temp.getSeguro().setRegional(rs.getString(10));
                        temp.setAudUsuarioI(rs.getInt(11));
                        return temp;
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en obtenerEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            afSeg = new AfiliacionSeguro();
            this.jdbcTemplate = null;
        }
        return afSeg;
    }
    /**
     * procedimiento para afiliar al seguro a un empleado
     */
    public boolean afiliarSeguroEmpleado(AfiliacionSeguro afSeg,String acc){
        int resp;
        try{
            resp=this.jdbcTemplate.update("execute p_abm_afiliacion @codAfiliacion=?,@codEmpleado=?,@codSeguro=?,@fechaAfiliacion=?,@fechaBaja=?,@nroAfiliacion=?,@audUsuarioI=?,@ACCION=?",
                    ps -> {
                        ps.setInt(1,afSeg.getCodAfiliacion());
                        ps.setInt(2,afSeg.getCodEmpleado());
                        ps.setInt(3,afSeg.getCodSeguro());
                        ps.setDate(4,afSeg.getFechaAfiliacion());
                        ps.setDate(5,afSeg.getFechaBaja());
                        ps.setString(6,afSeg.getNroAfiliacion());
                        ps.setInt(7,afSeg.getAudUsuarioI());
                        ps.setString(8,acc);
                    });
        }catch(BadSqlGrammarException e){
            System.out.println("Error: AfiliacionSeguroDAO en afiliarSeguroEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp !=0;
    }

}
