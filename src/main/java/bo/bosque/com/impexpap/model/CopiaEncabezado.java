package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

/**
 * Un destinatario extra del encabezado, el bloque "Copia a:"
 * ({@code tcr_copiaEncabezado}).
 *
 * <p>Se imprime arriba, junto al destinatario principal, con nombre y cargo.
 * No confundir con {@link CopiaArch}, que es el "cc/Arch" del pie.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CopiaEncabezado implements Serializable {

    private long idCopiaEncab;
    private long idDocumento;
    private int nroCite;
    private String copiaEnca;
    private String cargoCopia;
    private long audUsuario;
}
