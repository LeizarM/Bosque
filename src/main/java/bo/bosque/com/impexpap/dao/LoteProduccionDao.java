package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.LoteProduccion;
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
                                                ", @cantHojasSalida=? , @mermaTotal=? , @diferenciaProduccion=?, @diferenciaProdResma = ?, @cantEstimadaResma = ?, @pesoBalanzaTotal = ? , @estado = ? ,@obs=?, @numCorte=?, @anioCorte=?, @docNumOrdFab=?, @codEmpresa=? , @audUsuario=? ,  @ACCION=?",
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
                        ps.setInt(25, loProd.getDocNumOrdFab());
                        ps.setInt(26, loProd.getCodEmpresa());
                        ps.setInt(27, loProd.getAudUsuario());
                        ps.setString(28, acc);

                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: LoteProduccionDao en registrarLoteProduccion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
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
        }
        return lstTemp;

    }

    @Override
    public List<LoteProduccion> obtenerDocNumXEmpresa( int codEmpresa ) {

        List<LoteProduccion> lstTemp = new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tprod_loteProduccion @codEmpresa=?, @ACCION=?",
                    new Object[] { codEmpresa, "H" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount)->{

                        LoteProduccion temp = new LoteProduccion();

                        temp.setDocNumOrdFab(rs.getInt(1));
                        temp.setCodArtEntrada(rs.getString(2));
                        temp.setCodArtSalida(rs.getString(3));
                        temp.setCodEmpresa(rs.getInt(4));
                        temp.setDb(rs.getString(5));

                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: LoteProduccionDao en obtenerDocNumXEmpresa, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
        }
        return lstTemp;



    }

    /**
     * Para obtener los lotes de produccion de un rango de fechas.
     * Con ambas fechas en null el SP devuelve los ultimos 125.
     *
     * @param fechaIni
     * @param fechaFin
     * @return
     */
    @Override
    public List<LoteProduccion> obtenerLotesProduccion( java.util.Date fechaIni, java.util.Date fechaFin ) {

        List<LoteProduccion> lstTemp = new ArrayList<>();

        Utiles utiles = new Utiles();
        Date desde = utiles.fechaJ_a_Sql( fechaIni );
        Date hasta = utiles.fechaJ_a_Sql( fechaFin );

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tprod_loteProduccion @fechaIni=?, @fechaFin=?, @ACCION=?",
                    new Object[] { desde, hasta, "C" },
                    new int[] { Types.DATE, Types.DATE, Types.VARCHAR },
                    (rs, rowCount)->{

                        LoteProduccion temp = new LoteProduccion();

                        temp.setIdLp(rs.getInt(1));
                        temp.setIdMa(rs.getInt(2));
                        temp.setNumLote(rs.getInt(3));
                        temp.setAnio(rs.getInt(4));
                        temp.setFecha(rs.getDate(5));
                        temp.setHraInicioCorte(rs.getString(6));
                        temp.setHraInicio(rs.getString(7));
                        temp.setHraFin(rs.getString(8));
                        temp.setCantBobinasIngresoTotal(rs.getInt(9));
                        temp.setPesoKilosTotalIngreso(rs.getFloat(10));
                        temp.setPesoTotalSalida(rs.getFloat(11));
                        temp.setPesoPaletaSalida(rs.getFloat(12));
                        temp.setPesoMaterialSalida(rs.getFloat(13));
                        temp.setCantResmaSalida(rs.getInt(14));
                        temp.setCantHojasSalida(rs.getFloat(15));
                        temp.setMermaTotal(rs.getFloat(16));
                        temp.setDiferenciaProduccion(rs.getFloat(17));
                        temp.setDiferenciaProdResma(rs.getFloat(18));
                        temp.setCantEstimadaResma(rs.getFloat(19));
                        temp.setPesoBalanzaTotal(rs.getFloat(20));
                        temp.setEstado(rs.getInt(21));
                        temp.setObs(rs.getString(22));
                        temp.setNumCorte(rs.getInt(23));
                        temp.setAnioCorte(rs.getInt(24));
                        temp.setDocNumOrdFab(rs.getInt(25));
                        temp.setCodEmpresa(rs.getInt(26));
                        temp.setAudUsuario(rs.getInt(27));

                        return temp;

                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: LoteProduccionDao en obtenerLotesProduccion, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
        }
        return lstTemp;
    }
}
