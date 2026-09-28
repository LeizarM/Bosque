package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Par (idProveedorSap, proveedorExtSap) del catalogo tpr_proveedorExtSap, tal como
 * lo devuelven las ramas <b>G</b> (ordenada por nombre) y <b>R</b> (sin ordenar) de
 * {@code p_list_producto}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductoProveedorSapDto {

    private Long idProveedorSap;

    private String proveedorExtSap;
}
