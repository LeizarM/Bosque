package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuales de las cuatro acciones del detalle estan habilitadas para un cheque: el resultado de
 * {@code p_list_Cheque} 'K', que el legacy evaluaba en pantalla y el servidor evalua tambien al
 * ejecutar cada accion.
 *
 * <p>{@code codigo} son los 4 caracteres crudos del SP, en este orden: fecha de cobro, devolver,
 * cerrar con verificacion, cerrar sin verificacion. Un {@code 1} habilita.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BotonesChequeDto implements Serializable {

    private boolean fechaCobro;
    private boolean devolver;
    private boolean cerrarConVerificacion;
    private boolean cerrarSinVerificacion;
    private String codigo;

    /** Interpreta los 4 caracteres de la rama K. Cualquier cosa que no sea un {@code 1} deshabilita. */
    public static BotonesChequeDto desde(String codigo) {
        String c = codigo == null ? "" : codigo.trim();
        return new BotonesChequeDto(
                c.length() > 0 && c.charAt(0) == '1',
                c.length() > 1 && c.charAt(1) == '1',
                c.length() > 2 && c.charAt(2) == '1',
                c.length() > 3 && c.charAt(3) == '1',
                c);
    }
}
