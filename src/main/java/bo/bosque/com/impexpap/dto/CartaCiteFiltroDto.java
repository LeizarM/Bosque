package bo.bosque.com.impexpap.dto;

import java.util.Date;

import lombok.*;

/**
 * Cuerpo de las consultas del módulo Cartas CITE.
 *
 * <p>Un solo DTO para todos los endpoints de lectura y para los dos de PDF:
 * son todos POST con un body chico y campos que se repiten
 * ({@code idDocumento}, {@code idTipoDoc}, {@code codEmpresa}), y tener un DTO
 * por endpoint sería una decena de clases de tres campos.
 *
 * <p>Los que no aplican al endpoint llamado simplemente vienen en 0 o null.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CartaCiteFiltroDto {

    private long idDocumento;
    private long idTipoDoc;
    private long codEmpresa;
    private long codUsuario;
    private long codEmpleado;

    // ── filtros del listado ───────────────────────────────────────────────
    private Date fechaDesde;
    private Date fechaHasta;
    private String buscar;
    private int pagina;
    private int tamanoPagina;

    // ── PDF ───────────────────────────────────────────────────────────────
    private int nroCite;
    /** "SI" o "NO": sólo la carta (tipo 1) pregunta si va con membrete. */
    private String logo;
    /** Reporte mensual: 0 = toda la gestión. */
    private int mes;
    private int anio;

    /** Anulación: por qué se anula el documento. */
    private String motivo;

    // ── auditoría ─────────────────────────────────────────────────────────
    private long audUsuario;
}
