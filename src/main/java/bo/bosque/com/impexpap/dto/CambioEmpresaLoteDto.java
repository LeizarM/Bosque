package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.List;

import lombok.*;

/**
 * Cambio de empresa de uno o varios talonarios. Sirve para uno solo (lista de
 * un id) y para los tildados de la grilla: es el mismo endpoint.
 *
 * Va todo o nada. Los que ya estan en esa empresa pasan sin escribir nada.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CambioEmpresaLoteDto implements Serializable {

    /** Los talonarios a mover. */
    private List<Long> codTalonarios;

    /** Empresa destino, la misma para todos. */
    private long codEmpresa;

    private long audUsuario;

}
