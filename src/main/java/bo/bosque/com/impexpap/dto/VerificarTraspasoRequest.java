// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\VerificarTraspasoRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Cuerpo de POST /tareas-rutinarias/traspaso-entre-sistemas/verificar.
 *
 * <p>Trae la fila entera y no solo el id porque en este flujo la fila puede
 * no existir todavía: {@code tac_traspasoMovCaja} se escribe recién cuando
 * alguien verifica, así que la primera verificación de un traspaso es un
 * INSERT y necesita todos los datos que vinieron de SAP. Con
 * {@code idTrasp > 0} el SP hace UPDATE.
 *
 * <p>{@code obs} es obligatoria cuando {@code fueVerificado} es 0. No se
 * valida aquí: lo valida el SP (error 23), que es el único punto por el que
 * pasan todos los caminos de escritura.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class VerificarTraspasoRequest {

    /** Ocurrencia de la tarea 289 contra la que se registra. Obligatoria. */
    private long idBitTarRuti;

    /** 0 = el traspaso todavía no está guardado; el SP lo inserta. */
    private long idTrasp;

    private String bd;
    private Date fecha;
    private String account;
    private String contraAct;
    private String acctName;
    private String tipoTransaccion;
    private Double dolares;
    private Double bs;

    /** 1 = cuadró; 0 = no cuadra. */
    private Integer fueVerificado;

    /** Por qué no cuadró. Obligatoria si {@code fueVerificado} es 0. */
    private String obs;
}
