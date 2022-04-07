package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Ciudad;
import bo.bosque.com.impexpap.model.Pais;
import bo.bosque.com.impexpap.model.Persona;
import bo.bosque.com.impexpap.model.Zona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
                        temp.setCiNumero( rs.getInt(8) );
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
                        return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: PersonaDao en obtenerDatosPersonales, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            per = new Persona();
            this.jdbcTemplate = null;
        }

        return per;

    }
}
