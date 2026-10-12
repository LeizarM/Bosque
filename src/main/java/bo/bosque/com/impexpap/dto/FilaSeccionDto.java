// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\FilaSeccionDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Una fila de una sección con fuente ({@code p_list_tac_FirmaCierre 'G'},
 * archivo SQL 86): lo que devuelve la fuente con su marca.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FilaSeccionDto {
    private String clave;
    private Long codSucursal;
    private String nombreSucursal;
    private Date fecha;
    private String titulo;
    private String detalle;
    private Double monto;
    private Integer hallazgo;
    private String motivo;
    private Long autor;
    private Integer revisada;
    private Long revisadaPor;
    private String nombreRevisadaPor;
    /** Pantalla del módulo donde se abre la fila, si la fuente la da. */
    private String ruta;
    /** 1 si la hizo quien mira. */
    private Integer esPropia;
}
