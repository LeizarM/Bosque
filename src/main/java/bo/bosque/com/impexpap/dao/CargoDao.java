package bo.bosque.com.impexpap.dao;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import bo.bosque.com.impexpap.model.Cargo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CargoDao implements ICargo {

    /**
     * El Datasource
     */
    @Autowired
    JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento que obtendra los cargos por empresa
     * @param codEmpresa
     * @return
     */
    public List<Cargo> obtenerCargoXEmpresa( int codEmpresa ) {

        List<Cargo> lstTemp  = new ArrayList<>();

        try{

            lstTemp = this.jdbcTemplate.query("execute p_list_cargo @codEmpresa=?, @ACCION = ?",
                    new Object[]{ codEmpresa, "L" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) -> {
                        Cargo temp  = new Cargo();
                        temp.setCodEmpresa( rs.getInt(1) );
                        temp.setCodCargoPadre(rs.getInt(2));
                        temp.setDescripcion(rs.getString(3));
                        temp.setCodEmpresa(rs.getInt(4));
                        temp.setNombreEmpresa(rs.getString(5));
                        temp.setCodNivel(rs.getInt(6));
                        temp.setPosicion(rs.getInt(7));
                        temp.setAudUsuario(rs.getInt(8));
                        return temp;
                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: CargoDao en obtenerCargoXEmpresa, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Obtendra los cargos por empresa pero de forma mas informativa
     *
     * @param codEmpresa
     * @return
     */
    @Override
    public List<Cargo> obtenerCargoXEmpresaNew( int codEmpresa ) {

        List<Cargo> lstTemp  = new ArrayList<>();

        try{

            lstTemp = this.jdbcTemplate.query("execute p_list_cargo @codEmpresa=?, @ACCION = ?",
                    new Object[]{ codEmpresa, "B" },
                    new int[]{Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) -> {
                        Cargo temp  = new Cargo();

                        temp.setCodCargo( rs.getInt(1) );
                        temp.setCodCargoPadre(rs.getInt(2));
                        temp.setDescripcion(rs.getString(3));
                        temp.setCodEmpresa(rs.getInt(4));
                        temp.setCodNivel(rs.getInt(5));
                        temp.setNivel(rs.getInt(6));
                        temp.setEstado(rs.getInt(7));
                        temp.setTieneEmpleadosActivos(rs.getInt(8));
                        temp.setTieneEmpleadosTotales(rs.getInt(9));
                        temp.setEstaAsignadoSucursal( rs.getInt(10));
                        temp.setCanDeactivate( rs.getInt(11) );
                        temp.setNumDependientes( rs.getInt(12) );
                        temp.setNumDependenciasTotales( rs.getInt(13) );
                        temp.setNumDependenciasCompletas( rs.getInt(14) );
                        temp.setNumDeDependencias( rs.getInt(15) );
                        temp.setNumHijosActivos( rs.getInt(16) );
                        temp.setNumHijosTotal( rs.getInt(17) );
                        temp.setResumenCompleto( rs.getString(18) );
                        temp.setEstadoPadre( rs.getString(19) );
                        temp.setPosicion( rs.getInt(20) );
                        temp.setEsVisible( rs.getInt(21) );

                        return temp;
                    });
        }catch ( BadSqlGrammarException e ){
            System.out.println("Error: CargoDao en obtenerCargoXEmpresaNew, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;


    }
}
