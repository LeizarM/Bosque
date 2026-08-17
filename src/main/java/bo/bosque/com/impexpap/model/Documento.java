package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.*;

/**
 * Un documento CITE de la tabla {@code tcr_documento}: cartas, memorandos,
 * certificados de trabajo, comunicaciones internas e informes de control
 * interno, todos numerados con un correlativo por (tipo, empresa, gestión).
 *
 * <p>Migrado del módulo JSF {@code web/Bosque/tcrDocumento} de Bosque v2.
 * Persiste vía {@code p_abm_tcr_Documento} / {@code p_list_tcr_Documento};
 * los SPs viejos ({@code p_abm_Documento}, {@code p_list_registroDoc}) siguen
 * intactos porque el JSF los usa y los reportes Jasper también.
 *
 * <p><b>El correlativo lo asigna el SP, no este objeto.</b> El {@code nroCite}
 * que viaja en un alta es sólo la previsualización que vio el usuario; el
 * número definitivo se calcula dentro de la transacción del INSERT. Lo que
 * devuelve el listado sí es el número real.
 *
 * <p><b>Tipos</b> (tabla {@code tcr_tipoDocumento}), porque el formulario
 * cambia de forma según cuál sea:
 * <pre>
 *   1  CARTA                  ciudad + dirigido + cargo + referencia
 *   2  MEMORANDO              destinatario de planilla + asunto
 *   6  CERTF. TRABAJO         sin destinatario, área fija G.A.
 *   7  COM. INTERNA           igual que memorando
 *   8  INF. CONTROL INTERNO   agrega vía + cargo vía + asunto
 *   9  COM. CI                el destinatario se graba como "DE:"
 * </pre>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Documento implements Serializable {

    // ── columnas de tcr_documento ─────────────────────────────────────────
    private long idDocumento;
    private long idTipoDoc;
    private long idGestion;
    private long codEmpresa;
    private long codUsuario;
    private long codEmpleado;
    private String empleadoDe;
    private String cargoDe;
    private String ciudad;
    private String area;
    private int nroCite;
    private Date fechaDoc;
    private String dirigido;
    private String cargoDirigido;
    private String referencia;
    private String via;
    private String cargoVia;
    private String asunto;
    private String cuerpo;

    /**
     * Estado de la fila tal como lo graba el sistema, siempre 'V'.
     *
     * <p><b>No indica anulación.</b> La anulación vive en
     * {@code tcr_documentoAnulado} y llega en {@link #esAnulado}. Se hizo así
     * para no moverle la cuenta al módulo JSF, que numera contando las filas
     * con {@code estado='V'}: marcar la anulación acá le hacía reutilizar
     * números ya emitidos.
     */
    private String estado;

    /**
     * Por qué se anuló. Sólo viaja en la ACCION 'D' y se guarda en
     * {@code tcr_documentoAnulado}; en el resto de las acciones va en null y el
     * SP lo ignora.
     */
    private String motivo;

    private long audUsuario;

    // ── campos de join (sólo lectura, los llena el SP de listado) ─────────
    /** Nombre del tipo de documento — "CARTA", "MEMORANDO"… */
    private String tipo;
    /** Año de la gestión a la que pertenece el correlativo. */
    private int gestion;
    /** Razón social de la empresa emisora. */
    private String empresa;
    /** Correlativo ya formateado para mostrar: {@code "G.A./007/2025"}. */
    private String cite;
    /** "SI" cuando ya se generó el PDF al menos una vez. */
    private String exportado;
    /** Nombre completo de quien redactó. */
    private String redactadoPor;
    /** 1 si el usuario que consulta es el autor del documento. */
    private int esAutor;

    /**
     * 1 si el documento fue anulado. Sólo lo devuelve la ACCION 'R': el listado
     * ya los excluye, así que ahí siempre sería 0.
     */
    private int esAnulado;

    private long idRegDoc;
    /** Total de filas que matchean el filtro, para paginar en el cliente. */
    private int totalRegistros;

    // ── hijos, que viajan anidados en el guardado completo ────────────────
    private List<CopiaArch> copiasArchivo;
    private List<CopiaEncabezado> destinatarios;
    private List<Remitente> remitentes;

    /**
     * Ids de hijos que el usuario quitó en la pantalla de edición.
     *
     * <p>Van por separado y no se infieren comparando contra la BD: el
     * formulario es la única fuente que sabe qué se borró, y recalcularlo
     * en el servidor implicaría leer los hijos actuales en cada guardado.
     * Mismo criterio que {@code SolicitudProveedor.detallesAEliminar} en tpex.
     */
    private List<Long> copiasArchivoAEliminar;
    private List<Long> destinatariosAEliminar;
    private List<Long> remitentesAEliminar;

    // ── filtros de listado (no son columnas) ──────────────────────────────
    private Date fechaDesde;
    private Date fechaHasta;
    private String buscar;
    private int pagina;
    private int tamanoPagina;
}
