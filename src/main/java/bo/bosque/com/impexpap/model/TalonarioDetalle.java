package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Un evento en la vida de un talonario. Tabla tmto_talonarioDetalle.
 *
 * NO es un detalle hijo: es un LOG DE EVENTOS append-only. Cada fila es una
 * transicion de estado, y el estado del talonario se deriva contandolas.
 * Por eso no se editan codEstado ni las fechas: para corregir se borra el
 * ultimo evento y se vuelve a cargar.
 *
 * codEstado (v_tipos grupo 45):
 *   1 Adquirido -> lo inserta el alta del talonario, nunca por el ABM
 *   2 Entregado -> lleva destinatario, sucursal O empleado, exactamente uno
 *   3 Devuelto  -> sin destinatario
 *   4 Cerrado   -> sin destinatario, terminal
 *
 * El destinatario es polimorfico con centinela: si codSucursal <= 0 va por
 * codEmpleado. p_list_tmto_TalonarioDetalle ya lo devuelve resuelto en
 * tipoDestinatario / codDestinatario / datoDestinatario.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TalonarioDetalle implements Serializable {

    private long codDetalle;
    private long codTalonario;
    private long codEstado;
    /** Cuando se cargo. El SP la fuerza a GETDATE(), no se envia. */
    private Date fechaDetalle;
    /** Cuando ocurrio el hecho. La informa el usuario, es obligatoria. */
    private Date fechaEvento;
    private long codSucursal;
    private long codEmpleado;
    private String observacion;
    private long audUsuario;
    private Date audFecha;

    // ---- solo lectura, los llena p_list_tmto_TalonarioDetalle ----
    private String datoEstado;
    private String datoSucursal;
    private String datoEmpleado;
    private String nroTalonario;
    private String sigla;
    private String datoTalonario;
    private String datoFechaDetalle;
    private String datoFechaEvento;

    /** 'SUCURSAL', 'EMPLEADO' o vacio si el evento no lleva destinatario. */
    private String tipoDestinatario;
    private long codDestinatario;
    private String datoDestinatario;

}
