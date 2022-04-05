package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Ciudad;
import bo.bosque.com.impexpap.model.Pais;
import bo.bosque.com.impexpap.model.Persona;
import bo.bosque.com.impexpap.model.Zona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.sql.Types;

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
                        temp.builder().codPersona(rs.getInt(1))
                                .codZona(rs.getInt(2))
                                .nombres(rs.getString(3))
                                .apPaterno(rs.getString(4))
                                .apMaterno(rs.getString(5))
                                .ciExpedido(rs.getString(6))
                                .ciFechaVencimiento(rs.getDate(7))
                                .ciNumero( rs.getInt(8) )
                                .direccion(rs.getString(9))
                                .estadoCivil(rs.getString(10))
                                .fechaNacimiento(rs.getDate(11))
                                .lugarNacimiento(rs.getString(12))
                                .nacionalidad(rs.getInt(13))
                                .sexo(rs.getString(14))
                                .pais( Pais.builder().pais(rs.getString(15)).build())
                                .zona(Zona.builder().zona(rs.getString(16)).build())
                                .ciudad(Ciudad.builder().codCiudad(rs.getInt(17)).ciudad(rs.getString(18)).codPais(rs.getInt(rs.getInt(19))).build())
                                .pais(Pais.builder().pais(rs.getString(20)).build()).build();
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
