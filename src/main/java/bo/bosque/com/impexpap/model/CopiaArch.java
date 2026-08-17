package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

/**
 * Una línea del bloque "cc/Arch" que va al pie del documento
 * ({@code tcr_copiaArch}).
 *
 * <p>La columna en BD es {@code varchar(25)}: son siglas de área o nombres
 * cortos, no destinatarios completos. Para eso está {@link CopiaEncabezado}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CopiaArch implements Serializable {

    private long idCopiaArch;
    private long idDocumento;
    private int nroCite;
    private String copiaArch;
    private long audUsuario;
}
