package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Forma de las ramas <b>D y E</b> de {@code p_list_producto}.
 * <ul>
 *   <li>D: familias activas de un grupo de familia SAP, con proveedor y grupo.</li>
 *   <li>E: todas las familias activas con proveedor asignado. Esa rama NO devuelve
 *       {@code grpFam}, asi que ese campo queda en null.</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductoProveedorDto {

    private Integer codigoFamilia;

    private String proveedorExtSap;

    /** Solo lo devuelve la rama D; en la rama E llega null. */
    private String grpFam;
}
