package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RegistroDanoBobina;
import bo.bosque.com.impexpap.model.RegistroResma;
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
public class RegistroDanoBobinaDao  implements IRegistroDanoBobina {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Registrar un registro de daño de bobina
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarRegistroDanoBobina( RegistroDanoBobina mb, String acc ) {

        int resp;

        try{
            resp =  this.jdbcTemplate.update("execute p_abm_tmme_RegistroDanoBobina @idReg=?, @fecha=?, @codEmpleado=? ,@totalKilosBobinas=?, @totalKilosDanados=?,@totalKilosDanadosReal=?, @totalUSD=? ,@obs=?, @docNum=?, @codEmpresa=?  ,@audUsuario=?, @ACCION=?",
                    ps ->{
                        ps.setEscapeProcessing(true);
                        ps.setQueryTimeout(180);
                        ps.setInt(1, mb.getIdReg() );
                        ps.setDate(2, (Date) mb.getFecha());
                        ps.setInt(3, mb.getCodEmpleado() );
                        ps.setFloat(4, mb.getTotalKilosBobinas());
                        ps.setFloat(5, mb.getTotalKilosDanados() );
                        ps.setFloat(6, mb.getTotalKilosDanadosReal() );
                        ps.setFloat(7, mb.getTotalUsd() );
                        ps.setString(8, mb.getObs() );
                        ps.setInt(9, mb.getDocNum() );
                        ps.setInt(10, mb.getCodEmpresa() );
                        ps.setInt(11, mb.getAudUsuario() );
                        ps.setString(12, acc);
                    });
        }catch( BadSqlGrammarException e){
            System.out.println("Error: RegistroDanoBobinaDao en registrarRegistroDanoBobina, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }

    /**
     * Listara el registro de daños de bobinas
     *
     * @return
     */
    @Override
    public List<RegistroDanoBobina> lstRegistroDanoBobina() {
        return null;
    }

    /**
     * Obtiene la lista de los registros de Bobina que están entrando por empresa
     *
     * @param codEmpresa
     * @return
     */
    @Override
    public List<RegistroDanoBobina> lstEntradaDeMercaderiasBob(int codEmpresa) {

        List<RegistroDanoBobina> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tmme_RegistroDanoBobina @codEmpresa=?, @ACCION=?",
                    new Object[]{ codEmpresa, "A" },
                    new int[]{ Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) ->{

                        RegistroDanoBobina temp = new RegistroDanoBobina();

                        temp.setDocNum(rs.getInt(1));

                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: RegistroDanoBobinaDao en lstEntradaDeMercaderiasBob, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Obtiene la lista de artculos por numero de documento y empresa
     *
     * @param codEmpresa
     * @param docNum
     * @return List
     */
    @Override
    public List<RegistroDanoBobina> lstArticuloXEntradaBob(int codEmpresa, int docNum) {

        List<RegistroDanoBobina> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tmme_RegistroDanoBobina @codEmpresa=?, @docNum=? ,@ACCION=?",
                    new Object[]{ codEmpresa, docNum ,"B" },
                    new int[]{ Types.INTEGER, Types.INTEGER ,Types.VARCHAR },
                    (rs, rowCount) ->{

                        RegistroDanoBobina temp = new RegistroDanoBobina();

                        temp.setCodArticulo(rs.getString(1));
                        temp.setDescripcion(rs.getString(2));
                        temp.setArticulo(rs.getString(3));
                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: RegistroDanoBobinaDao en lstArticuloXEntradaBob, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
}
