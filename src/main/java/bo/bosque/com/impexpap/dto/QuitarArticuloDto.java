package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/** Cuerpo de {@code /price/armado/quitarArticulo}. Los dos campos son obligatorios. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuitarArticuloDto implements Serializable {

    private Long idPropuesta;

    /** tpr_articuloPropuesto.idArticulo. Tiene que pertenecer a {@link #idPropuesta}. */
    private Long idArticulo;
}
