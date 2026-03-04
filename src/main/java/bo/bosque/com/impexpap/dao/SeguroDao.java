package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.Persona;
import bo.bosque.com.impexpap.model.Seguro;
import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SeguroDao implements ISeguro{

    @Autowired
    JdbcTemplate jdbcTemplate;

    //OBTIENE UNA LISTA DE EMPLEADOS CON LA FECHA DE CUMPLEA;OS
    public List<Seguro> obtenerSeguros(){
        List<Seguro>lstTemp;
        try{
            lstTemp = this.jdbcTemplate.query(" execute p_list_seguro  @ACCION=?",
                    new Object[]{"L"},
                    new int[]{Types.VARCHAR},
                    (rs, rowNum)->{
                        Seguro temp= new Seguro();
                        temp.setCodSeguro(rs.getInt(1));
                        temp.setNombre(rs.getString(2));
                        temp.setNumero(rs.getString(3));
                        temp.setNombreCorto(rs.getString(4));
                        temp.setRegional(rs.getString(5));
                        temp.setTipo(rs.getString(6));
                        temp.setDescripcion(rs.getString(7));
                        temp.setAudUsuarioI(rs.getInt(8));
                        temp.setCodCiudad(rs.getInt(10));
                        return temp;
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: SeguroDAO en obtenerSeguros,DataAccessException->"+e.getMessage()+".SQL code->"+((SQLException)e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Registrara/editara/eliminara una aseguradora
     * @param seg
     * @param acc
     * @return
     */
    public boolean registroAseguradora (Seguro seg,String acc){
        int resp;
        try{
            resp=this.jdbcTemplate.update("execute p_abm_Seguro @codSeguro=?,@nombre=?,@nombreCorto=?,@numero=?,@regional=?,@tipo=?,@audUsuarioI=?,@codCiudad=?,@ACCION=?",
                    ps -> {
                        ps.setInt(1,seg.getCodSeguro());
                        ps.setString(2,seg.getNombre());
                        ps.setString(3,seg.getNombreCorto());
                        ps.setString(4,seg.getNumero());
                        ps.setString(5,seg.getRegional());
                        ps.setString(6,seg.getTipo());
                        ps.setInt(7,seg.getAudUsuarioI());
                        ps.setInt(8,seg.getCodCiudad());
                        ps.setString(9,acc);
                    });
        }catch(BadSqlGrammarException e){
            System.out.println("Error: SeguroDao en registroAseguradora, DataAccessException->"+e.getMessage()+ ",SQL Code->"+((SQLException) e.getCause()).getErrorCode());
            resp=0;
        }
        return resp !=0;
    }
    /**
     * Obtendra una lista de tipo de seguro
     * @return
     */
    public List<Tipos> listTipoSeguro() {
        return new Tipos().listTipoSeguro();
    }
}
