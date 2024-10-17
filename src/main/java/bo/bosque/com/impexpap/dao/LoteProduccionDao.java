package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.LoteProduccion;
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
public class LoteProduccionDao implements ILoteProduccion {


    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Para registrar el lote de produccion
     *
     * @param loProd
     * @param acc
     * @return
     */
    public boolean registrarLoteProduccion( LoteProduccion loProd, String acc ) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_tprod_loteProduccion @idLp=?, @idMa=? ,@numLote=?, @anio=?, @fecha=?, @hraInicioCorte=? , @hraInicio=? , @hraFin=? " +
                                                ", @cantBobinasIngresoTotal=?, @pesoKilosTotalIngreso=? , @pesoTotalSalida=? , @pesoPaletaSalida=? , @pesoMaterialSalida=? , @cantResmaSalida=?" +
                                                ", @cantHojasSalida=? , @mermaTotal=? , @diferenciaProduccion=?, @diferenciaProdResma = ?, @cantEstimadaResma = ?, @pesoBalanzaTotal = ? , @estado = ? ,@obs=?, @numCorte=?, @anioCorte=? , @audUsuario=? ,  @ACCION=?",
                    ps -> {

                        ps.setEscapeProcessing( true );
                        ps.setInt(1, loProd.getIdLp());
                        ps.setInt(2, loProd.getIdMa());
                        ps.setInt(3, loProd.getNumLote());
                        ps.setInt(4, loProd.getAnio());
                        ps.setDate(5, (Date) loProd.getFecha());
                        ps.setString(6, loProd.getHraInicioCorte());
                        ps.setString(7, loProd.getHraInicio());
                        ps.setString(8, loProd.getHraFin());
                        ps.setInt(9, loProd.getCantBobinasIngresoTotal());
                        ps.setFloat(10, loProd.getPesoKilosTotalIngreso());
                        ps.setFloat(11, loProd.getPesoTotalSalida());
                        ps.setFloat(12, loProd.getPesoPaletaSalida());
                        ps.setFloat(13, loProd.getPesoMaterialSalida());
                        ps.setInt(14, loProd.getCantResmaSalida());
                        ps.setFloat(15, loProd.getCantHojasSalida());
                        ps.setFloat(16, loProd.getMermaTotal());
                        ps.setFloat(17, loProd.getDiferenciaProduccion());
                        ps.setFloat(18, loProd.getDiferenciaProdResma());
                        ps.setFloat(19, loProd.getCantEstimadaResma());
                        ps.setFloat(20, loProd.getPesoBalanzaTotal());
                        ps.setInt(21, loProd.getEstado());
                        ps.setString(22, loProd.getObs());
                        ps.setInt(23, loProd.getNumCorte());
                        ps.setInt(24, loProd.getAnioCorte());
                        ps.setInt(25, loProd.getAudUsuario());
                        ps.setString(26, acc);

                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: LoteProduccionDao en registrarLoteProduccion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;


    }

    /**
     * Para obtener la loteProduccion ultimo
     * @return
     */
    public List<LoteProduccion> obtenerLotesProduccionNew(int idMa) {

        List<LoteProduccion> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tprod_loteProduccion @idMa=?,  @ACCION=?",
                    new Object[] {  idMa,  "A" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{

                        LoteProduccion temp = new LoteProduccion();

                        temp.setIdLp(rs.getInt(1));
                        temp.setNumLote(rs.getInt(2));
                        temp.setAnio(rs.getInt(3));
                        temp.setFecha(rs.getDate(4));
                        temp.setHraInicioCorte(rs.getString(5));
                        temp.setHraInicio(rs.getString(6));
                        temp.setHraFin(rs.getString(7));
                        temp.setCantBobinasIngresoTotal(rs.getInt(8));
                        temp.setPesoKilosTotalIngreso(rs.getFloat(9));
                        temp.setPesoTotalSalida(rs.getFloat(10));
                        temp.setPesoPaletaSalida(rs.getFloat(11));
                        temp.setPesoMaterialSalida(rs.getFloat(12));
                        temp.setCantResmaSalida(rs.getInt(13));
                        temp.setCantHojasSalida(rs.getFloat(14));
                        temp.setMermaTotal(rs.getFloat(15));
                        temp.setDiferenciaProduccion(rs.getFloat(16));
                        temp.setObs(rs.getString(17));

                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: LoteProduccionDao en obtenerLotesProduccionNew, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Para obtener los articulos
     * @return
     */
    public List<LoteProduccion> obtenerArticulos() {

        List<LoteProduccion> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tprod_loteProduccion @ACCION=?",
                    new Object[] { "B" },
                    new int[] { Types.VARCHAR },
                    (rs, rowCount)->{

                        LoteProduccion temp = new LoteProduccion();

                        temp.setCodArticulo(rs.getString(1));
                        temp.setDatoArt(rs.getString(2));
                        temp.setArticulo(rs.getString(3));
                        temp.setUtm(rs.getFloat(4));

                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: LoteProduccionDao en obtenerArticulos, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;

    }
}
