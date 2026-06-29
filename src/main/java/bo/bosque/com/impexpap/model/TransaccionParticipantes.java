package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionParticipantes implements Serializable {

    // ── campos de la tabla tpex_TransaccionParticipantes ──────────────────
    private long idParticipante;
    private long idTransaccion;
    private String tipoParticipante;  // EMPRESA / TERCERO
    private String nombre;            // IPX, MONRROY RODRIGO, NEMER...
    private double porcentaje;        // participación s/ montoConvertido
    private double montoUs;
    private double montoBs;
    private double itfUs;             // ITF propio del participante
    private double itfBs;
    private String observaciones;
    private long audUsuario;
    private Date audFecha;

    // ── campos de join (solo lectura, devueltos por el SP) ────────────────
    private String estadoTransaccion;
    private Date fechaTransaccion;
    private double montoOrigen;
    private double montoConvertido;

    // ── campos de cuadre (solo en ACCION "V" del SP de listado) ───────────
    private int cantidadParticipantes;
    private double totalPorcentaje;
    private double totalMontoUs;
    private double totalMontoBs;
    private double totalItfUs;
    private double totalItfBs;
    private double diferenciaUs;
    private String estadoCuadre;      // "CUADRADO" / "DESCUADRADO"
}
