package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una fila de {@code p_list_tarc_Contenido} 'D': un documento con su caja, ubicacion y sucursal.
 * Alimenta RptTarcDetalle; los getters sin campo son para el jrxml.
 */
@Getter
@Setter
@NoArgsConstructor
public class DetalleArchivoRptDto implements Serializable {

    private Long codArchivo;
    private String nombreArchivo;
    private String nombreSucursal;
    private String ciudadSucursal;
    private Long codCaja;
    private Integer numeroCaja;
    private String estante;
    private String fila;
    private Long codContenido;
    private String nombreTipoDocumento;
    private String detalle;
    private String gestion;
    private Integer anioDesde;
    private Integer anioHasta;
    private String nombreEmpresa;
    private String observacion;

    public String getSucursalTexto() {
        return ResumenArchivoRptDto.textoSucursal(nombreSucursal, ciudadSucursal);
    }

    /** "Estante 1 · Fila 2", o "Sin ubicación": una celda vacia parece un error. */
    public String getUbicacionTexto() {
        StringBuilder t = new StringBuilder();
        if (estante != null && !estante.trim().isEmpty()) t.append("Estante ").append(estante.trim());
        if (fila != null && !fila.trim().isEmpty()) t.append(t.length() > 0 ? " · " : "").append("Fila ").append(fila.trim());
        return t.length() == 0 ? "Sin ubicación" : t.toString();
    }

    public String getEmpresaTexto() {
        return nombreEmpresa == null || nombreEmpresa.trim().isEmpty() ? "Sin empresa" : nombreEmpresa.trim();
    }

    public String getGestionTexto() {
        return gestion == null || gestion.trim().isEmpty() ? "Sin gestión" : gestion.trim();
    }
}
