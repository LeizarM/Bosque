package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Filtros de los reportes de comisiones.
 * <p>
 * Los reportes por periodo usan mes y anio; el de comisiones por vendedor usa
 * el rango de fechas. Van juntos en un solo DTO porque el cliente siempre manda
 * el mismo objeto y solo completa lo que el reporte pide.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteComisionDto implements Serializable {

    private int  mes;
    private int  anio;
    private Date fechaDesde;
    private Date fechaHasta;
}
