package bo.bosque.com.impexpap.dao;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.Persona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;


@Repository
public class PersonaDao implements IPersona {

    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento para obtener los datos personales de un empleado
     * @param codPersona
     * @return
     */
    public Persona obtenerDatosPersonales(int codPersona) {
        Persona per;
        try {
            per =  this.jdbcTemplate.queryForObject("execute p_list_Persona @codPersona=?, @ACCION=?",
                    new Object[] { codPersona, "L" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Persona temp = new Persona();
                        temp.setCodPersona(rs.getInt(1));
                        temp.setCodZona(rs.getInt(2));
                        temp.setNombres(rs.getString(3));
                        temp.setApPaterno(rs.getString(4));
                        temp.setApMaterno(rs.getString(5));
                        temp.setCiExpedido(rs.getString(6));
                        temp.setCiFechaVencimiento(rs.getDate(7));
                        temp.setCiNumero( rs.getString(8) );
                        temp.setDireccion(rs.getString(9));
                        temp.setEstadoCivil(rs.getString(10));
                        temp.setFechaNacimiento(rs.getDate(11));
                        temp.setLugarNacimiento(rs.getString(12));
                        temp.setNacionalidad(rs.getInt(13));
                        temp.setSexo(rs.getString(14));
                        temp.getPais().setPais(rs.getString(15));
                        temp.getZona().setZona(rs.getString(16));
                        temp.getCiudad().setCodCiudad(rs.getInt(17));
                        temp.getCiudad().setCiudad(rs.getString(18));
                        temp.getCiudad().setCodPais(rs.getInt(19));
                        temp.getPais().setPais(rs.getString(20));
                        temp.setLat(rs.getFloat(21));
                        temp.setLng(rs.getFloat(22));
                        return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: PersonaDao en obtenerDatosPersonales, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            per = new Persona();
            this.jdbcTemplate = null;
        }

        return per;

    }

    /**
     * Procedimiento para el abm
     * @param per
     * @param acc
     */
    public boolean registrarPersona(Persona per, String acc) {

        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_persona @codPersona=?, @codZona=?, @nombres=?, @apPaterno=?, @apMaterno=?, @ciExpedido=?, @ciFechaVencimiento=?, @ciNumero=?, @direccion=?, @estadoCivil=?, @fechaNacimiento=?, @lugarNacimiento=?, @nacionalidad=?, @sexo=?, @lat=?, @lng=?,  @audUsuarioI=?, @ACCION=?",
                    ps -> {
                    ps.setInt(1, per.getCodPersona() );
                    ps.setInt(2, per.getCodZona() );
                    ps.setString(3, per.getNombres() );
                    ps.setString(4, per.getApPaterno() );
                    ps.setString(5, per.getApMaterno() );
                    ps.setString(6, per.getCiExpedido() );
                    ps.setDate(7, (Date) per.getCiFechaVencimiento());
                    ps.setString(8, per.getCiNumero() );
                    ps.setString(9,per.getDireccion() );
                    ps.setString(10, per.getEstadoCivil() );
                    ps.setDate(11, (Date) per.getFechaNacimiento());
                    ps.setString(12, per.getLugarNacimiento());
                    ps.setInt(13, per.getNacionalidad());
                    ps.setString(14, per.getSexo() );
                    ps.setFloat( 15, per.getLat() );
                    ps.setFloat( 16, per.getLng());
                    ps.setInt(17, per.getAudUsuarioI() );
                    ps.setString(18, acc);
                   });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: PersonaDao en obtenerDatosPersonales, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp!=0;
    }

    /**
     * Procedimiento para obtener el ultimo codigo de la persona insertad
     * @return
     */
    public int obtenerUltimoPersona() {
        Persona temp = new Persona();
        try {
            temp = this.jdbcTemplate.queryForObject("execute p_list_Persona @ACCION=?",
                    new Object[] { "B" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        Persona per = new Persona();
                        per.setCodPersona(rs.getInt(1));
                        return per;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: PersonaDao en obtenerUltimoPersona, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            temp = new Persona();
        }
        System.out.println("el codigo ultima persona es "+temp.getCodPersona()
        );
        return temp.getCodPersona();
    }
}
