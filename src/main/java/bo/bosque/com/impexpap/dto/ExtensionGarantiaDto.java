package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Cuerpo de {@code /garantias/extension}: registra la accion EXT y mueve la fecha de
 * expiracion de la garantia (el dialogo "Extension de Garantia" del legacy).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionGarantiaDto implements Serializable {

    private Long codGarantia;

    /** Fecha del evento EXT. */
    private Date fecha;

    private String observacion;

    /** Nueva fecha de expiracion; tiene que ser posterior a la actual. */
    private Date fechaExpiracion;
}
