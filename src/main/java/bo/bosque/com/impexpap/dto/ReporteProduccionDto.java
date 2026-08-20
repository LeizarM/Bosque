package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Parametros de los reportes del modulo de produccion.
 * El reporte de un lote usa idLp; los de resumen usan el rango de fechas.
 */
@Getter
@Setter
public class ReporteProduccionDto {

    private int idLp;
    private Date fechaIni;
    private Date fechaFin;

}
