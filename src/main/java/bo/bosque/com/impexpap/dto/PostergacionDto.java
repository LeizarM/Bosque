package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.ChPostergacion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Una postergacion en la lista del detalle del cheque ({@code p_list_ChPostergacion} 'L' por {@code codCheque}).
 * Hereda las columnas de la tabla y agrega lo que la pantalla necesita.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class PostergacionDto extends ChPostergacion {

    /** Numero de fila, 1..n, por codigo de postergacion (el orden en que se registraron). Lo pone el servicio. */
    private Integer fila;

    /**
     * Si la postergacion ya tiene su documento PDF en la carpeta del servidor. {@code null} = no se pudo saber (la carpeta
     * no esta configurada o no esta disponible): el listado nunca falla por eso.
     */
    private Boolean tienePdf;
}
