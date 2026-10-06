package bo.bosque.com.impexpap.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import bo.bosque.com.impexpap.model.ChCheque;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Un cheque con sus datos de JOIN: fila de {@code p_list_Cheque} ramas A, B y C.
 *
 * <p>Hereda las columnas de {@link ChCheque} y agrega lo que sale de los JOIN. Esas ramas devuelven
 * columnas SIN alias y con nombres repetidos ({@code descripcion} tres veces), asi que se leen por
 * posicion (ver {@code SpHelper.ejecutarListadoPorPosicion} y {@code ChChequeDAO.MAPA_FILA}), igual
 * que el legacy:
 * <pre>
 *    1 codCheque        2 fechaRecepcion   3 nrocheque       4 codCliente     5 datoCliente
 *    6 aOrdenDe         7 fechaCheque      8 fechaCobrar     9 monto         10 moneda
 *   11 descMoneda      12 tipo            13 descTipo       14 estado        15 descEstado
 *   16 codBanco        17 nombreBanco     18 codEmpleado    19 datoEmpleado  20 reciboManual
 *   21 observacion     22 codSucursal     23 nroRecibo      24 nroTalonario  25 codEmpresa
 *   26 datoEmpresa     27 audUsuario      28 audFecha
 * </pre>
 *
 * <p><b>{@code descTipo} puede venir {@code null}</b>: 8570 de los 9719 cheques de
 * {@code BOSQUE2PRUEBA} tienen {@code tipo} guardado como los bytes crudos de "PAG" dentro de una
 * columna {@code nvarchar}, y el {@code LEFT JOIN} contra {@code v_tipos} no los encuentra. El
 * legacy muestra la celda vacia; aqui es lo mismo (decision 2: paridad, no se corrige al leer).
 * {@code codEmpleado} es 0 si lo entrego el cliente y entonces {@code datoEmpleado} dice
 * {@code " - Entregado por el Cliente -"}.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class ChequeFilaDto extends ChCheque {

    /** Dia en que se registro (fecha de la accion REC). */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaRecepcion;

    /** Nombre del cliente en {@code text_SocioNegocio}. */
    private String datoCliente;

    /** Descripcion de la moneda (v_tipos 24): "Bs" / "$us". */
    private String descMoneda;

    /** Descripcion del tipo (v_tipos 21): "PAGO" / "RESPALDO". Puede ser null: ver el javadoc. */
    private String descTipo;

    /** Descripcion del estado del cheque (v_tipos 23): "PENDIENTE" / "CERRADO". */
    private String descEstado;

    /** Nombre del banco. */
    private String nombreBanco;

    /** Nombre de quien lo entrego, o {@code " - Entregado por el Cliente -"}. */
    private String datoEmpleado;

    /** Observacion de la accion REC (la que se escribe al registrar). */
    private String observacion;

    /** Nombre de la empresa. */
    private String datoEmpresa;

    /** Numero de fila, 1..n. Lo pone el DAO. */
    private Integer fila;
}
