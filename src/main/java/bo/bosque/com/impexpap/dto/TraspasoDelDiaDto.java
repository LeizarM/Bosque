// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\TraspasoDelDiaDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Una fila del listado del día de la tarea 289, "Verificar Traspaso de
 * Efectivo Entre Sistemas" ({@code p_list_tac_TraspasoMovCaja @ACCION='A'}).
 *
 * <p><b>No se reusa {@link bo.bosque.com.impexpap.model.TraspasoMovCaja}</b> a
 * propósito. Ese modelo es el que viaja al ABM por
 * {@code SpHelper.ejecutarAbm}, que arma la llamada desde la metadata del
 * procedimiento: cualquier campo que no sea un parámetro declarado del SP
 * rompe la llamada. Y esta vista tiene uno que no lo es —
 * {@code soloEnBosque}— porque no sale de la tabla sino de la comparación
 * contra SAP.
 *
 * <p>{@code idTrasp} llega en 0 cuando el traspaso todavía no se guardó: en
 * este flujo la fila de {@code tac_traspasoMovCaja} recién se escribe cuando
 * alguien verifica. Por eso el listado del día es una mezcla de lo que dice
 * SAP y lo que ya está guardado, y no un simple SELECT de la tabla.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TraspasoDelDiaDto {

    /** 0 = todavía no existe fila guardada para este traspaso. */
    private long idTrasp;

    private String bd;
    private Date fecha;
    private String account;
    private String contraAct;
    private String acctName;
    private String tipoTransaccion;
    private Double dolares;
    private Double bs;

    /** 1 = cuadró; 0 = revisado y no cuadra (entonces {@code obs} tiene el motivo). */
    private Integer fueVerificado;

    /** Por qué no cuadró. Vacía mientras nadie haya marcado la fila. */
    private String obs;

    /** Ocurrencia de la tarea que registró la verificación. */
    private Long idBitTarRuti;

    /**
     * {@code true} = la fila está guardada en Bosque pero SAP ya no la
     * devuelve.
     *
     * <p>No se oculta a propósito. Que un traspaso verificado desaparezca del
     * sistema de origen es exactamente el descuadre que esta tarea existe
     * para encontrar; esconderlo sería lo peor que puede hacer esta pantalla.
     */
    private Boolean soloEnBosque;
}
