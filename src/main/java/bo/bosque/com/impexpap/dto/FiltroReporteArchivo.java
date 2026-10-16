package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pedido de un reporte del archivo de documentos. Todo filtro nulo es "todos".
 * {@code codEmpresa} = 0 pide los documentos sin empresa (los OTROS del Excel); POR_CAJA solo mira {@code codCaja}.
 */
@Getter
@Setter
@NoArgsConstructor
public class FiltroReporteArchivo implements Serializable {

    /** POR_SUCURSAL, POR_TIPO, DETALLE o POR_CAJA. */
    private String reporte;
    private Long codSucursal;
    private Long codTipoDocumento;
    private Long codEmpresa;
    private Integer anioDesde;
    private Integer anioHasta;
    private Long codCaja;
}
