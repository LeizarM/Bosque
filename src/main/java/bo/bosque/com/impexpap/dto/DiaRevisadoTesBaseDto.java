// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\DiaRevisadoTesBaseDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Qué día revisa una ocurrencia de "Verificar Traspaso de Efectivo Entre
 * Sistemas" ({@code p_list_TesBase @ACCION='H'}, archivo SQL 58).
 *
 * <p>Es el día hábil anterior a la fecha de la ocurrencia: el lunes revisa el
 * sábado y el día después de un feriado revisa el anterior al feriado. Los
 * feriados son los de la sucursal de quien tiene la tarea.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DiaRevisadoTesBaseDto {

    /** La fecha de la ocurrencia. */
    private Date fechaTarea;

    /** El día hábil anterior: lo que Cobranza registró ese día es lo que se revisa. */
    private Date fechaRevisada;

    /** La sucursal con la que se eligieron los feriados. Nula si no se pudo resolver. */
    private Long codSucursal;

    /** Si entre los dos días hubo un feriado, su motivo (el más reciente). */
    private String feriado;
}
