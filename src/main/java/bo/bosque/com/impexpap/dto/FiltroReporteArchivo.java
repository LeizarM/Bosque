package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pedido de un reporte del archivo de documentos. Todo filtro nulo es "todos".
 * {@code codEmpresa} = 0 pide los documentos sin empresa (los OTROS del Excel).
 */
@Getter
@Setter
@NoArgsConstructor
public class FiltroReporteArchivo implements Serializable {

    /** POR_SUCURSAL, POR_TIPO o DETALLE. */
    private String reporte;
    private Long codSucursal;
    private Long codTipoDocumento;
    private Long codEmpresa;
    private Integer anioDesde;
    private Integer anioHasta;
}
