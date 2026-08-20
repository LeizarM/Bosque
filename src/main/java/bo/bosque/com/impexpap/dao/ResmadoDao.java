package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.Resmado;
import bo.bosque.com.impexpap.utils.Utiles;
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
public class ResmadoDao implements IResmado {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Para registrar el Resmado
     *
     * @param rmd
     * @param acc
     * @return true or false
     */
    @Override
    public boolean registrarResmado( Resmado rmd, String acc ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tprod_Resmado @idRes=?, @idGrupo=?, @codEmpleado=?, @fecha=?, @total=?, @hraInicio=?, @hraFin=?, @docNumOrdFab=?, @codEmpresa=? ,@audUsuario=? ,  @ACCION=?",
                    ps -> {

                        ps.setEscapeProcessing( true );
                        ps.setInt(1, rmd.getIdRes());
                        ps.setInt(2, rmd.getIdGrupo());
                        ps.setInt(3, rmd.getCodEmpleado());
                        ps.setDate(4, (Date) rmd.getFecha());
                        ps.setFloat(5, rmd.getTotal());
                        ps.setString(6, rmd.getHraInicio());
                        ps.setString(7, rmd.getHraFin());
                        ps.setInt(8, rmd.getDocNumOrdFab());
                        ps.setInt(9, rmd.getCodEmpresa());
                        ps.setInt(10, rmd.getAudUsuario());
                        ps.setString(11, acc);

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: ResmadoDao en registrarLoteProduccion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            resp = 0;
        }

        return resp!=0;


    }

    /**
     * Para obtener los resmados de un rango de fechas.
     * Con ambas fechas en null el SP devuelve los ultimos 125.
     *
     * @param fechaIni
     * @param fechaFin
     * @return
     */
    @Override
    public List<Resmado> obtenerResmados( java.util.Date fechaIni, java.util.Date fechaFin ) {

        List<Resmado> lstTemp = new ArrayList<>();

        Utiles utiles = new Utiles();
        Date desde = utiles.fechaJ_a_Sql( fechaIni );
        Date hasta = utiles.fechaJ_a_Sql( fechaFin );

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tprod_Resmado @fechaIni=?, @fechaFin=?, @ACCION=?",
                    new Object[] { desde, hasta, "B" },
                    new int[] { Types.DATE, Types.DATE, Types.VARCHAR },
                    (rs, rowCount)->{

                        Resmado temp = new Resmado();

                        temp.setIdRes(rs.getInt(1));
                        temp.setDescripcion(rs.getString(2));
                        temp.setNombreCompleto(rs.getString(3));
                        temp.setFecha(rs.getDate(4));
                        temp.setTotal(rs.getFloat(5));
                        temp.setHraInicio(rs.getString(6));
                        temp.setHraFin(rs.getString(7));
                        temp.setDocNumOrdFab(rs.getInt(8));
                        temp.setCodEmpresa(rs.getInt(9));
                        temp.setEmpresa(rs.getString(10));
                        temp.setIdGrupo(rs.getInt(11));

                        return temp;

                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: ResmadoDao en obtenerResmados, DataAccessException->" + e.getMessage());
            lstTemp = new ArrayList<>();
        }
        return lstTemp;
    }

    /**
     * Para actualizar solo la orden de fabricacion y la empresa de un resmado.
     * Usa la ACCION 'B' del SP, que toca esas dos columnas y nada mas.
     *
     * @param rmd
     * @return true or false
     */
    @Override
    public boolean actualizarOrdenFabricacion( Resmado rmd ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tprod_Resmado @idRes=?, @docNumOrdFab=?, @codEmpresa=?, @audUsuario=?, @ACCION=?",
                    ps -> {

                        ps.setEscapeProcessing( true );
                        ps.setInt(1, rmd.getIdRes());
                        ps.setInt(2, rmd.getDocNumOrdFab());
                        ps.setInt(3, rmd.getCodEmpresa());
                        ps.setInt(4, rmd.getAudUsuario());
                        ps.setString(5, "B");

                    });

        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: ResmadoDao en actualizarOrdenFabricacion, DataAccessException->" + e.getMessage());
            resp = 0;
        }

        return resp!=0;
    }
}
