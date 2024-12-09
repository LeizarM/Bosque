package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TipoSolicitud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class TipoSolicitudDao implements ITipoSolicitud{


    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Obtiene todos los tipos de solicitud registrados en la base de datos.
     *
     * @return
     */
    @Override
    public List<TipoSolicitud> obtenerTipoSolicitudes() {

        List<TipoSolicitud> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_tpre_TipoSolicitud  @ACCION = ?",
                    new Object[]{ "L" },
                    new int[]{ Types.VARCHAR },
                    (rs, rowCount) ->{

                        TipoSolicitud temp = new TipoSolicitud();

                        temp.setIdES( rs.getInt(1)  );
                        temp.setDescripcion( rs.getString(2) );
                        temp.setEstado(rs.getInt(3) );
                        temp.setAudUsuario( rs.getInt(4) );


                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: TipoSolicitudDao en obtenerTipoSolicitudes, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;



    }
}
