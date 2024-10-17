package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ProveedorExtSap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;


@Repository
public class ProveedorExtSapDao implements  IProveedorExtSap {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el Proveedor Externo Sap
     *
     * @param prodProveedorExtSap
     * @param acc
     * @return
     */
    @Override
    public boolean registrarProveedorExtSap(ProveedorExtSap prodProveedorExtSap, String acc) {

        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_proveedorExtSap @idProveedorSap=?, @codProvExtSap=?, @proveedorExtSap=?,@audUsuario=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, prodProveedorExtSap.getIdProveedorSap());
                        ps.setInt(2, prodProveedorExtSap.getCodProvExtSap());
                        ps.setString(3, prodProveedorExtSap.getProveedorExtSap());
                        ps.setInt(4, prodProveedorExtSap.getAudUsuario());
                        ps.setString(5, acc);
                    });
        }catch( BadSqlGrammarException e){
            System.out.println("Error: ProveedorExtSapDao en registrarProveedorExtSap, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp != 0;
    }
}
