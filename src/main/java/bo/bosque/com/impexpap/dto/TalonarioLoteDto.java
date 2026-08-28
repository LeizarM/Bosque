package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.Talonario;

import java.io.Serializable;
import java.util.List;

import lombok.*;

/**
 * Alta masiva de talonarios. Cabecera del formulario + la lista generada.
 *
 * Reemplaza cargarListTalGenerads() / guardarListTalonarios() del
 * WizardTalonario del JSF. Nombres de campo aclarados respecto del legacy,
 * que usaba numeracionInicial para el bloque y numeracionFinal para la
 * CANTIDAD, lo cual se presta a confusion.
 *
 * Flujo: POST simular-lote devuelve talonarios[] con los duplicados marcados
 * y NO escribe nada; POST aplicar-lote los graba todo o nada.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TalonarioLoteDto implements Serializable {

    /** Cada talonario cubre 50 recibos. Es fijo en el legacy. */
    public static final int RECIBOS_POR_TALONARIO = 50;

    public static final int COSTO_INDIVIDUAL = 1;
    public static final int COSTO_TOTAL      = 2;

    private long codTipoRecibo;
    private long codEmpresa;

    /** Cuantos talonarios generar (era numeracionFinal en el legacy). */
    private int cantidad;

    /**
     * Bloque de folios desde donde arrancar, 1-based (era numeracionInicial).
     * El folio inicial sale de (bloqueInicial - 1) * 50 + 1.
     */
    private int bloqueInicial;

    /** Correlativo inicial del nroTalonario, se completa con ceros a 3 digitos. */
    private int correlativoInicial;

    /** COSTO_INDIVIDUAL: el costo es por talonario. COSTO_TOTAL: se divide entre cantidad. */
    private int tipoCosto;
    private double costo;

    /** Si es true el prefijo del nroTalonario es anio + sigla; si no, solo la sigla. */
    private boolean porGestion;
    private int anio;

    private String observacion;
    private long audUsuario;

    /**
     * Salida de simular-lote y entrada de aplicar-lote.
     * Los que vienen con observacion de duplicado no se graban.
     */
    private List<Talonario> talonarios;

    /** Duplicados detectados por la simulacion, para avisar antes de confirmar. */
    private List<String> duplicados;

}
