package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

/**
 * Quien firma el documento ({@code tcr_remitente}). Máximo dos por documento:
 * el formato impreso tiene lugar para dos firmas y el SP rechaza la tercera.
 *
 * <p>El primero se precarga con el nombre y cargo del usuario que redacta.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Remitente implements Serializable {

    private long idRemitente;
    private long idDocumento;
    private int nroCite;
    private String remitente;
    private String cargoRemitente;
    private long audUsuario;
}
