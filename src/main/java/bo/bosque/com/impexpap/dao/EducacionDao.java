package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Educacion;
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
public class EducacionDao implements IEducacion  {
    /**
     * DATA SOURCE
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * PROCEDIMIENTO PARA REGISTRAR EDUCACION
     */
    public boolean registrarEducacion (Educacion educacion, String acc){
        int resp;
        try{
            resp= this.jdbcTemplate.update("execute p_abm_Educacion @codEducacion=?,@codEmpleado=?,@tipoEducacion=?,@descripcion=?,@fecha=?,@audUsuarioI=?,@ACCION=?",
                    ps -> {
                            ps.setInt(1,educacion.getCodEducacion());
                            ps.setInt(2,educacion.getCodEmpleado());
                            ps.setString(3,educacion.getTipoEducacion());
                            ps.setString(4,educacion.getDescripcion());
                            ps.setDate(5,(Date) educacion.getFecha());
                            ps.setInt(6,educacion.getAudUsuario());
                            ps.setString(7,acc);
                    });
        }catch(BadSqlGrammarException e){
            System.out.println("Error: EducacionDao  en registrarEducacion, DataAccessException->"+e.getMessage()+",SQL Code->"+((SQLException)e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp=0;
        }
        return resp !=0;
    }

    /**
     * OBTENDRA UNA LISTA DE EDUCACION
     * @param codEducacion
     * @return
     */
    public List<Educacion> obtenerEducacion(int codEducacion){
        List<Educacion>lstTemp=new ArrayList<>();
        try{
            lstTemp= this.jdbcTemplate.query("execute p_list_Educacion @codEmpleado=?,@ACCION=?",
                    new Object[]{codEducacion,"L"},
                    new int [] {Types.INTEGER,Types.VARCHAR},
                    (rs, rowCount)->{
                        Educacion educacion =  new Educacion();
                        educacion.setCodEducacion(rs.getInt(1));
                        educacion.setCodEmpleado(rs.getInt(2));
                        educacion.setTipoEducacion(rs.getString(3));
                        educacion.setDescripcion(rs.getString(4));
                        educacion.setFecha(rs.getDate(5));
                        educacion.setAudUsuario(rs.getInt(6));
                        return educacion;
                    });
        }catch(BadSqlGrammarException e){
            System.out.println("Error: EducacionDao eb obtenerEducacion, DataAccessException->"+e.getMessage()+",SQL Code->"+((SQLException)e.getCause()).getErrorCode());
            lstTemp= new ArrayList<>();
            this.jdbcTemplate= null;
        }
        return lstTemp;
    }
}
