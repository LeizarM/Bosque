// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\LlegadaCajaFuerteDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Una fila del formulario "Reportar dinero para caja fuerte" (idATR=4). */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LlegadaCajaFuerteDto {
    private String cliente;
    /** "BS" | "USD". */
    private String moneda;
    private Double importe;
    /** "CHEQUE" | "EFECTIVO" — obligatorio, el SP rechaza el lote entero si falta en cualquier fila. */
    private String tipo;
    private String destino;
    private String obs;
}
