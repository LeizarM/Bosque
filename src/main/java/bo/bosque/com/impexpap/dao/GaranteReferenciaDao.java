package bo.bosque.com.impexpap.dao;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


import bo.bosque.com.impexpap.model.GaranteReferencia;

@Repository
public class GaranteReferenciaDao implements IGaranteReferencia {

    private JdbcTemplate jdbcTemplate;

    public GaranteReferenciaDao( JdbcTemplate jdbcTemplate ) {
        this.jdbcTemplate = jdbcTemplate;
    }


    /**
     * Metodo para listar los garantes o referencias de un Empleado
     * @param codEmpleado
     * @return
     */
    public List<GaranteReferencia> obtenerGaranteReferencia( int codEmpleado ) {
        List<GaranteReferencia> lstTemp = new ArrayList<>();
        try {
            lstTemp = this.jdbcTemplate.query("execute p_list_GaranteReferencia @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "R" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)-> {
                        GaranteReferencia garRef = new GaranteReferencia();

                        garRef.setCodGarante(rs.getInt(1));
                        garRef.setCodPersona(rs.getInt(2));
                        garRef.setNombreCompleto(rs.getString(3));
                        garRef.setDireccionDomicilio(rs.getString(4));
                        garRef.setDireccionTrabajo(rs.getString(5));
                        garRef.setEmpresaTrabajo(rs.getString(6));
                        garRef.setTipo(rs.getString(7));
                        garRef.setObservacion( rs.getString(8));
                        garRef.setEsEmpleado( rs.getString(9));
                        garRef.setTelefonos(rs.getString(10));

                        return garRef;

                    });
        }catch (BadSqlGrammarException e) {
            System.out.println("Error: GaranteReferenciaDao en obtenerGaranteReferencia, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return lstTemp;
    }

    /**
     * Para el abm de garante referencia
     *
     * @param garRef
     * @param acc
     * @return true si lo hizo correctamente
     */
    public boolean registrarGaranteReferencia(GaranteReferencia garRef, String acc) {
        int resp;
        System.out.println(garRef);
        System.out.println(acc);
        try{
            resp = this.jdbcTemplate.update("execute p_abm_GaranteReferencia @codGarante=?, @codPersona=?, @codEmpleado=?, @direccionTrabajo=? ,@empresaTrabajo=?, @tipo=?, @observacion=?, @audUsuarioI=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, garRef.getCodGarante() );
                        ps.setInt(2, garRef.getCodPersona() );
                        ps.setInt (3, garRef.getCodEmpleado() );
                        ps.setString(4, garRef.getDireccionTrabajo() );
                        ps.setString(5, garRef.getEmpresaTrabajo() );
                        ps.setString(6, garRef.getTipo() );
                        ps.setString(7, garRef.getObservacion() );
                        ps.setInt(8, garRef.getAudUsuario() );
                        ps.setString(9, acc);
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: GaranteReferenciaDao en registrarGaranteReferencia, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp!=0;
    }

    /**
     * Obtendra una lista de garante-referencia
     * @return
     */
    public List<Tipos> lstTipoGarRef() {
        return new Tipos().lstTipoGarRef();
    }
    /**
     * Obtendra una lista de garantes ref
     * @return
     */
    public List<GaranteReferencia>obtenerListaGarantes(){
        List<GaranteReferencia>lstTemp;
        try{
            lstTemp = this.jdbcTemplate.query(" execute p_list_GaranteReferencia @ACCION=?",
                    new Object[]{"L"},
                    new int[]{Types.VARCHAR},
                    (rs, rowNum)->{
                        GaranteReferencia temp= new GaranteReferencia();
                        temp.setCodGarante(rs.getInt(1));
                        temp.setCodPersona(rs.getInt(2));
                        temp.setCodEmpleado(rs.getInt(3));
                        temp.setNombreCompleto(rs.getString(4));
                        temp.setDireccionTrabajo(rs.getString(5));
                        temp.setEmpresaTrabajo(rs.getString(6));
                        temp.setTipo(rs.getString(7));
                        temp.setObservacion(rs.getString(8));
                        temp.setAudUsuario(rs.getInt(9));

                        return temp;
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en obtenerListaGarantes,DataAccessException->"+e.getMessage()+".SQL code->"+((SQLException)e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    public int contarGarantesPorPersona(int codPersona) {
        String sql = "SELECT COUNT(*) FROM trh_garanteReferencia WHERE codPersona = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{codPersona}, Integer.class);
    }

    public boolean existeGaranteTipo(int codPersona, int codEmpleado, String tipo) {
        String sql = "SELECT COUNT(*) FROM trh_garanteReferencia WHERE codPersona = ? AND codEmpleado = ? AND tipo = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{codPersona, codEmpleado, tipo}, Integer.class) > 0;
    }


}
