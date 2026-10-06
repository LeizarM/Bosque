package bo.bosque.com.impexpap.dao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.ClienteSapDto;
import bo.bosque.com.impexpap.dto.EntregaGarantiaRptDto;
import bo.bosque.com.impexpap.dto.FiltroGarantiaDto;
import bo.bosque.com.impexpap.dto.GarantiaDto;
import bo.bosque.com.impexpap.dto.GarantiaResumenClienteDto;
import bo.bosque.com.impexpap.dto.ReciboGarantiaRptDto;
import bo.bosque.com.impexpap.model.GarantiaCbr;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a <b>tcbr_garantia</b>. Todo pasa por procedimiento almacenado via {@link SpHelper}.
 *
 * <h3>Por que todas las llamadas arman su Map a mano</h3>
 * <ul>
 *   <li><b>Escrituras:</b> {@code ejecutarAbm(sp, modelo, accion)} mandaria cada campo del
 *       modelo como parametro. {@code p_abm_GarantiaCbr} no declara {@code @observacion} en el
 *       modelo ni acepta {@code @audFecha} con sentido, y cada rama usa un subconjunto
 *       distinto (B solo toca firmas y protesta). Con el Map va exactamente lo de la rama.</li>
 *   <li><b>Lecturas:</b> los {@code p_list_} de tcbr usan {@code @x IS NULL} como "sin
 *       filtro". {@code ejecutarListado(modelo)} conservaria los 0 de los primitivos y
 *       filtraria por {@code id = 0}.</li>
 * </ul>
 * Los null se omiten del Map: el parametro queda en su DEFAULT NULL, que para estos
 * procedimientos es lo mismo que mandarlo en null.
 *
 * <h3>Clientes repetidos en las ramas del legacy</h3>
 * {@code v_clientesSAP} trae una fila por empresa SAP (1, 5, 7, 8). Las ramas nuevas S, R y G
 * ya agrupan por CardCode; las viejas D y T no, y repiten cada garantia hasta 4 veces. Para
 * los reportes que salen de ellas, este DAO deduplica.
 */
@Repository
public class GarantiaCbrDao implements IGarantiaCbr {

    private static final String SP_ABM = "p_abm_GarantiaCbr";
    private static final String SP_LIST = "p_list_GarantiaCbr";

    private final SpHelper spHelper;

    public GarantiaCbrDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // ------------------------------ Lecturas ------------------------------

    @Override
    public List<GarantiaResumenClienteDto> resumenPorCliente(String buscar) {
        Map<String, Object> p = new LinkedHashMap<>();
        poner(p, "buscar", texto(buscar));
        return spHelper.ejecutarListado(SP_LIST, p, "R", GarantiaResumenClienteDto.class);
    }

    @Override
    public List<GarantiaDto> listar(FiltroGarantiaDto f) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (f != null) {
            poner(p, "codClienteSAP", texto(f.getCodClienteSAP()));
            poner(p, "estado", texto(f.getEstado()));
            poner(p, "tipoGarantia", texto(f.getTipoGarantia()));
            poner(p, "vencDesde", f.getVencDesde());
            poner(p, "vencHasta", f.getVencHasta());
            poner(p, "regDesde", f.getRegDesde());
            poner(p, "regHasta", f.getRegHasta());
        }
        return spHelper.ejecutarListado(SP_LIST, p, "G", GarantiaDto.class);
    }

    @Override
    public GarantiaDto obtener(long codGarantia) {
        if (codGarantia <= 0) return null;
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codGarantia", codGarantia);
        List<GarantiaDto> filas = spHelper.ejecutarListado(SP_LIST, p, "G", GarantiaDto.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public List<ClienteSapDto> buscarClientesSap(String buscar) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("buscar", texto(buscar));
        return spHelper.ejecutarListado(SP_LIST, p, "S", ClienteSapDto.class);
    }

    // ------------------------------ Escrituras ------------------------------

    @Override
    public RespuestaSp alta(GarantiaCbr g, String observacion) {
        Map<String, Object> p = new LinkedHashMap<>();
        poner(p, "codClienteSAP", texto(g.getCodClienteSAP()));
        poner(p, "montoGarantia", g.getMontoGarantia());
        poner(p, "montoCredito", g.getMontoCredito());
        poner(p, "tiempoPago", g.getTiempoPago());
        poner(p, "fechaInicio", g.getFechaInicio());
        poner(p, "fechaExpiracion", g.getFechaExpiracion());
        poner(p, "montoGarantiaCalc", g.getMontoGarantiaCalc());
        poner(p, "recFirmas", texto(g.getRecFirmas()));
        poner(p, "nroProtesta", texto(g.getNroProtesta()));
        poner(p, "audUsuario", g.getAudUsuario());
        poner(p, "observacion", texto(observacion));
        return spHelper.ejecutarAbmMap(SP_ABM, p, "A");
    }

    @Override
    public RespuestaSp actualizar(GarantiaCbr g) {
        Map<String, Object> p = new LinkedHashMap<>();
        poner(p, "codGarantia", g.getCodGarantia());
        poner(p, "montoGarantia", g.getMontoGarantia());
        poner(p, "montoCredito", g.getMontoCredito());
        poner(p, "tiempoPago", g.getTiempoPago());
        poner(p, "fechaInicio", g.getFechaInicio());
        poner(p, "fechaExpiracion", g.getFechaExpiracion());
        poner(p, "montoGarantiaCalc", g.getMontoGarantiaCalc());
        poner(p, "recFirmas", texto(g.getRecFirmas()));
        poner(p, "nroProtesta", texto(g.getNroProtesta()));
        poner(p, "audUsuario", g.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, "U");
    }

    @Override
    public RespuestaSp actualizarFirmas(long codGarantia, String recFirmas, String nroProtesta, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codGarantia", codGarantia);
        poner(p, "recFirmas", texto(recFirmas));
        poner(p, "nroProtesta", texto(nroProtesta));
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "B");
    }

    // ------------------------------ Reportes ------------------------------

    @Override
    public List<ReciboGarantiaRptDto> recibo(long codGarantia) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codGarantia", codGarantia);
        List<ReciboGarantiaRptDto> filas =
                spHelper.ejecutarListado(SP_LIST, p, "D", ReciboGarantiaRptDto.class);

        // Una fila por garantia (la rama 'D' repite una por empresa SAP del cliente).
        List<ReciboGarantiaRptDto> unicas = new ArrayList<>();
        Set<Long> vistas = new LinkedHashSet<>();
        for (ReciboGarantiaRptDto f : filas) {
            if (vistas.add(f.getCodGarantia())) unicas.add(f);
        }
        return unicas;
    }

    @Override
    public List<EntregaGarantiaRptDto> ultimoTraspaso() {
        List<EntregaGarantiaRptDto> filas = spHelper.ejecutarListado(
                SP_LIST, new LinkedHashMap<String, Object>(), "T", EntregaGarantiaRptDto.class);

        // Una fila por garantia y numeracion corrida (la rama 'T' repite por empresa SAP).
        List<EntregaGarantiaRptDto> unicas = new ArrayList<>();
        Set<String> vistas = new LinkedHashSet<>();
        for (EntregaGarantiaRptDto f : filas) {
            if (vistas.add(f.getNroRecibo())) {
                f.setFila((long) (unicas.size() + 1));
                unicas.add(f);
            }
        }
        return unicas;
    }

    // ------------------------------ Utilidades ------------------------------

    private static void poner(Map<String, Object> p, String nombre, Object valor) {
        if (valor != null) p.put(nombre, valor);
    }

    /** Texto recortado; vacio cuenta como null (para que el SP lo tome como "sin filtro"). */
    private static String texto(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
