package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Par (idGrpFamiliaSap, grpFam) que devuelven las ramas <b>C, H, Q y T</b> de
 * {@code p_list_producto}. Las cuatro tienen exactamente la misma forma de
 * resultset y solo cambian el filtro y el orden:
 * <ul>
 *   <li>C: grupos de familia que tienen productos activos (opcionalmente de un codigo de familia).</li>
 *   <li>H: catalogo completo de tpr_grupoFamiliaSap ordenado por grpFam.</li>
 *   <li>Q: catalogo completo sin ordenar.</li>
 *   <li>T: grupos con productos activos y idGrpFamiliaSap mayor a 2 (pagina web).</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductoGrupoFamiliaDto {

    private Long idGrpFamiliaSap;

    private String grpFam;
}
