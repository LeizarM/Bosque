package bo.bosque.com.impexpap.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DocsVencidosDTO {

    private int fila;
    private int codEmpleado;
    private String nombreCompleto;
    private String ciNumero;
    private String ciFechaVencimiento;  // Viene como "DD/MM/YYYY" o "SIN FECHA"
    private String licenciaVencimiento;    // Viene como "DD/MM/YYYY" o "SIN LICENCIA REGISTRADA"
    private String estadoDocumentos;
    //private String estadoCI;              // "VENCIDO" | "PRÓXIMO A VENCER" | "VIGENTE"
    //private String estadoLicencia;        // "NO REGISTRADA" | "VENCIDO" | "PRÓXIMO A VENCER" | "VIGENTE"
}