package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RelEmplEmpr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RelEmpEmprDAO implements IRelEmpEmpr {

    /**
     * El DataSource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Procedimiento para obtener el listado de relaciones laborales de un empleado
     * @param codEmpleado
     * @return
     */
    public List<RelEmplEmpr> obtenerRelacionesLaborales( int codEmpleado ) {

        List<RelEmplEmpr> lstTemp = new ArrayList<>();
        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_RelEmplEmpr @codEmpleado=?, @ACCION=?",
                    new Object[]{ codEmpleado, "A" },
                    new int[]{ Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) ->{
                        RelEmplEmpr temp = new RelEmplEmpr();

                        temp.setCodRelEmplEmpr(rs.getInt(1));
                        temp.setDatoFechasBeneficio(rs.getString(2));
                        return temp;
                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: RelEmpEmprDao en obtenerRelacionesLaborales, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return lstTemp;
    }

    /**
     * Procedimiento para el abm
     * @param empCar
     * @param acc
     * @return
     */
    public boolean registrarRelEmpEmpr(RelEmplEmpr empCar, String acc) {
        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_RelEmplEmpr @codRelEmplEmpr=?, @codEmpleado=?, @esActivo=?, @tipoRel=?, @nombreFileContrato=?, @fechaIni=?, @fechaFin=?, @motivoFin=? ,@audUsuarioI=?, @ACCION=?",
                    ps->{
                        ps.setInt(1, empCar.getCodRelEmplEmpr() );
                        ps.setInt(2, empCar.getCodEmpleado() );
                        ps.setInt(3, empCar.getEsActivo());
                        ps.setString ( 4, empCar.getTipoRel() );
                        ps.setString ( 5, empCar.getNombreFileContrato());
                        ps.setDate(6, (Date) empCar.getFechaIni());
                        ps.setDate(7, (Date) empCar.getFechaFin());
                        ps.setString(8, empCar.getMotivoFin());
                        ps.setInt(9, empCar.getAudUsuario());
                        ps.setString(10, acc);

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: RelEmpEmprDAO en registrarRelEmpEmpr, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }
}
