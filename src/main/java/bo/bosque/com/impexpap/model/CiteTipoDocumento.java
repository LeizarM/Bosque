package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

/**
 * Catálogo {@code tcr_tipoDocumento}. Los ids no son correlativos (1, 2, 6, 7,
 * 8, 9 — el 3, 4 y 5 no existen) y el formulario decide qué campos mostrar en
 * función de este id, así que se leen de la BD y no se hardcodean.
 *
 * <p>Prefijo {@code Cite} porque el paquete ya tiene un {@code TipoDocumento}
 * de otro módulo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CiteTipoDocumento implements Serializable {

    private long idTipoDoc;
    private String tipo;
}
