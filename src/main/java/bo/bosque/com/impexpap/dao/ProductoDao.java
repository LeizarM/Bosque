package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductoDao implements IProducto {

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Para registrar el producto o familia
     *
     * @param producto
     * @param acc
     * @return
     */
    @Override
    public boolean registrarProducto( Producto producto, String acc ) {

        int resp;

        try{

            resp = this.jdbcTemplate.update("execute p_abm_producto   @codigoFamilia=?, @idProveedorSap=?, @idGrpFamiliaSap=?, @idPresentacion=?, @idTipo=? ,@idRangoGram=?, @gramaje=?, @formato=?, @idColor=?, @estado=?, @costoTM=?, @idPropuestaAprobada=? ,@audUsuario=?, @ACCION=?",
                    ps -> {
                        ps.setEscapeProcessing(true);
                        ps.setQueryTimeout(180);
                        ps.setInt(1, producto.getCodigoFamilia());
                        ps.setInt(2, producto.getIdProveedorSap());
                        ps.setInt(3, producto.getIdGrpFamiliaSap());
                        ps.setInt(4, producto.getIdPresentacion());
                        ps.setInt(5, producto.getIdTipo());
                        ps.setInt(6, producto.getIdRangoGramaje());
                        ps.setString(7, producto.getGramaje());
                        ps.setString(8, producto.getFormato());
                        ps.setInt(9, producto.getIdColor());
                        ps.setInt(10, producto.getEstado());
                        ps.setFloat(11, producto.getCostoTM());
                        ps.setInt(12, producto.getIdPropuestaAprobada());
                        ps.setInt(13, producto.getAudUsuario());
                        ps.setString(14, acc);
                    });

        }catch( BadSqlGrammarException e ){
            System.out.println("Error: ProductoDao en registrarProducto, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp!=0;

    }

    /**
     * Metdodo que devolvera los preveedores del SAP registrados en el bosque
     *
     * @return
     */
    @Override
    public List<Producto> listadoProveedor() {
        List<Producto> lstTemp = new ArrayList<Producto>();
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_producto @ACCION=?",
                    new Object[] { "E" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        Producto temp = new Producto();

                        temp.setCodigoFamilia( rs.getInt(1 ) );
                        temp.setNombreProveedor( rs.getString(2 ) );
                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: listadoProveedor en ProductoDao, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Metdodo que devolvera los familias
     *
     * @return
     */
    public List<Producto> listadoFamilia( int codFamilia ) {
        List<Producto> lstTemp = new ArrayList<Producto>();
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_producto @codigoFamilia=?, @ACCION=?",
                    new Object[] { codFamilia, "C" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Producto temp = new Producto();

                        temp.setIdGrpFamiliaSap( rs.getInt(1 ) );
                        temp.setNombreFamilia( rs.getString(2 ) );
                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: listadoFamilia en ProductoDao, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

    /**
     * Metodo que devolvera la lista de todos las familias que pertenecen a un grupo de familia del sap
     * @param idGrpFamiliaSap
     * @return
     */
    @Override
    public List<Producto> listadoFamiliaXGrupo( int idGrpFamiliaSap ) {
        List<Producto> lstTemp = new ArrayList<Producto>();
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_producto @idGrpFamiliaSap=? ,@ACCION=?",
                    new Object[] { idGrpFamiliaSap, "D" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Producto temp = new Producto();

                        temp.setCodigoFamilia( rs.getInt(1 ) );
                        temp.setNombreProveedor( rs.getString(2));
                        temp.setNombreFamilia( rs.getString(3 ) );
                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: listadoFamiliaXGrupo en ProductoDao, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;

    }

    /**
     * Devolvera un objecto de acuerrdo al codigo de familia
     *
     * @param codigoFamilia
     * @return
     */
    public Producto cargarDatoFamilia( int codigoFamilia ) {
        Producto p = new Producto();
        try {
            p =  this.jdbcTemplate.queryForObject("execute p_list_producto @codigoFamilia=?, @ACCION=?",
                    new Object[] { codigoFamilia, "L" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {

                        Producto temp = new Producto();

                        temp.setCodigoFamilia( rs.getInt(1 ) );
                        temp.setNombreProveedor( rs.getString(2));
                        temp.setNombreFamilia( rs.getString(3 ) );
                        temp.setPresentacion(rs.getString(4) );
                        temp.setTipo( rs.getString(5) );
                        temp.setRangoGramaje( rs.getString(6) );
                        temp.setGramaje( rs.getString(7) );
                        temp.setFormato( rs.getString(8) );
                        temp.setColor( rs.getString(9) );
                        temp.setEstado( rs.getInt(10));
                        temp.setCostoTM( rs.getFloat(11) );
                        temp.setIdPropuestaAprobada( rs.getInt(12) );
                        temp.setAudUsuario( rs.getInt(13) );


                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: cargarDatoFamilia en ProductoDao, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            p = new Producto();
            this.jdbcTemplate = null;
        }

        return p;
    }

}
