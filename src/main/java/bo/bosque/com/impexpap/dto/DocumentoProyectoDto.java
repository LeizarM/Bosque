package bo.bosque.com.impexpap.dto;

import lombok.*;
import java.io.Serializable;

/**
 * Documento abierto de SAP (factura proveedor / orden de compra) filtrado por proyecto.
 * Shape del resultset de p_list_tpex_SolicitudPago ACCION "C" (OPENQUERY a SRV_2022).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoProyectoDto implements Serializable {

    private int codEmpresa;
    private String empresa;
    private String descripcion;   // tipo de documento en SAP
    private int docNum;           // número de documento SAP (facturaProvSap)
    private String moneda;
    private String project;       // código del proyecto SAP
    private Double montoTotal;    // wrapper: SAP puede devolver NULL (fallback legacy del SP)
}
