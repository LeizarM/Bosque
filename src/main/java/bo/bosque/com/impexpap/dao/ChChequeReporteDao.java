package bo.bosque.com.impexpap.dao;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.ChequeCobranzaRptDto;
import bo.bosque.com.impexpap.dto.ChequeCustodioRptDto;
import bo.bosque.com.impexpap.dto.ChequeRecibidoRptDto;
import bo.bosque.com.impexpap.dto.ChequeTraspasoRptDto;
import bo.bosque.com.impexpap.dto.HoraTraspasoChequeDto;
import bo.bosque.com.impexpap.dto.NotaRemisionRptDto;
import bo.bosque.com.impexpap.dto.ReciboChequeRptDto;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Ver {@link IChChequeReporte}. Todo pasa por {@code p_list_Cheque} y {@code p_list_NotaRemision}.
 *
 * <p>Cada rama se lee <b>por posicion</b>, como el legacy (y como {@code ChChequeDAO}): los nombres de las columnas
 * son los {@code <field>} de las plantillas y los tipos de las filas son los que declaran, ver los DTO.
 */
@Repository
public class ChChequeReporteDao implements IChChequeReporte {

    private static final String SP = "p_list_Cheque";
    private static final String SP_NOTAS = "p_list_NotaRemision";

    private final SpHelper spHelper;

    public ChChequeReporteDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<ChequeRecibidoRptDto> recibidos(long codSucursal, String desde, String hasta) {
        Map<String, Object> p = sucursal(codSucursal);
        ArgsSp.poner(p, "cadFecha", ArgsSp.texto(desde));
        ArgsSp.poner(p, "cadFechaFin", ArgsSp.texto(hasta));
        return spHelper.ejecutarListadoPorPosicion(SP, p, "R", (rs, i) -> new ChequeRecibidoRptDto(
                ArgsSp.largo(rs, 1), rs.getTimestamp(2), rs.getTimestamp(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                rs.getDate(10), ArgsSp.doble(rs, 11), rs.getString(12), rs.getString(13)));
    }

    @Override
    public List<ChequeCobranzaRptDto> cobranzas(long codSucursal, String desde, String hasta, String estado,
                                                String codCliente) {
        Map<String, Object> p = sucursal(codSucursal);
        ArgsSp.poner(p, "cadFecha", ArgsSp.texto(desde));
        ArgsSp.poner(p, "cadFechaFin", ArgsSp.texto(hasta));
        ArgsSp.poner(p, "estado", ArgsSp.texto(estado));
        ArgsSp.poner(p, "datoCliente", ArgsSp.texto(codCliente));
        return spHelper.ejecutarListadoPorPosicion(SP, p, "P", (rs, i) -> new ChequeCobranzaRptDto(
                ArgsSp.largo(rs, 1), rs.getTimestamp(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getString(7), rs.getString(8), rs.getDate(9),
                ArgsSp.doble(rs, 10), rs.getString(11), rs.getString(12)));
    }

    @Override
    public List<ChequeCustodioRptDto> custodio(long codSucursal, String fecha, Integer codEmpleado) {
        Map<String, Object> p = sucursal(codSucursal);
        ArgsSp.poner(p, "cadFecha", ArgsSp.texto(fecha));
        // Para esta rama el cobrador viaja en @estado (como texto): el SP lo convierte a int.
        ArgsSp.poner(p, "estado", ArgsSp.idONulo(codEmpleado) == null ? null : String.valueOf(codEmpleado));
        return spHelper.ejecutarListadoPorPosicion(SP, p, "S", (rs, i) -> new ChequeCustodioRptDto(
                ArgsSp.largo(rs, 1), rs.getTimestamp(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), ArgsSp.doble(rs, 7), rs.getString(8), ArgsSp.entero(rs, 9),
                rs.getString(10), rs.getString(11), null));
    }

    @Override
    public ReciboChequeRptDto recibo(long codCheque, long codSucursal) {
        Map<String, Object> p = sucursal(codSucursal);
        p.put("codCheque", codCheque);
        List<ReciboChequeRptDto> filas = spHelper.ejecutarListadoPorPosicion(SP, p, "D", (rs, i) -> new ReciboChequeRptDto(
                ArgsSp.entero(rs, 1), rs.getString(2), rs.getTimestamp(3), rs.getString(4), rs.getString(5),
                rs.getString(6), ArgsSp.doble(rs, 7), rs.getString(8), rs.getString(9), rs.getString(10),
                rs.getString(11)));
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public Integer ultimoChequeDelUsuario(int codUsuario, long codSucursal) {
        Map<String, Object> p = sucursal(codSucursal);
        p.put("audUsuario", codUsuario);
        List<Integer> filas = spHelper.ejecutarListadoPorPosicion(SP, p, "E", (rs, i) -> ArgsSp.entero(rs, 1));
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public List<ChequeTraspasoRptDto> ultimoTraspaso(long codSucursal) {
        return spHelper.ejecutarListadoPorPosicion(SP, sucursal(codSucursal), "T", FILA_TRASPASO);
    }

    @Override
    public List<ChequeTraspasoRptDto> reimpresionTraspaso(long codSucursal, long codAccion) {
        Map<String, Object> p = sucursal(codSucursal);
        // El codigo de la accion viaja en @estado (como texto): el SP lo convierte a bigint.
        p.put("estado", String.valueOf(codAccion));
        return spHelper.ejecutarListadoPorPosicion(SP, p, "H", FILA_TRASPASO);
    }

    /** Las dos nominas ('T' y 'H') tienen las mismas 11 columnas. */
    private static final org.springframework.jdbc.core.RowMapper<ChequeTraspasoRptDto> FILA_TRASPASO =
            (rs, i) -> new ChequeTraspasoRptDto(
                    ArgsSp.largo(rs, 1), rs.getTimestamp(2), rs.getString(3), rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getString(7), rs.getDate(8), ArgsSp.doble(rs, 9), rs.getString(10),
                    rs.getString(11));

    @Override
    public List<HoraTraspasoChequeDto> horasDeTraspaso(long codSucursal, Date fecha) {
        Map<String, Object> p = sucursal(codSucursal);
        ArgsSp.poner(p, "audFecha", fecha);
        return spHelper.ejecutarListadoPorPosicion(SP, p, "G", (rs, i) ->
                new HoraTraspasoChequeDto(ArgsSp.entero(rs, 1), rs.getString(2)));
    }

    @Override
    public List<NotaRemisionRptDto> notasDeRemision(long codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        return spHelper.ejecutarListadoPorPosicion(SP_NOTAS, p, "A", (rs, i) -> new NotaRemisionRptDto(
                ArgsSp.entero(rs, 1), rs.getString(2), ArgsSp.entero(rs, 3), rs.getDate(4), rs.getString(5)));
    }

    private static Map<String, Object> sucursal(long codSucursal) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codSucursal", codSucursal);
        return p;
    }
}
