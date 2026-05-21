package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Asientos implements Serializable {

    // ── campos de la tabla tpex_Asientos ──────────────────────────────────
    private long idAsiento;
    private long idTransaccion;
    private int numero;
    private String tipoAsiento;      // PR / PE
    private int codBancoRef;
    private String cuentaDebe;
    private String cuentaHaber;
    private String descripcion;
    private double debitoUs;
    private double creditoUs;
    private double debitoBs;
    private double creditoBs;
    private double tcAplicado;
    private int audUsuario;
    private Date audFecha;

    // ── campos de join (solo lectura, devueltos por el SP) ────────────────
    private String banco;
    private String estadoTransaccion;
    private Date fechaTransaccion;

    // ── campos de cuadre (solo en ACCION "V" de p_list_tpex_Asientos) ────
    private double totalDebitoUs;
    private double totalCreditoUs;
    private double totalDebitoBs;
    private double totalCreditoBs;
    private double diferenciaBs;
    private String estadoCuadre;     // "CUADRADO" / "DESCUADRADO"
}
