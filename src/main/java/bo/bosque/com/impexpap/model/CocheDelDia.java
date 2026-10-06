// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\CocheDelDia.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de la lista "coches del día" de una ocurrencia puntual de tarea
 * rutinaria — proyección de {@code p_coches_listarDelDia} (JOIN
 * tac_cocheLlegadas + tac_coche), no una tabla real.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CocheDelDia implements Serializable {
    private long idCo;
    private Long idTarRuti;
    private Long idCoche;
    private Long idBitTarRuti;
    private String descripcion;
    private Date fecha;
    private Integer llego;
    private String obs;
    private String marca;
    private String clase;
    private String placa;
    private String color;
    private Integer anio;
}
