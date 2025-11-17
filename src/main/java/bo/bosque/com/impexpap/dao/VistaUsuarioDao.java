package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TipoContenedor;
import bo.bosque.com.impexpap.model.VistaUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class VistaUsuarioDao implements  IVistaUsuario{

    private static final Logger logger = LoggerFactory.getLogger(VistaUsuario.class);

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;



    /**
     * Para registrar la vista por usuario
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarVistaUsuario( VistaUsuario mb, String acc ) {




        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_VistaUsuario  @codUsuario = ?, @codVista = ?, @nivelAcceso = ?, @autorizador = ?,  @audUsuarioI = ?, @ACCION = ?",
                    ps -> {

                        ps.setInt(1, mb.getCodUsuario());
                        ps.setInt(2, mb.getCodVista());
                        ps.setInt(3, mb.getNivelAcceso());
                        ps.setInt(4, mb.getAutorizador());
                        ps.setInt(5, mb.getAudUsuarioI());
                        ps.setString(6, acc);

                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: VistaUsuarioDao en registrarVistaUsuario, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;
    }

    /**
     * Listara los permisos de vistas y botones para un usuario para que posteriormente se pueda armar un arbol
     *
     * @param codUsuario
     * @return
     */
    @Override
    public List<VistaUsuario> lstPermisosVistasYBotones( int codUsuario ) {

        List<VistaUsuario> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute dbo.p_list_VistaUsuario @ACCION = ?, @codUsuario = ?",
                    new Object[] { "F", codUsuario },
                    new int[] { Types.VARCHAR, Types.INTEGER },
                    (rs, rowNum) -> {
                        VistaUsuario temp = new VistaUsuario();

                        temp.setFila(rs.getInt(1));
                        temp.setCodUsuario(rs.getInt(2));
                        temp.setCodVista(rs.getInt(3));
                        temp.setCodVistaPadre(rs.getInt(4));
                        temp.setCodBoton(rs.getInt(5));
                        temp.setDireccion(rs.getString(6));
                        temp.setNombreComponente(rs.getString(7));
                        temp.setModulo(rs.getString(8));
                        temp.setVista(rs.getString(9));
                        temp.setBoton(rs.getString(10));
                        temp.setDescripcion( rs.getString(11) );
                        temp.setImagen( rs.getString( 12 ) );
                        temp.setNivelAcceso( rs.getInt(13) );
                        temp.setNivelAccesoBoton( rs.getInt(14) );
                        temp.setAutorizador( rs.getInt(15) );


                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar en lstPermisosVistasYBotones"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;


    }


    /**
     * Método auxiliar para registrar errores de acceso a datos
     * @param ex Excepción ocurrida
     * @param mensaje Mensaje descriptivo del error
     */
    private void logDataAccessException(DataAccessException ex, String mensaje) {
        logger.error(mensaje);
        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof SQLException) {
            SQLException sqlEx = (SQLException) rootCause;
            logger.error("Código de error SQL: {}, Estado SQL: {}",
                    sqlEx.getErrorCode(),
                    sqlEx.getSQLState());
        }
        logger.error("Detalle del error:", ex);
    }
}
