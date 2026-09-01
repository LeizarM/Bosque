package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;

/** Body de {@code POST /biometrico/reporte-mensual}. */
@Getter
@Setter
public class ReporteBiometricoRequest {
    private long codEmpleado;
    private int anio;
    private int mes;
}
