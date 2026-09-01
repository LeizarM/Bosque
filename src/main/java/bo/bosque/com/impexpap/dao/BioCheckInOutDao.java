package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioCheckInOut;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BioCheckInOutDao implements IBioCheckInOut {

    private static final String SP_ABM  = "p_abm_BioCHECKINOUT";
    private static final String SP_LIST = "p_list_BioCHECKINOUT";

    private final SpHelper spHelper;
    private final JdbcTemplate jdbcTemplate;

    public BioCheckInOutDao(SpHelper spHelper, JdbcTemplate jdbcTemplate) {
        this.spHelper = spHelper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Declarado a mano porque {@code p_abm_BioCHECKINOUT} no sigue el
     * contrato {@code error/errormsg/idGenerado} que asume
     * {@code SpHelper.ejecutarAbm}, y porque {@code SimpleJdbcCall} con
     * {@code withoutProcedureColumnMetaDataAccess()} arma una llamada
     * POSICIONAL con sólo los parámetros declarados — {@code {call
     * proc(?,?,?)}} bindea esos tres `?` a las primeras tres posiciones REALES
     * del SP (@USERID, @CHECKTIME, @CHECKTYPE), no a las que uno declaró.
     * Con {@code @CHECKTIME} yendo al primer `?` terminaba en {@code @USERID}
     * (int) → "conversión implícita de datetime a int no permitida".
     *
     * <p>Mismo truco que {@code SpHelper.ejecutarAbmMap}: {@code EXEC} con
     * parámetros NOMBRADOS (no posicionales) — así el orden no importa y los
     * parámetros que no se mandan quedan en su DEFAULT de T-SQL — envuelto en
     * {@code DECLARE}/{@code SELECT} para leer el {@code OUTPUT}.
     */
    @Override
    public String dispararImportacionMensual(Date checkTime) {
        log.info("Disparando importacion mensual de marcaciones, CHECKTIME={}", checkTime);

        String sql = "DECLARE @__retorna VARCHAR(100) = N''; "
                + "EXEC " + SP_ABM + " @CHECKTIME=?, @ACCION=?, @RETORNA=@__retorna OUTPUT; "
                + "SELECT @__retorna AS RETORNA;";

        return jdbcTemplate.execute(
                (Connection con) -> con.prepareStatement(sql),
                (PreparedStatement ps) -> {
                    ps.setTimestamp(1, new Timestamp(checkTime.getTime()));
                    ps.setString(2, "B");

                    boolean hasResult = ps.execute();
                    while (!hasResult) {
                        if (ps.getUpdateCount() == -1) break;
                        hasResult = ps.getMoreResults();
                    }
                    if (!hasResult) {
                        return null;
                    }
                    try (ResultSet rs = ps.getResultSet()) {
                        return rs.next() ? rs.getString("RETORNA") : null;
                    }
                }
        );
    }

    @Override
    public List<BioCheckInOut> listar(Map<String, Object> filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", BioCheckInOut.class);
    }
}
