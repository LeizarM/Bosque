package bo.bosque.com.impexpap.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.ChequePendienteFilaDto;
import bo.bosque.com.impexpap.dto.VerificacionDepositoFilaDto;
import bo.bosque.com.impexpap.model.ChVerificacionDeposito;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a <b>tch_verificacionDeposito</b>. Todo pasa por procedimiento almacenado via {@link SpHelper}.
 *
 * <p>Las ramas A, C y D de {@code p_list_VerificacionDeposito} devuelven 13 columnas con nombres repetidos
 * ({@code datoEstado} dos veces), asi que se leen POR POSICION, como hace el legacy
 * ({@code VerificacionDepositoDAO}). El {@code Map} de parametros se arma a mano porque el procedimiento usa
 * {@code @x IS NULL} como "sin filtro": un parametro que no se manda queda en NULL.
 */
@Repository
public class ChVerificacionDepositoDAO implements IChVerificacionDeposito {

    private static final String SP_ABM = "p_abm_VerificacionDeposito";
    private static final String SP_LIST = "p_list_VerificacionDeposito";

    private final SpHelper spHelper;

    public ChVerificacionDepositoDAO(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /** Un {@code date} como dia, sin zona. */
    private static LocalDate dia(ResultSet rs, int col) throws SQLException {
        java.sql.Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    /** Las 13 columnas de la rama A, por posicion. */
    static final RowMapper<VerificacionDepositoFilaDto> MAPA_VERIFICACION = (rs, i) -> {
        VerificacionDepositoFilaDto f = new VerificacionDepositoFilaDto();
        f.setCodvd(ArgsSp.entero(rs, 1));
        f.setCodCheque(ArgsSp.largo(rs, 2));
        f.setCodBanco(ArgsSp.largo(rs, 3));
        f.setFechaBanco(dia(rs, 4));
        f.setObservacion(rs.getString(5));
        f.setDatoBanco(rs.getString(6));
        f.setNroCheque(rs.getString(7));          // texto: hay numeros con guion o con ?
        f.setMontoCheque(ArgsSp.doble(rs, 8));
        f.setDatoBancoCheque(rs.getString(9));
        f.setDatoEstadoCheque(rs.getString(10));
        f.setFechaCobrarCheque(dia(rs, 11));
        f.setEstado(rs.getString(12));
        f.setDatoEstado(rs.getString(13));
        return f;
    };

    /** Las 13 columnas de las ramas C y D, por posicion; solo se leen las que son del cheque. */
    static final RowMapper<ChequePendienteFilaDto> MAPA_PENDIENTE = (rs, i) -> {
        ChequePendienteFilaDto f = new ChequePendienteFilaDto();
        f.setCodCheque(ArgsSp.largo(rs, 2));
        f.setCodBanco(ArgsSp.largo(rs, 3));       // elCodBanco: el banco DEL CHEQUE
        f.setNroCheque(rs.getString(7));
        f.setMontoCheque(ArgsSp.doble(rs, 8));
        f.setDatoBancoCheque(rs.getString(9));
        f.setDatoEstadoCheque(rs.getString(10));
        f.setFechaCobrarCheque(dia(rs, 11));
        return f;
    };

    // ------------------------------ Lecturas ------------------------------

    @Override
    public List<VerificacionDepositoFilaDto> listar(LocalDate fechaBanco) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (fechaBanco != null) p.put("fechaBanco", java.sql.Date.valueOf(fechaBanco));
        return spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "A", MAPA_VERIFICACION);
    }

    @Override
    public VerificacionDepositoFilaDto obtener(int codvd) {
        if (codvd <= 0) return null;
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codvd", codvd);
        List<VerificacionDepositoFilaDto> filas = spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "A", MAPA_VERIFICACION);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public List<ChequePendienteFilaDto> sinRegularizarDelDia(LocalDate fechaCobranza, String estadoCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (fechaCobranza != null) p.put("fechaBanco", java.sql.Date.valueOf(fechaCobranza));   // la rama C la compara con fechaCobrar
        ArgsSp.poner(p, "estado", ArgsSp.texto(estadoCheque));                                     // @estado = el del CHEQUE
        return spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "C", MAPA_PENDIENTE);
    }

    @Override
    public List<ChequePendienteFilaDto> sinRegularizarHastaHoy(String estadoCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "estado", ArgsSp.texto(estadoCheque));
        return spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "D", MAPA_PENDIENTE);
    }

    @Override
    public ChequePendienteFilaDto sinRegularizarDeUnCheque(long codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        List<ChequePendienteFilaDto> filas = spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "C", MAPA_PENDIENTE);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public boolean tieneVerificacionValida(long codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("estad", "Y");                 // @estad = el estado de la VERIFICACION
        p.put("codCheque", codCheque);
        return !spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "A", (rs, i) -> rs.getInt(1)).isEmpty();
    }

    // ------------------------------ Escrituras ------------------------------

    @Override
    public RespuestaSp registrar(ChVerificacionDeposito v) {
        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "codCheque", v.getCodCheque());
        ArgsSp.poner(p, "codBanco", v.getCodBanco());
        if (v.getFechaBanco() != null) p.put("fechaBanco", java.sql.Date.valueOf(v.getFechaBanco()));
        ArgsSp.poner(p, "observacion", v.getObservacion());
        ArgsSp.poner(p, "estado", v.getEstado());
        ArgsSp.poner(p, "audUsuario", v.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I");
    }

    @Override
    public RespuestaSp actualizar(ChVerificacionDeposito v) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codvd", v.getCodvd());
        ArgsSp.poner(p, "codBanco", v.getCodBanco());
        if (v.getFechaBanco() != null) p.put("fechaBanco", java.sql.Date.valueOf(v.getFechaBanco()));
        ArgsSp.poner(p, "observacion", v.getObservacion());
        ArgsSp.poner(p, "estado", v.getEstado());
        ArgsSp.poner(p, "audUsuario", v.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, "U");
    }
}
