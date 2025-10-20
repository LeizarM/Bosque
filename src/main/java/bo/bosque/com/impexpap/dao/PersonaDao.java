package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Persona;
import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;


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

                        temp.getPais().setCodPais(rs.getInt(19));
                        temp.getPais().setPais(rs.getString(20));
                        temp.setLat(rs.getFloat(21));
                        temp.setLng(rs.getFloat(22));
                        temp.setAudUsuarioI(rs.getInt(23));
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

    public Integer registrarPersona(Persona per, String acc) {
        System.out.println(per.toString());
        Integer retorna = null;
        //se usa callablestatement
        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             CallableStatement cs = conn.prepareCall( "execute p_abm_persona @codPersona=?, @codZona=?, @nombres=?, @apPaterno=?, @apMaterno=?, @ciExpedido=?, @ciFechaVencimiento=?, @ciNumero=?, @direccion=?, @estadoCivil=?, @fechaNacimiento=?, @lugarNacimiento=?, @nacionalidad=?, @sexo=?, @lat=?, @lng=?,  @audUsuarioI=?,@RETORNA=?, @ACCION=?"
                     )) {
            cs.setInt(1, per.getCodPersona());
            cs.setInt(2, per.getCodZona());
            cs.setString(3, per.getNombres());
            cs.setString(4, per.getApPaterno());
            cs.setString(5, per.getApMaterno());
            cs.setString(6, per.getCiExpedido());
            cs.setDate(7, new java.sql.Date(per.getCiFechaVencimiento().getTime()));
            cs.setString(8, per.getCiNumero());
            cs.setString(9, per.getDireccion());
            cs.setString(10, per.getEstadoCivil());
            cs.setDate(11, new java.sql.Date(per.getFechaNacimiento().getTime()));
            cs.setString(12, per.getLugarNacimiento());
            cs.setInt(13, per.getNacionalidad());
            cs.setString(14, per.getSexo());
            cs.setFloat(15, per.getLat());
            cs.setFloat(16, per.getLng());
            cs.setInt(17, per.getAudUsuarioI());
            cs.registerOutParameter(18, Types.INTEGER);
            cs.setString(19, acc);

            cs.execute();

            retorna = cs.getInt(18);

        } catch (SQLException e) {
            System.out.println("Error SQL: " + e.getMessage());
        }
        return retorna;
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

    /**
     * Obtendra una lista de tipos de sexo
     * @return
     */
    public List<Tipos> lstSexo() {
        return new Tipos().lstSexo();
    }
    /**
     * Obtendra una lista de tipos de sexo
     * @return
     */
    public List<Tipos> lstEstadoCivil() {
        return new Tipos().lstEstadoCivil();
    }
    /**
     * Obtendra una lista de tipos de ciExp
     * @return
     */
    public List<Tipos> lstCiExp() {
        return new Tipos().lstCiExp();
    }
    /**
     * Obtendra una lista de tipos de FORMACION
     * @return
     */
    public List<Tipos> lstTipoFormacion() {
        return new Tipos().lstTipoFormacion();
    }

    public List<Tipos>lstTipoDuracionFormacion(){
        return new Tipos().lstTipoDuracionFormacion();
    }

    /**
     * Obtendra una lista de personas
     * @return
     */
    public List<Persona>obtenerListaPersonas(){
        List<Persona>lstTemp;
        try{
            lstTemp = this.jdbcTemplate.query(" execute p_list_Persona @ACCION=?",
                    new Object[]{"C"},
                    new int[]{Types.VARCHAR},
                    (rs, rowNum)->{
                        Persona temp= new Persona();
                        temp.setCodPersona(rs.getInt(1));
                        temp.setDatoPersona(rs.getString(2));
                         //temp.setNombres(rs.getString(2));
                        //temp.setApPaterno(rs.getString(3));
                        //temp.setApMaterno(rs.getString(4));
                        return temp;
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en obtenerListaPersonas,DataAccessException->"+e.getMessage()+".SQL code->"+((SQLException)e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }



}
