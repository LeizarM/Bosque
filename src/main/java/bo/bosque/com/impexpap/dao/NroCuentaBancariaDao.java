package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.NroCuentaBancaria;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class NroCuentaBancariaDao implements INroCuentaBancaria {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * PROCEDIMIENTO PARA OBTENER UNA LISTA DE LOS NUMEROS DE CUENTA BANCARIA
     * @param codEmpleado
     * @return
     */
    public List<NroCuentaBancaria> obtenerCuentasBanco (int codEmpleado){
        List<NroCuentaBancaria>lstTemp=new ArrayList<>();
        try{
            lstTemp =  this.jdbcTemplate.query("execute p_list_nroCuentaBancaria @codEmpleado=?,@ACCION=?",
                    new Object[]{codEmpleado,"L"},
                    new int [] {Types.INTEGER,Types.VARCHAR},
                    (rs, rowCount )->{
                        NroCuentaBancaria nroCuentaBancaria= new NroCuentaBancaria();
                        nroCuentaBancaria.setCodCuenta(rs.getInt(1));
                        nroCuentaBancaria.setCodEmpleado(rs.getInt(2));
                        nroCuentaBancaria.setCodBanco(rs.getInt(3));
                        nroCuentaBancaria.setNroCuentaBancaria(rs.getString(4));
                        nroCuentaBancaria.setEstado(rs.getInt(5));
                        nroCuentaBancaria.setAudUsuarioI(rs.getInt(6));
                        return nroCuentaBancaria;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: NroCuentaBancariaDao en  obtenerCuentasBanco, DataAccessException->"+e.getMessage()+",SQL Code"+((SQLException)e.getCause()).getErrorCode());
            lstTemp= new ArrayList<>();
            this.jdbcTemplate= null;
        }
        return lstTemp;
    }
    public boolean registrarCuentaBancaria (NroCuentaBancaria nroCuentaBancaria, String acc){
        int resp;
        try{
            resp= this.jdbcTemplate.update("execute p_abm_nroCuentaBancaria @codCuenta=?,@codEmpleado=?,@codBanco=?,@nroCuentaBancaria=?,@estado=?,@audUsuarioI=?,@ACCION=?",
                    ps -> {
                            ps.setInt(1,nroCuentaBancaria.getCodCuenta());
                            ps.setInt(2,nroCuentaBancaria.getCodEmpleado());
                            ps.setInt(3,nroCuentaBancaria.getCodBanco());
                            ps.setString(4,nroCuentaBancaria.getNroCuentaBancaria());
                            ps.setInt(5,nroCuentaBancaria.getEstado());
                            ps.setInt(6,nroCuentaBancaria.getAudUsuarioI());
                            ps.setString(7,acc);
                    });
        }catch(BadSqlGrammarException e){
            System.out.println("Error: EducacionDao  en registrarEducacion, DataAccessException->"+e.getMessage()+",SQL Code->"+((SQLException)e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp=0;
        }
        return resp !=0;
    }
}
