package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CiteArea;
import bo.bosque.com.impexpap.model.CiteEmpleado;
import bo.bosque.com.impexpap.model.CiteGestion;
import bo.bosque.com.impexpap.model.CiteTipoDocumento;
import bo.bosque.com.impexpap.model.CopiaArch;
import bo.bosque.com.impexpap.model.CopiaEncabezado;
import bo.bosque.com.impexpap.model.Documento;
import bo.bosque.com.impexpap.model.Remitente;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del acceso a datos de Cartas CITE.
 *
 * <p><b>Los listados usan el overload de Map, no el de modelo.</b>
 * {@code ejecutarListado(spName, model, ...)} serializa el objeto entero y
 * conserva los ceros de los primitivos; este SP trata 0 como "sin filtro" en
 * empresa y tipo, pero {@code idDocumento=0} le llegaría igual y no filtraría
 * nada útil. Mandando exactamente los parámetros que hacen falta, el resto
 * queda en su DEFAULT NULL. Es la misma regla que siguen {@code CotizacionesDao}
 * y {@code AsientosDao}.
 */
@Repository
public class DocumentoCiteDao implements IDocumentoCite {

    private static final String SP_ABM_DOC     = "p_abm_tcr_Documento";
    private static final String SP_ABM_CC      = "p_abm_tcr_CopiaArch";
    private static final String SP_ABM_CE      = "p_abm_tcr_CopiaEncab";
    private static final String SP_ABM_REM     = "p_abm_tcr_Remitente";
    private static final String SP_LIST        = "p_list_tcr_Documento";

    private final SpHelper spHelper;

    public DocumentoCiteDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // ══════════════════════ ESCRITURA ══════════════════════

    @Override
    public RespuestaSp registrarDocumento(Documento mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM_DOC, mb, acc);
    }

    @Override
    public RespuestaSp registrarCopiaArch(CopiaArch mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM_CC, mb, acc);
    }

    @Override
    public RespuestaSp registrarCopiaEncabezado(CopiaEncabezado mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM_CE, mb, acc);
    }

    @Override
    public RespuestaSp registrarRemitente(Remitente mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM_REM, mb, acc);
    }

    // ══════════════════════ LECTURA ══════════════════════

    @Override
    public List<Documento> listarDocumentos(Date fechaDesde, Date fechaHasta, long idTipoDoc,
                                            long codEmpresa, long codUsuario, String buscar,
                                            int pagina, int tamanoPagina) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("fechaDesde",   fechaDesde);
        filtro.put("fechaHasta",   fechaHasta);
        filtro.put("idTipoDoc",    idTipoDoc);
        filtro.put("codEmpresa",   codEmpresa);
        filtro.put("codUsuario",   codUsuario);
        filtro.put("buscar",       buscar);
        filtro.put("pagina",       pagina);
        filtro.put("tamanoPagina", tamanoPagina);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Documento.class);
    }

    @Override
    public Documento obtenerDocumento(long idDocumento) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idDocumento", idDocumento);
        return primero(spHelper.ejecutarListado(SP_LIST, filtro, "R", Documento.class));
    }

    @Override
    public CiteGestion obtenerSiguienteCite(long idTipoDoc, long codEmpresa) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTipoDoc",  idTipoDoc);
        filtro.put("codEmpresa", codEmpresa);
        return primero(spHelper.ejecutarListado(SP_LIST, filtro, "A", CiteGestion.class));
    }

    @Override
    public List<CiteTipoDocumento> listarTiposDocumento() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "T", CiteTipoDocumento.class);
    }

    @Override
    public List<CiteArea> listarAreas(long codEmpresa) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpresa", codEmpresa);
        return spHelper.ejecutarListado(SP_LIST, filtro, "E", CiteArea.class);
    }

    @Override
    public List<CiteEmpleado> listarEmpleados() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "M", CiteEmpleado.class);
    }

    @Override
    public CiteEmpleado obtenerEmpleado(long codEmpleado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);
        return primero(spHelper.ejecutarListado(SP_LIST, filtro, "C", CiteEmpleado.class));
    }

    @Override
    public CiteEmpleado obtenerFirmaUsuario(long codUsuario) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codUsuario", codUsuario);
        return primero(spHelper.ejecutarListado(SP_LIST, filtro, "U", CiteEmpleado.class));
    }

    @Override
    public List<CiteGestion> listarGestiones() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "G", CiteGestion.class);
    }

    @Override
    public List<CopiaArch> listarCopiasArch(long idDocumento) {
        return spHelper.ejecutarListado(SP_LIST, hijosDe(idDocumento), "H", CopiaArch.class);
    }

    @Override
    public List<Remitente> listarRemitentes(long idDocumento) {
        return spHelper.ejecutarListado(SP_LIST, hijosDe(idDocumento), "I", Remitente.class);
    }

    @Override
    public List<CopiaEncabezado> listarCopiasEncabezado(long idDocumento) {
        return spHelper.ejecutarListado(SP_LIST, hijosDe(idDocumento), "J", CopiaEncabezado.class);
    }

    // ══════════════════════ AUXILIARES ══════════════════════

    private static Map<String, Object> hijosDe(long idDocumento) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idDocumento", idDocumento);
        return filtro;
    }

    private static <T> T primero(List<T> lista) {
        return (lista == null || lista.isEmpty()) ? null : lista.get(0);
    }
}
