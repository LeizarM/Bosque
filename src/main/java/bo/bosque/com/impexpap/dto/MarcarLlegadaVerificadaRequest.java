// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\MarcarLlegadaVerificadaRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Cuerpo de POST /tareas-rutinarias/verificar-cierre/marcar-llegada-verificada. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MarcarLlegadaVerificadaRequest {
    private long idRp;
    /** 1=verificado, 0=no. */
    private int fueVerificado;

    /**
     * La ocurrencia de la tarea de revisión desde la que se marca: es el
     * permiso. Sin ella, el endpoint se cae al botón plCajaFuerte.
     */
    private long idBitTarea;
}
