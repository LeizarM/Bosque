package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una fila de {@code p_list_tarc_Contenido} 'R': un tipo de documento dentro de un archivo, con sus cajas
 * como tramos ("1-3, 7"). Alimenta RptTarcPorSucursal y RptTarcPorTipo; los getters sin campo son para el jrxml.
 */
@Getter
@Setter
@NoArgsConstructor
public class ResumenArchivoRptDto implements Serializable {

    private Long codArchivo;
    private String nombreArchivo;
    private String nombreSucursal;
    private String ciudadSucursal;
    private Long codTipoDocumento;
    private String nombreTipoDocumento;
    private Integer cajas;
    private Integer lineas;
    private Integer anioDesde;
    private Integer anioHasta;
    private String cajasNumeros;
    /** Totales del archivo entero (se repiten en cada fila): las cajas distintas no se pueden sumar por tipo. */
    private Integer cajasArchivo;
    private Integer lineasArchivo;

    public String getSucursalTexto() {
        return textoSucursal(nombreSucursal, ciudadSucursal);
    }

    public String getGestionTexto() {
        return textoGestion(anioDesde, anioHasta);
    }

    /** "120 documentos en 15 cajas", para el encabezado del archivo. */
    public String getTotalArchivoTexto() {
        return cuenta(lineasArchivo == null ? 0 : lineasArchivo, "documento", "documentos") + " en "
                + cuenta(cajasArchivo == null ? 0 : cajasArchivo, "caja", "cajas");
    }

    /** "1.079 documentos" (punto de miles, como se escribe en Bolivia) o "1 documento". */
    public static String cuenta(int n, String singular, String plural) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", s).format(n) + " " + (n == 1 ? singular : plural);
    }

    /** "Sucursal 15 Achocalla · LA PAZ", o "Sin sucursal" explicito. */
    static String textoSucursal(String nombre, String ciudad) {
        if (nombre == null || nombre.trim().isEmpty()) return "Sin sucursal";
        return ciudad == null || ciudad.trim().isEmpty() ? nombre.trim() : nombre.trim() + " · " + ciudad.trim();
    }

    static String textoGestion(Integer desde, Integer hasta) {
        if (desde == null) return "Sin gestión";
        return hasta == null || hasta.equals(desde) ? String.valueOf(desde) : desde + " - " + hasta;
    }
}
