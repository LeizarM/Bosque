package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.FacturaTigo;
import bo.bosque.com.impexpap.model.SociosTigo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SociosTigoDao implements  ISociosTigo {
    /**
     * El DataSource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;
    /**
     * Procedimiento para obtener los numeros asociados a TIGO por empleado agrupados
     * @param
     * @return
     */
    public List<SociosTigo> obtenerSociosTIGO() {
        List<SociosTigo> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tTigoSocios @ACCION=?",
                    new Object[] {  "A" },
                    new int[] {Types.VARCHAR },
                    (rs, rowCount)->{

                        SociosTigo temp = new SociosTigo();

                        temp.setCodCuenta(rs.getInt(1));
                        temp.setNombreCompleto(rs.getString(2));
                        temp.setCodEmpleado(rs.getInt(3));
                        temp.setTelefono(rs.getInt(4));
                        temp.setDescripcion(rs.getString(5));
                        //temp.setAudUsuario(rs.getInt(6));
                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: SociosTigoDao en obtenerSociosTIGO, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
    /**
     * Procedimiento para insertar los socios de TIGO
     * @param st, acc
     * @return
     */
    public boolean registrarSociosTigo(SociosTigo st, String acc) {
        int resp;
        try {
            resp = this.jdbcTemplate.update(
                    "EXEC p_abm_tTigoSocios @codCuenta=?, @codEmpleado = ?, @telefono = ?,@nombreCompleto = ?, @descripcion=?, @audUsuarioI = ?, @audFechaI = ?, @ACCION = ?",
                    ps -> {
                        ps.setInt(1, st.getCodCuenta());
                        ps.setInt(1, st.getCodCuenta());
                        if (st.getCodEmpleado() != null) {
                            ps.setInt(2, st.getCodEmpleado());
                        } else {
                            ps.setNull(2, java.sql.Types.INTEGER); // <-- Esto evita el NullPointerException
                        }
                        ps.setInt(3,st.getTelefono());
                        ps.setString(4, st.getNombreCompleto());
                        ps.setString(5, st.getDescripcion());
                        ps.setInt(6, st.getAudUsuario());
                        ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                        ps.setString(8, acc);
                    }
            );
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: SociosTigoDao en registrarSociosTigo, DataAccessException -> " + e.getMessage() +
                    ", SQL Code -> " + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp != 0;
    }

    /**
     * OBTENER LISTA COMPLETA DE GRUPOS (SOCIOS) TIGO + EMPLEADOS ORIGINALES
     * @return
     */
    public List<SociosTigo> obtenerListaGruposTigo(String periodoCobrado) {
        List<SociosTigo> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tTigoSocios @periodoCobrado=?, @ACCION=?",
                    new Object[] {periodoCobrado,  "B" },
                    new int[] {Types.VARCHAR,Types.VARCHAR },
                    (rs, rowCount)->{

                        SociosTigo temp = new SociosTigo();

                        temp.setCodCuenta(rs.getInt(1));
                        temp.setNombreCompleto(rs.getString(2));
                        temp.setCodEmpleado(rs.getInt(3));
                        temp.setTelefono(rs.getInt(4));
                        temp.setDescripcion(rs.getString(5));
                        //temp.setAudUsuario(rs.getInt(6));
                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: SociosTigoDao en obtenerListaGruposTigo, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
    /**
     * Procedimiento para obtener los numeros SIN ASIGNAR DE LA FACTURA TIGO
     * @param
     * @return
     */
    public List<SociosTigo> obtenerNumerosSinAsignar(String periodoCobrado) {
        List<SociosTigo> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tTigoSocios @periodoCobrado=?, @ACCION=?",
                    new Object[] { periodoCobrado, "C" },
                    new int[] {Types.VARCHAR,Types.VARCHAR },
                    (rs, rowCount)->{

                        SociosTigo temp = new SociosTigo();

                        temp.setTelefono(rs.getInt(1));
                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: SociosTigoDao en obtenerNumerosSinAsignar, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
