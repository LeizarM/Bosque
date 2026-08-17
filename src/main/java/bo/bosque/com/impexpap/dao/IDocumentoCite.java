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

import java.util.Date;
import java.util.List;

/**
 * Acceso a datos del módulo Cartas CITE. Todo pasa por
 * {@code p_abm_tcr_*} / {@code p_list_tcr_Documento}: en este proyecto no hay
 * consultas SQL armadas en Java.
 */
public interface IDocumentoCite {

    // ── escritura ─────────────────────────────────────────────────────────

    /**
     * @param acc "I" alta, "U" modificación, "D" anulación lógica,
     *            "X" marcar exportado, "G" rollover de gestión
     */
    RespuestaSp registrarDocumento(Documento mb, String acc);

    RespuestaSp registrarCopiaArch(CopiaArch mb, String acc);

    RespuestaSp registrarCopiaEncabezado(CopiaEncabezado mb, String acc);

    RespuestaSp registrarRemitente(Remitente mb, String acc);

    // ── lectura ───────────────────────────────────────────────────────────

    /**
     * Listado paginado. Cada fila trae {@code totalRegistros} con el total que
     * matchea el filtro, para que el cliente pueda paginar sin una segunda
     * llamada de conteo.
     *
     * @param idTipoDoc  0 = todos los tipos
     * @param codEmpresa 0 = todas las empresas
     * @param codUsuario usuario que consulta; define {@code esAutor} en cada fila
     */
    List<Documento> listarDocumentos(Date fechaDesde, Date fechaHasta, long idTipoDoc,
                                     long codEmpresa, long codUsuario, String buscar,
                                     int pagina, int tamanoPagina);

    /** Cabecera de un documento. {@code null} si no existe. */
    Documento obtenerDocumento(long idDocumento);

    /**
     * Gestión activa y el correlativo que tocaría para ese tipo y empresa.
     *
     * <p>Es orientativo: el número real lo asigna el ABM dentro de la
     * transacción del alta. Entre que se muestra y se guarda, otro usuario
     * puede haberse llevado ese número.
     */
    CiteGestion obtenerSiguienteCite(long idTipoDoc, long codEmpresa);

    List<CiteTipoDocumento> listarTiposDocumento();

    List<CiteArea> listarAreas(long codEmpresa);

    List<CiteEmpleado> listarEmpleados();

    /** Un empleado con su cargo vigente. {@code null} si no existe o no está activo. */
    CiteEmpleado obtenerEmpleado(long codEmpleado);

    /** Nombre y cargo del usuario logueado, para precargar la firma. */
    CiteEmpleado obtenerFirmaUsuario(long codUsuario);

    List<CiteGestion> listarGestiones();

    List<CopiaArch> listarCopiasArch(long idDocumento);

    List<Remitente> listarRemitentes(long idDocumento);

    List<CopiaEncabezado> listarCopiasEncabezado(long idDocumento);
}
