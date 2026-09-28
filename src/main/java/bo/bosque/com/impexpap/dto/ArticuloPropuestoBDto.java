package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_ArticuloProp ACCION 'B'</b>: los articulos de una
 * propuesta con el encabezado de la propuesta pegado (titulo y obs salen de
 * tpr_propuesta, por eso no pueden vivir en el model).
 *
 * <p>Nueve columnas, en el orden del SELECT. Los nombres coinciden con los alias.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPropuestoBDto implements Serializable {

    /** tpr_articuloPropuesto.idArticulo */
    private Long idArticulo;

    /** tpr_propuesta.titulo, varchar(200). Campo de display: viene del JOIN. */
    private String titulo;

    /** tpr_propuesta.obs, varchar(250). Campo de display: viene del JOIN. */
    private String obs;

    private Long idPropuesta;
    private String codArticulo;
    private Integer codigoFamilia;
    private String datoArticulo;
    private BigDecimal stock;
    private BigDecimal utm;
}
