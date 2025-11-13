package bo.bosque.com.impexpap.dao;
import bo.bosque.com.impexpap.model.FacturaTigo;
import bo.bosque.com.impexpap.model.SociosTigo;
import bo.bosque.com.impexpap.model.TigoEjecutado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TigoEjecutadoDao implements  ITigoEjecutado{
    /**
     * DATASOURCE
     */
    @Autowired
    JdbcTemplate jdbcTemplate;
    /**
     * procedimiento para obtener la factura ejecutada TIGO
     */
    public List<TigoEjecutado> obtenerTotalCobradoXCuenta(String periodoCobrado) {
        List<TigoEjecutado> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tTigo_ejecutado @periodoCobrado=?, @ACCION=?",
                    new Object[] {periodoCobrado,  "A" },
                    new int[] {Types.VARCHAR,Types.VARCHAR },
                    (rs, rowCount)->{

                        TigoEjecutado temp = new TigoEjecutado();
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.setNombreCompleto(rs.getString(2));
                        temp.setCiNumero(rs.getString(3));
                        temp.setPeriodoCobrado(rs.getString(4));
                        temp.setTotalCobradoXCuenta(rs.getFloat(5));
                        temp.setMontoCubiertoXEmpresa(rs.getFloat(6));
                        temp.setMontoEmpleado(rs.getFloat(7));
                        //temp.setAudUsuario(rs.getInt(7));
                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: TigoEjecutadoDao en obtenerTigoEjecutadoTotalXCuenta, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
    /**
     * Procedimiento para insertar los socios de TIGO
     * @param acc
     * @return
     */
    public boolean generarAnticiposTigo(String periodoCobrado) {
        int resultado = 0;

        try {
            resultado = this.jdbcTemplate.update(
                    "EXEC p_abm_tTigo_ejecutado @periodoCobrado=?, @ACCION=?",
                    new Object[] { periodoCobrado, "B" },
                    new int[] { Types.VARCHAR, Types.VARCHAR }
            );
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: TigoEjecutadoDao en generarAnticiposTigo, DataAccessException->"
                    + e.getMessage() + ", SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            resultado = 0;
            this.jdbcTemplate = null;
        }

        return resultado != 0;
    }

    /**
     * procedimiento para obtener la factura ejecutada TIGO
     */
    public List<TigoEjecutado> obtenerResumenCuentas(String periodoCobrado) {
        List<TigoEjecutado> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tTigo_ejecutado @periodoCobrado=?, @ACCION=?",
                    new Object[] {periodoCobrado,  "B" },
                    new int[] {Types.VARCHAR,Types.VARCHAR },
                    (rs, rowCount)->{

                        TigoEjecutado temp = new TigoEjecutado();
                        //temp.setCodEmpleado(rs.getInt(1));
                        temp.setNombreCompleto(rs.getString(1));
                        //temp.setCiNumero(rs.getString(3));
                        //temp.setPeriodoCobrado(rs.getString(4));
                        temp.setTotalCobradoXCuenta(rs.getFloat(2));
                        //temp.setMontoCubiertoXEmpresa(rs.getFloat(6));
                        //temp.setMontoEmpleado(rs.getFloat(7));
                        //temp.setAudUsuario(rs.getInt(7));
                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: TigoEjecutadoDao en obtenerTigoEjecutadoTotalXCuenta, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
    /**
     * procedimiento para obtener detalle x cuenta tigo
     */
    public List<TigoEjecutado> obtenerDetalleXCuentas(String periodoCobrado) {
        List<TigoEjecutado> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "EXEC p_list_tTigo_ejecutado @periodoCobrado = ?, @ACCION = ?",
                    new Object[] { periodoCobrado, "E" },
                    new int[] { Types.VARCHAR, Types.VARCHAR },
                    (rs, rowCount) -> {
                        TigoEjecutado temp = new TigoEjecutado();
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.setNombreCompleto(rs.getString(2));
                        temp.setDescripcion(rs.getString(3));
                        temp.setCiNumero(rs.getString(4));
                        temp.setCorporativo(rs.getString(5));
                        temp.setEmpresa(rs.getString(6));
                        temp.setPeriodoCobrado(rs.getString(7));
                        temp.setTotalCobradoXCuenta(rs.getFloat(8));
                        temp.setMontoCubiertoXEmpresa(rs.getFloat(9));
                        temp.setMontoEmpleado(rs.getFloat(10));
                        return temp;
                    }
            );
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: TigoEjecutadoDao en obtenerDetalleXCuentas, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return lstTemp;
    }
    /**
     * Procedimiento para insertar los socios de TIGO
     * @param acc
     * @return
     */
    public boolean insertarTigoEjecutado(String periodoCobrado) {
        int resultado = 0;

        try {
            resultado = this.jdbcTemplate.update(
                    "EXEC p_abm_tTigo_ejecutado @periodoCobrado=?, @ACCION=?",
                    new Object[] { periodoCobrado, "G" },
                    new int[] { Types.VARCHAR, Types.VARCHAR }
            );
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: TigoEjecutadoDao en generarAnticiposTigo, DataAccessException->"
                    + e.getMessage() + ", SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            resultado = 0;
            this.jdbcTemplate = null;
        }

        return resultado != 0;
    }
    /**
     * TTIGOEJECUTADO
     */
    public boolean registrarTigoEjecutado(TigoEjecutado te, String acc) {
        int resp;
        try {
            resp = this.jdbcTemplate.update(
                    "EXEC p_list_tTigo_ejecutado @codEmpleado = ?, @nombreCompleto = ?, @descripcion = ?, @ciNumero = ?, @corporativo = ?, @empresa = ?,@periodoCobrado=?,@estado=?, @totalCobradoXCuenta = ?,@montoCubiertoXEmpresa= ?,@montoEmpleado=?, @audUsuarioI = ?, @ACCION = ?",
                    ps -> {
                        ps.setInt(1, te.getCodEmpleado() != null ? te.getCodEmpleado() : 0);
                        ps.setString(2, te.getNombreCompleto());
                        ps.setString(3, te.getDescripcion());
                        ps.setString(4, te.getCiNumero());
                        ps.setString(5, te.getCorporativo());
                        ps.setString(6, te.getEmpresa());
                        ps.setString(7, te.getPeriodoCobrado());
                        ps.setString(8,te.getEstado());
                        ps.setFloat(9,te.getTotalCobradoXCuenta());
                        ps.setFloat(10,te.getMontoCubiertoXEmpresa());
                        ps.setFloat(11,te.getMontoEmpleado());
                        ps.setInt(12, te.getAudUsuario());
                        //ps.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
                        ps.setString(13, acc);
                    }
            );
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: FacturaDao en registrarFacturaTigo, DataAccessException -> " + e.getMessage() +
                    ", SQL Code -> " + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }catch (DataAccessException e) { // <-- NUEVO CATCH
            // Intercepta cualquier excepción de acceso a datos
            Throwable rootCause = e.getRootCause();
            if (rootCause instanceof java.sql.SQLException) {
                java.sql.SQLException sqlEx = (java.sql.SQLException) rootCause;
                // El código 50000 es el que SQL Server usa para RAISERROR (severidad 16)
                if (sqlEx.getErrorCode() == 50000) {
                    // LANZAMOS EL MENSAJE CLARO COMO UNA EXCEPCIÓN DE EJECUCIÓN
                    throw new RuntimeException(sqlEx.getMessage());
                }
            }
            // Si no es el error 50000, relanzamos la excepción original
            throw e;
        }
        return resp != 0;
    }
    /**
     * procedimiento para obtener la factura ejecutada TIGO
     */
    public List<TigoEjecutado> obtenerTigoEjecutado(String empresa,String periodoCobrado) {
        List<TigoEjecutado> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tTigo_ejecutado @empresa=?, @periodoCobrado=?, @ACCION=?",
                    new Object[] {empresa,periodoCobrado,  "K" },
                    new int[] {Types.VARCHAR,Types.VARCHAR,Types.VARCHAR },
                    (rs, rowCount)->{

                        TigoEjecutado temp = new TigoEjecutado();
                        //temp.setCodEmpleado(rs.getInt(1));
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.setCodEmpleadoPadre(rs.getObject(2) != null ? rs.getInt(2) : null);
                        temp.setCorporativo(rs.getString(3));
                        temp.setNombreCompleto(rs.getString(4));
                        temp.setCiNumero(rs.getString(5));
                        temp.setDescripcion(rs.getString(6));
                        temp.setPeriodoCobrado(rs.getString(7));
                        temp.setEmpresa(rs.getString(8));
                        temp.setEstado(rs.getString(9));
                        temp.setTotalCobradoXCuenta(rs.getFloat(10));
                        temp.setMontoCubiertoXEmpresa(rs.getFloat(11));
                        temp.setMontoEmpleado(rs.getFloat(12));
                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: TigoEjecutadoDao en obtenerTigoEjecutado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
    /**
     * procedimiento para obtener la factura ejecutada TIGO
     */
    public List<TigoEjecutado> obtenerArbolDetallado(String empresa, String periodoCobrado) {
        List<TigoEjecutado> lstTemp;
        try {
            lstTemp = this.jdbcTemplate.query(
                    "EXEC p_list_tTigo_ejecutado @empresa=?, @periodoCobrado=?, @ACCION=?",
                    new Object[] { empresa, periodoCobrado, "N" },
                    new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR },
                    (rs, rowNum) -> {
                        TigoEjecutado temp = new TigoEjecutado();
                        //temp.setFila(rs.getInt(1));
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.setCodEmpleadoPadre(rs.getObject(2) != null ? rs.getInt(2) : null);
                        temp.setNombreCompleto(rs.getString(3));
                        temp.setDescripcion(rs.getString(4));
                        temp.setCiNumero(rs.getString(5));
                        temp.setCorporativo(rs.getString(6));
                        temp.setEmpresa(rs.getString(7));
                        temp.setPeriodoCobrado(rs.getString(8));
                        temp.setTotalCobradoXCuenta(rs.getFloat(9));
                        temp.setMontoCubiertoXEmpresa(rs.getFloat(10));
                        temp.setMontoEmpleado(rs.getFloat(11) );
                        return temp;
                    }
            );
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: TigoEjecutadoDao en obtenerArbolDetallado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

}
