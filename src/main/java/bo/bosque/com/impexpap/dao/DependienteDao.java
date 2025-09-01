package bo.bosque.com.impexpap.dao;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import bo.bosque.com.impexpap.utils.Tipos;
import org.exolab.castor.mapping.xml.Sql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import bo.bosque.com.impexpap.model.Dependiente;
import org.springframework.stereotype.Repository;


@Repository
public class DependienteDao implements IDependiente {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Obtendra la lista de dependientes de un empleado
     * @param codEmpleado
     * @return
     */
    @Override
    public List<Dependiente> obtenerDependientes( int codEmpleado ) {
        List<Dependiente> lstTemp = new ArrayList<>();
        try {
           // System.out.println("Valor de codEmpleado: " + codEmpleado);
            lstTemp = this.jdbcTemplate.query("execute p_list_Dependiente @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "A" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)-> {
                        Dependiente dep = new Dependiente();
                        dep.setCodDependiente(rs.getInt(1));
                        dep.setCodPersona(rs.getInt(2));
                       dep.setCodEmpleado(rs.getInt(3));
                        dep.setEsActivo(rs.getString(4));
                        dep.setNombreCompleto(rs.getString(5));
                        dep.setParentesco(rs.getString(6));
                        dep.setEdad(rs.getInt(7));
                        return dep;
                    });
        }catch (BadSqlGrammarException e) {
            System.out.println("Error: DependienteDao en obtenerDependientes, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Para el abm de dependientes
     * @param dep
     * @param acc
     * @return true si lo hizo correctamente
     */
    public boolean registrarDependiente( Dependiente dep, String acc ) {
        System.out.println(dep.toString());
        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_Dependiente @codDependiente=?, @codPersona=?, @codEmpleado=?, @parentesco=?, @esActivo=?, @audUsuarioI=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, dep.getCodDependiente() );
                        ps.setInt(2, dep.getCodPersona() );
                        ps.setInt (3, dep.getCodEmpleado() );
                        ps.setString(4, dep.getParentesco() );
                        ps.setString(5, dep.getEsActivo() );
                        ps.setInt(6, dep.getAudUsuario() );
                        ps.setString(7, acc);
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: DependienteDao en registrarDependiente, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp!=0;
    }



    /**
     * Procedimiento para mostrar si los dependientes son empleados
     * @return
     */

    public List<Dependiente>obtenerDepEmp(int codEmpleado){
    List<Dependiente>lstTemp= new ArrayList<>();
    try{
        lstTemp = this.jdbcTemplate.query("execute p_list_Empleado @codEmpleado=?, @ACCION=?",
                new Object[] { codEmpleado, "Q" },
                new int[] { Types.INTEGER, Types.VARCHAR },
                (rs, rowCount)-> {
                    Dependiente dep= new Dependiente();
                    dep.setCodDependiente(rs.getInt("codDependiente"));
                    dep.setCodPersona(rs.getInt("codPersona"));
                    dep.setCodEmpleado(rs.getInt("codEmpleado"));
                    dep.setAudUsuario(rs.getInt("audUsuarioI"));
                    dep.setNombreCompleto(rs.getString("NombreCompleto"));
                    return dep;
                });

        }
    catch (BadSqlGrammarException e){
        System.out.println("Error: DependienteDao en obtenerDepEmp, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
        lstTemp = new ArrayList<>();
        this.jdbcTemplate = null;
    }
        return lstTemp;

    }
    /**
     * Obtendra una lista de tipos de parentesco
     * @return
     */
    public List<Tipos> lstTipoDependiente() {
        return new Tipos().lstTipoDependiente();
    }
    /**
     * Obtendra una lista de tipos de activo
     * @return
     */
    public List<Tipos> lstTipoActivo() {
        return new Tipos().lstTipoActivo();
    }

}


