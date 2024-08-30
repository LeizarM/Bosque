package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Empresa;
import bo.bosque.com.impexpap.model.RegistroResma;
import bo.bosque.com.impexpap.model.RegistroResmaDetalle;
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
public class RegistroResmaDao implements IRegistroResma {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Registra un nuevo registro de resma
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarRegistroResma(RegistroResma mb, String acc) {

        int resp;

        try{
            resp =  this.jdbcTemplate.update("execute p_abm_tmme_RegistroResma @idMer=?, @fecha=?, @totalPeso=?, @totalUSD=?, @obs=?, @codEmpleado=?, @docNum=?, @codEmpresa=?  ,@audUsuario=?, @ACCION=?",
                    ps ->{
                        ps.setEscapeProcessing(true);
                        ps.setQueryTimeout(180);
                        ps.setInt(1, mb.getIdMer() );
                        ps.setDate(2, (Date) mb.getFecha());
                        ps.setFloat(3, mb.getTotalPeso() );
                        ps.setFloat(4, mb.getTotalUSD() );
                        ps.setString(5, mb.getObs() );
                        ps.setInt(6, mb.getCodEmpleado() );
                        ps.setInt(7, mb.getDocNum() );
                        ps.setInt(8, mb.getCodEmpresa() );
                        ps.setInt(9, mb.getAudUsuario() );
                        ps.setString(10, acc);
                    });
        }catch( BadSqlGrammarException e){
            System.out.println("Error: RegistroResmaDao en registrarRegistroResma, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }

    /**
     * Obtiene la lista de todos los registros de resma
     *
     * @return
     */
    @Override
    public List<RegistroResma> lstRegistroResma() {

        return null;
    }

    /**
     * Obtiene la lista de los registros de resma que están entrando por empresa
     *
     * @param codEmpresa
     * @return
     */
    @Override
    public List<RegistroResma> lstEntradaDeMercaderias( int codEmpresa ) {

        List<RegistroResma> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tmme_RegistroResma @codEmpresa=?, @ACCION=?",
                    new Object[]{ codEmpresa, "A" },
                    new int[]{ Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) ->{

                        RegistroResma temp = new RegistroResma();

                        temp.setDocNum(rs.getInt(1));

                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: RegistroResmaDao en lstEntradaDeMercaderias, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Obtiene la lista de los artículos que están en la entrada de mercaderias por empresa y número de documento
     * @param codEmpresa
     * @param docNum
     * @return
     */
    @Override
    public List<RegistroResma> lstArticuloXEntrada( int codEmpresa, int docNum ) {

        List<RegistroResma> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tmme_RegistroResma @codEmpresa=?, @docNum=? ,@ACCION=?",
                    new Object[]{ codEmpresa, docNum ,"B" },
                    new int[]{ Types.INTEGER, Types.INTEGER ,Types.VARCHAR },
                    (rs, rowCount) ->{

                        RegistroResma temp = new RegistroResma();

                        temp.setCodArticulo(rs.getString(1));
                        temp.setDescripcion(rs.getString(2));
                        temp.setArticulo(rs.getString(3));
                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: RegistroResmaDao en lstArticuloXEntrada, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

}
