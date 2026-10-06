package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Mapea 1:1 la tabla <b>tch_cheque</b> (modulo de cheques recibidos).
 *
 * <pre>
 *   codCheque     int          NOT NULL IDENTITY PK
 *   nrocheque     varchar(50)  NOT NULL
 *   codCliente    varchar(50)  NULL      CardCode de SAP, sin FK (SAP es otra base)
 *   aOrdenDe      varchar(100) NULL
 *   fechaCheque   date         NULL
 *   fechaCobrar   date         NULL
 *   monto         float        NULL
 *   moneda        varchar(10)  NULL      v_tipos grupo 24 (BS / SUS)
 *   tipo          nvarchar(100) NULL     v_tipos grupo 21
 *   estado        varchar(50)  NULL      v_tipos grupo 23: PEN / CER
 *   codBanco      int          NULL      tch_banco (sin FK)
 *   codEmpleado   int          NULL      0 = lo dejo el cliente (el legacy escribe 0, nunca NULL)
 *   reciboManual  varchar(30)  NULL      "0" = sin recibo manual
 *   codSucursal   bigint       NULL
 *   nroRecibo     bigint       NULL      lo calcula p_abm_Cheque 'I': maximo de la sucursal + 1
 *   nroTalonario  varchar(20)  NULL      "0" = sin talonario
 *   codEmpresa    int          NULL
 *   audUsuario    int          NULL
 *   audFecha      datetimeoffset NULL
 * </pre>
 *
 * <h3>Dos detalles que no son obvios</h3>
 * <ul>
 *   <li><b>{@code audFecha} es {@code String}.</b> jTDS 1.3.1 negocia TDS 8.0 y SQL Server le manda
 *       los tipos de 2008 ({@code date}, {@code datetimeoffset}) como texto. {@code date} se
 *       convierte bien con {@code getTimestamp}; {@code datetimeoffset}
 *       ({@code "2026-08-31 14:54:51.5670000 +00:00"}) <b>no</b>: {@code getTimestamp} falla, y con un
 *       {@code Date} aqui reventaria cualquier lectura que incluya la columna. Medido contra
 *       {@code BOSQUE2PRUEBA} con {@code ProbarJtdsFechas.java}. Nadie la escribe: la sella el
 *       procedimiento.</li>
 *   <li><b>{@code aOrdenDe} tiene accesores escritos a mano.</b> Con Lombok el getter seria
 *       {@code getAOrdenDe()}, que Jackson serializa como {@code aordenDe} (baja todas las
 *       mayusculas iniciales). {@code getaOrdenDe()} da la propiedad {@code aOrdenDe} tanto para
 *       Jackson como para {@code BeanPropertyRowMapper}, igual que el nombre de la columna.</li>
 * </ul>
 *
 * <p>Todos los tipos son wrappers: {@code BeanPropertyRowMapper} revienta al escribir un NULL
 * sobre un primitivo. Los datos de JOIN y de pantalla van en DTO aparte
 * ({@link bo.bosque.com.impexpap.dto.ChequeFilaDto}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChCheque implements Serializable {

    /** int IDENTITY. 0 o null = alta. */
    private Integer codCheque;

    /** varchar(50) NOT NULL. */
    private String nrocheque;

    /** varchar(50). CardCode del cliente en SAP. */
    private String codCliente;

    /** varchar(100). A la orden de. Accesores manuales: ver el javadoc de la clase. */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String aOrdenDe;

    /** date. En JSON va como {@code yyyy-MM-dd}, sin hora ni zona. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaCheque;

    /** date. Solo cambia con la accion "Fecha de cobro" (o el formulario del administrador). */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaCobrar;

    /** float. */
    private Double monto;

    /** varchar(10). BS / SUS (dolares). */
    private String moneda;

    /** nvarchar(100). Codigo del catalogo v_tipos grupo 21. */
    private String tipo;

    /** varchar(50). PEN o CER. */
    private String estado;

    /** int. tch_banco.codBanco. */
    private Integer codBanco;

    /** int. 0 = lo entrego el cliente; si no, el empleado (jefe de cobranzas, cobrador o chofer). */
    private Integer codEmpleado;

    /** varchar(30). "0" cuando no hay recibo manual. */
    private String reciboManual;

    /** bigint. */
    private Long codSucursal;

    /** bigint. */
    private Long nroRecibo;

    /** varchar(20). "0" cuando no hay talonario. */
    private String nroTalonario;

    /** int. */
    private Integer codEmpresa;

    /** int. Sale del token, nunca del cuerpo. */
    private Integer audUsuario;

    /** datetimeoffset, como texto. La sella el procedimiento. Ver el javadoc de la clase. */
    private String audFecha;

    public String getaOrdenDe() {
        return aOrdenDe;
    }

    public void setaOrdenDe(String aOrdenDe) {
        this.aOrdenDe = aOrdenDe;
    }
}
