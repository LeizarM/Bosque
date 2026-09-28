// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\ResumenGeneracionDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Una fila del resumen de generación
 * ({@code p_list_tac_BitTareaRuti @ACCION='G'}): cuántos candidatos cayeron en
 * cada motivo en una corrida del generador. Lo escribe el propio generador
 * desde el archivo SQL 54.
 *
 * <p>El motivo "Paso los filtros (frecuencia o ya existia)" junta dos cosas a
 * propósito — ver el encabezado del archivo 54. Para separarlas para una
 * persona concreta está la ACCION 'P' ({@link DiagnosticoGeneracionDto}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ResumenGeneracionDto {

    /** Cuándo corrió. Llega como DATETIME: el SP castea el DATETIME2 de la tabla. */
    private Date corrida;

    private String motivo;
    private Integer cantidad;
}
