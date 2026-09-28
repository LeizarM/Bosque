package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Forma de las ramas <b>J y M</b> de {@code p_list_producto}: comparativa de
 * articulos entre el SAP (vista {@code v_sap_ArticulosFamProv}) y Bosque
 * ({@code tpr_articulo}).
 *
 * <p>La rama J devuelve la union de ambos origenes marcados con {@code src};
 * la rama M devuelve, con EXCEPT, los que estan en el SAP y no en Bosque.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductoArticuloSapDto {

    private Integer codigoFamilia;

    private String codArticulo;

    private String datoArticulo;

    private String grupoFamilia;

    /** En el origen SAP viene en mayusculas por el UPPER() del SP. */
    private String proveedor;

    /** Origen del registro: 1 = SAP, 2 = Bosque. */
    private Integer src;
}
