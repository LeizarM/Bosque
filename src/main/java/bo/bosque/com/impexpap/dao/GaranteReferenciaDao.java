package bo.bosque.com.impexpap.dao;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
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
     * Metodo para listar los garantes o referencias de un empleado
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


}
