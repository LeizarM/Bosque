package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo de {@code POST /cheque/verificacion/listar}: la lista principal. {@code fechaBanco} es el dia de la
 * verificacion; ausente = todas (la pantalla manda hoy por defecto, como el legacy: el servidor no pone ninguna).
 */
@Getter
@Setter
@NoArgsConstructor
public class VerificacionFiltroDto implements Serializable {

    @FechaDiaJson
    private LocalDate fechaBanco;

    /** Desde 1. */
    private Integer pagina;

    /** 20 por defecto, 200 como maximo. */
    private Integer tamanio;
}
