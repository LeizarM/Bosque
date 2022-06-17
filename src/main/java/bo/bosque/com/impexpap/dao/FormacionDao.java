package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Formacion;
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
public class FormacionDao implements  IFormacion {

    /**
     * El DataSource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento para obtener la formacion de un empleado
     * @param codEmpleado
     * @return
     */
    public List<Formacion> obtenerFormacion( int codEmpleado ) {
        List<Formacion> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_Formacion @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "L" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
            (rs, rowCount)->{

                Formacion temp = new Formacion();

                temp.setCodFormacion(rs.getInt(1));
                temp.setCodEmpleado(rs.getInt(2));
                temp.setDescripcion(rs.getString(3));
                temp.setDuracion(rs.getInt(4));
                temp.setTipoDuracion(rs.getString(5));
                temp.setTipoFormacion(rs.getString(6));
                temp.setFechaFormacion(rs.getDate(7));
                temp.setAudUsuario(rs.getInt(8));
                return temp;

            });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: FormacionDao en obtenerFormacion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Procedimiento para el abm de la formacion de un empleado
     * @param fr
     * @param acc
     * @return
     */
    public boolean registrarFormacion(Formacion fr, String acc) {
        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_Formacion @codFormacion  = ?,@codEmpleado = ?,@descripcion  = ?,@duracion = ?,@tipoDuracion= ?,@tipoFormacion = ?,@fechaFormacion= ? ,@audUsuarioI = ?, @ACCION = ?",
                    ps -> {
                        ps.setInt(1, fr.getCodFormacion() );
                        ps.setInt(2, fr.getCodEmpleado() );
                        ps.setString(3, fr.getDescripcion() );
                        ps.setInt(4, fr.getDuracion());
                        ps.setString(5, fr.getTipoDuracion());
                        ps.setString( 6, fr.getTipoFormacion() );
                        ps.setDate( 7, (Date) fr.getFechaFormacion());
                        ps.setInt(8, fr.getAudUsuario());
                        ps.setString(9, acc);
                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: FormacionDao en registrarFormacion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp != 0;
    }
}
