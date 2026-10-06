package bo.bosque.com.impexpap.dao;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.ChequeFiltroDto;
import bo.bosque.com.impexpap.dto.ChequeResumenDto;
import bo.bosque.com.impexpap.dto.HoraAccionDto;
import bo.bosque.com.impexpap.model.ChCheque;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a <b>tch_cheque</b>. Todo pasa por procedimiento almacenado via {@link SpHelper}.
 *
 * <h3>Por que el Map se arma a mano</h3>
 * <ul>
 *   <li><b>Lecturas:</b> {@code p_list_Cheque} usa {@code @x IS NULL} como "sin filtro". Un filtro que
 *       no se manda queda en su DEFAULT NULL; un {@code 0} o un texto vacio filtraria por ese valor.
 *       El legacy tambien manda NULL cuando el campo esta vacio o es 0.</li>
 *   <li><b>Escrituras:</b> cada rama de {@code p_abm_Cheque} usa un subconjunto de parametros. Con el
 *       {@code Map} va exactamente lo de la rama.</li>
 * </ul>
 *
 * <h3>Por que se leen por posicion</h3>
 * Las ramas A, B y C de {@code p_list_Cheque} devuelven 28 columnas sin alias o con nombres repetidos
 * ({@code descripcion} x3). {@code BeanPropertyRowMapper} no puede con eso; el legacy las lee con
 * {@code rs.getXxx(n)} y {@link #MAPA_FILA} hace lo mismo. Ver {@link ChequeFilaDto} para el orden.
 */
@Repository
public class ChChequeDAO implements IChCheque {

    private static final String SP_ABM = "p_abm_Cheque";
    private static final String SP_LIST = "p_list_Cheque";

    private final SpHelper spHelper;

    public ChChequeDAO(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /** Las 28 columnas de las ramas A, B y C, por posicion. */
    static final RowMapper<ChequeFilaDto> MAPA_FILA = (rs, i) -> {
        ChequeFilaDto f = new ChequeFilaDto();
        f.setCodCheque(ArgsSp.entero(rs, 1));
        f.setFechaRecepcion(ArgsSp.fecha(rs, 2));
        f.setNrocheque(rs.getString(3));
        f.setCodCliente(rs.getString(4));
        f.setDatoCliente(rs.getString(5));
        f.setaOrdenDe(rs.getString(6));
        f.setFechaCheque(ArgsSp.fecha(rs, 7));
        f.setFechaCobrar(ArgsSp.fecha(rs, 8));
        f.setMonto(ArgsSp.doble(rs, 9));
        f.setMoneda(rs.getString(10));
        f.setDescMoneda(rs.getString(11));
        f.setTipo(rs.getString(12));
        f.setDescTipo(rs.getString(13));
        f.setEstado(rs.getString(14));
        f.setDescEstado(rs.getString(15));
        f.setCodBanco(ArgsSp.entero(rs, 16));
        f.setNombreBanco(rs.getString(17));
        f.setCodEmpleado(ArgsSp.entero(rs, 18));
        f.setDatoEmpleado(rs.getString(19));
        f.setReciboManual(rs.getString(20));
        f.setObservacion(rs.getString(21));
        f.setCodSucursal(ArgsSp.largo(rs, 22));
        f.setNroRecibo(ArgsSp.largo(rs, 23));
        f.setNroTalonario(rs.getString(24));
        f.setCodEmpresa(ArgsSp.entero(rs, 25));
        f.setDatoEmpresa(rs.getString(26));
        f.setAudUsuario(ArgsSp.entero(rs, 27));
        f.setAudFecha(rs.getString(28));   // datetimeoffset: texto (getTimestamp falla con jTDS)
        return f;
    };

    // ------------------------------ Lecturas ------------------------------

    @Override
    public List<ChequeFilaDto> listar(ChequeFiltroDto f) {
        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "nrocheque", ArgsSp.texto(f.getNroCheque()));
        ArgsSp.poner(p, "datoCliente", ArgsSp.texto(f.getCliente()));
        ArgsSp.poner(p, "tipo", ArgsSp.texto(f.getTipo()));
        ArgsSp.poner(p, "estado", ArgsSp.texto(f.getEstado()));
        ArgsSp.poner(p, "fechaCobrar", f.getFechaCobro());
        ArgsSp.poner(p, "codBanco", ArgsSp.idONulo(f.getCodBanco()));
        ArgsSp.poner(p, "audFecha", f.getFechaRecepcion());     // dia de la accion REC
        p.put("codSucursal", f.getCodSucursal());

        String accion = "COBRO".equalsIgnoreCase(f.getOrden()) ? "B" : "A";
        List<ChequeFilaDto> filas = spHelper.ejecutarListadoPorPosicion(SP_LIST, p, accion, MAPA_FILA);
        numerar(filas);
        return filas;
    }

    @Override
    public ChequeFilaDto obtener(int codCheque) {
        if (codCheque <= 0) return null;
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        List<ChequeFilaDto> filas = spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "A", MAPA_FILA);
        numerar(filas);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public boolean existeDuplicado(String nroCheque, String codCliente, int codBanco, long codSucursal, Date fechaCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "nrocheque", nroCheque);
        ArgsSp.poner(p, "codCliente", codCliente);
        p.put("codBanco", codBanco);
        p.put("codSucursal", codSucursal);
        ArgsSp.poner(p, "fechaCheque", fechaCheque);
        return !spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "L", (rs, i) -> rs.getInt(1)).isEmpty();
    }

    @Override
    public boolean talonarioYReciboValidos(String nroTalonario, String reciboManual, int codEmpresa) {
        // La rama 'M' hace CONVERT(bigint, @reciboManual): con un recibo que no es solo digitos el SP
        // revienta. El legacy lo trataba como "no valido"; aqui se corta antes, con el mismo resultado,
        // para que una caida real de la base no se confunda con un dato mal escrito. El recorte es porque
        // CONVERT acepta espacios en los extremos y el formato del recibo los permite; al SP va el original.
        if (reciboManual == null || !reciboManual.trim().matches("\\d{1,18}")) return false;

        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "nroTalonario", nroTalonario);
        p.put("reciboManual", reciboManual);
        p.put("codEmpresa", codEmpresa);
        List<Integer> filas = spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "M", (rs, i) -> rs.getInt(1));
        return !filas.isEmpty() && filas.get(0) == 1;   // el legacy exige exactamente 1 (ChequesDao.validTalonYRecibo)
    }

    @Override
    public String codigoBotones(int codCheque) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        List<String> filas = spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "K", (rs, i) -> rs.getString(1));
        return filas.isEmpty() || filas.get(0) == null ? "0000" : filas.get(0);
    }

    @Override
    public List<ChequeFilaDto> sinCustodio(long codSucursal, Date fechaCobro) {
        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "audFecha", fechaCobro);   // en la rama C filtra por fecha de COBRO
        p.put("codSucursal", codSucursal);
        List<ChequeFilaDto> filas = spHelper.ejecutarListadoPorPosicion(SP_LIST, p, "C", MAPA_FILA);
        numerar(filas);
        return filas;
    }

    @Override
    public List<ChequeResumenDto> chequesDeSucursal(long codSucursal) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codSucursal", codSucursal);
        return spHelper.ejecutarListado(SP_LIST, p, "J", ChequeResumenDto.class);
    }

    @Override
    public List<HoraAccionDto> custodiasDelDia(long codSucursal, Date fecha) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codSucursal", codSucursal);
        ArgsSp.poner(p, "audFecha", fecha);
        return spHelper.ejecutarListado(SP_LIST, p, "I", HoraAccionDto.class);
    }

    // ------------------------------ Escrituras ------------------------------

    @Override
    public RespuestaSp alta(ChCheque c, String observacion) {
        Map<String, Object> p = columnas(c);
        ArgsSp.poner(p, "observacion", observacion);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I");
    }

    @Override
    public RespuestaSp actualizar(ChCheque c, String observacion) {
        Map<String, Object> p = columnas(c);
        p.put("codCheque", c.getCodCheque());
        ArgsSp.poner(p, "observacion", observacion);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "U");
    }

    @Override
    public RespuestaSp cambiarEstado(int codCheque, String estado, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        p.put("estado", estado);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "E");
    }

    @Override
    public RespuestaSp cambiarFechaCobro(int codCheque, Date fechaCobrar, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codCheque", codCheque);
        p.put("fechaCobrar", fechaCobrar);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "F");
    }

    /**
     * Las columnas que escriben el alta y la edicion. {@code nroRecibo} no va: el alta lo calcula el
     * procedimiento y la edicion no lo toca. {@code codEmpleado} = 0 SI se manda (0 = lo dejo el
     * cliente, como lo escribe el legacy).
     */
    private static Map<String, Object> columnas(ChCheque c) {
        Map<String, Object> p = new LinkedHashMap<>();
        ArgsSp.poner(p, "nrocheque", c.getNrocheque());
        ArgsSp.poner(p, "codCliente", c.getCodCliente());
        ArgsSp.poner(p, "aOrdenDe", c.getaOrdenDe());
        ArgsSp.poner(p, "fechaCheque", c.getFechaCheque());
        ArgsSp.poner(p, "fechaCobrar", c.getFechaCobrar());
        ArgsSp.poner(p, "monto", c.getMonto());
        ArgsSp.poner(p, "moneda", c.getMoneda());
        ArgsSp.poner(p, "tipo", c.getTipo());
        ArgsSp.poner(p, "estado", c.getEstado());
        ArgsSp.poner(p, "codBanco", c.getCodBanco());
        ArgsSp.poner(p, "codEmpleado", c.getCodEmpleado());
        ArgsSp.poner(p, "reciboManual", c.getReciboManual());
        ArgsSp.poner(p, "codSucursal", c.getCodSucursal());
        ArgsSp.poner(p, "nroTalonario", c.getNroTalonario());
        ArgsSp.poner(p, "codEmpresa", c.getCodEmpresa());
        ArgsSp.poner(p, "audUsuario", c.getAudUsuario());
        return p;
    }

    private static void numerar(List<ChequeFilaDto> filas) {
        int n = 1;
        for (ChequeFilaDto f : filas) f.setFila(n++);
    }
}
