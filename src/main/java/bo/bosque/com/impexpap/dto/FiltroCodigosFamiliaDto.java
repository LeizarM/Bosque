package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Filtro por una lista de codigos de familia separados por coma.
 *
 * <p>Reemplaza al campo {@code codCad} que antes viajaba dentro del model
 * {@code ArticuloPropuesto}: ese campo no es una columna de
 * {@code tpr_articuloPropuesto}, y {@code SpHelper.ejecutarAbm} manda cada campo
 * del POJO como parametro del procedimiento, asi que un campo de mas rompe el EXEC.
 *
 * <p>El procedimiento concatena esta cadena dentro de un {@code sp_executesql}, por
 * lo que el DAO valida que solo traiga digitos, comas y espacios antes de mandarla.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FiltroCodigosFamiliaDto implements Serializable {

    /** Codigos separados por coma, con coma final. Ejemplo: {@code "12,13,14,"}. */
    private String codCad;
}
