package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Movimiento;
import bo.bosque.com.impexpap.model.NivelJerarquico;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class NivelJerarquicoDao implements INivelJerarquico {

    private static final Logger logger = LoggerFactory.getLogger(MovimientoDao.class);

    private static final String SP_ABM = "p_abm_NivelJerarquico";
    private static final String SP_LIST = "p_list_NivelJerarquico";

    private final JdbcTemplate jdbcTemplate;
    private final SpHelper spHelper;

    public NivelJerarquicoDao(JdbcTemplate jdbcTemplate, SpHelper spHelper) {
        this.jdbcTemplate = jdbcTemplate;
        this.spHelper = spHelper;
    }

    @Override
    public List<NivelJerarquico> listar() {
        return spHelper.ejecutarListado(SP_LIST, new LinkedHashMap<String, Object>(), "P", NivelJerarquico.class);
    }

    @Override
    public RespuestaSp registrar(NivelJerarquico escala) {
        boolean alta = escala.getCodNivel() == 0;
        Map<String, Object> p = new LinkedHashMap<>();
        if (!alta) p.put("codNivel", escala.getCodNivel());
        p.put("nivel", escala.getNivel());
        p.put("haberBasico", (double) escala.getHaberBasico());
        p.put("bonoProduccion", (double) escala.getBonoProduccion());
        p.put("fecha", escala.getFecha());
        p.put("audUsuarioI", escala.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, alta ? "I" : "U");
    }

    @Override
    public RespuestaSp eliminar(int codNivel, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codNivel", codNivel);
        p.put("audUsuarioI", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }


    /**
     * Obtendra la lista de todos los niveles de los niveles Jerarquicos
     * @return
     */
    @Override
    public List<NivelJerarquico> getAllNiveles() {


        List<NivelJerarquico> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_NivelJerarquico  @ACCION = ?",
                    new Object[] {  "B" },
                    new int[] {  Types.VARCHAR },
                    (rs, rowNum) -> {
                        NivelJerarquico temp = new NivelJerarquico();

                        temp.setCodNivel(rs.getInt(1));
                        temp.setNivel(rs.getInt(2));
                        temp.setHaberBasico(rs.getFloat(3));
                        temp.setBonoProduccion(rs.getFloat(4));
                        temp.setFecha(rs.getDate(5));
                        temp.setActivo(rs.getInt(6));



                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar los niveles de los niveles Jerarquicos"); // Usamos el método auxiliar
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
