package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.FacturaTigo;
import bo.bosque.com.impexpap.model.Formacion;
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
public class FacturaTigoDao implements IFacturaTigo {
    /**
     * El DataSource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;
    /**
     * Procedimiento para obtener el detalle deuda TIGO
     * @param codFactura
     * @return
     */
    public List<FacturaTigo> obtenerDetalleDeudaTigo() {
        List<FacturaTigo> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tTigoFactura @ACCION=?",
                    new Object[] {  "C" },
                    new int[] {Types.VARCHAR },
                    (rs, rowCount)->{

                        FacturaTigo temp = new FacturaTigo();
                        //temp.setNroCuenta(rs.getInt(1));
                        temp.setPeriodoCobrado(rs.getString(1));
                        temp.setEstado(rs.getString(2));
                        temp.setTotalCobradoXCuenta(rs.getFloat(3));
                        // temp.setAudUsuario(rs.getInt(4));
                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: FormacionDao en obtenerFormacion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
    /**
     * Procedimiento para insertar datos del detalle deuda TIGO
     * @param ft, acc
     * @return
     */
    public boolean registrarFacturaTigo(FacturaTigo ft, String acc) {
        int resp;
        try {
            resp = this.jdbcTemplate.update(
                    "EXEC p_abm_tTigoFactura @nroFactura = ?, @tipoServicio = ?, @nroContrato = ?, @nroCuenta = ?, @periodoCobrado = ?, @descripcionPlan = ?, @totalCobradoXcuenta = ?,@estado=?, @audUsuarioI = ?, @audFechaI = ?, @ACCION = ?",
                    ps -> {
                        ps.setString(1, ft.getNroFactura());
                        ps.setString(2, ft.getTipoServicio());
                        ps.setInt(3, ft.getNroContrato());
                        ps.setInt(4, ft.getNroCuenta());
                        ps.setString(5, ft.getPeriodoCobrado()); // <-- Aquí el cambio
                        ps.setString(6, ft.getDescripcionPlan());
                        ps.setFloat(7, ft.getTotalCobradoXCuenta());
                        ps.setString(8,ft.getEstado());
                        ps.setInt(9, ft.getAudUsuario());
                        ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
                        ps.setString(11, acc);
                    }
            );
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: FacturaDao en registrarFacturaTigo, DataAccessException -> " + e.getMessage() +
                    ", SQL Code -> " + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp != 0;
    }



}
