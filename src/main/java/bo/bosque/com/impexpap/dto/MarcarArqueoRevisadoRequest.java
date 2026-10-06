// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\MarcarArqueoRevisadoRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Cuerpo de POST /tareas-rutinarias/verificar-cierre/marcar-arqueo-revisado. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MarcarArqueoRevisadoRequest {
    private long idAC;

    /**
     * La ocurrencia de la tarea de revisión desde la que se marca: es el
     * permiso. Sin ella, el endpoint se cae al botón cboFueRevisado.
     */
    private long idBitTarea;

    /**
     * 1 = darle el visto bueno, 0 = quitárselo porque fue un error (Marcelo,
     * 2026-10-05). Vale 1 si no viene: una versión de la app anterior no lo
     * manda y tiene que seguir marcando como siempre.
     */
    private int fueRevisado = 1;
}
