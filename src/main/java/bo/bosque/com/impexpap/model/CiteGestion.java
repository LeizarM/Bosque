package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

/**
 * Gestión (año) del correlativo, tabla {@code tcr_gestion}. Sólo una está
 * activa a la vez y es la que numera los documentos nuevos.
 *
 * <p>Sirve para dos respuestas del SP de listado: el catálogo de gestiones
 * (ACCION G) y la previsualización del siguiente número (ACCION A), que
 * devuelve la gestión activa junto con el {@code nroCite} que tocaría.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CiteGestion implements Serializable {

    private long idGestion;
    private int gestion;
    private String activo;

    /** Sólo en la previsualización (ACCION A); en el catálogo viene 0. */
    private int nroCite;
}
