package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;

/** Body de {@code POST /biometrico/reporte-detallado-rango-pdf} — un empleado, varios meses seguidos. */
@Getter
@Setter
public class ReporteBiometricoRangoRequest {
    private long codEmpleado;
    private int anioDesde;
    private int mesDesde;
    private int anioHasta;
    private int mesHasta;
}
