package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Cabecera anual del Rol de Turnos de Sábado — tabla {@code trs_Rol}.
 *
 * <p>SPs: {@code p_abm_trs_Rol} (I/U/D) · {@code p_list_trs_Rol} (L, I).
 *
 * <p>Un rol = un año + un alcance (empresa/sucursal) + una versión.
 * {@code codSucursal} NULL significa "todos los activos".
 *
 * <p><b>El alta NO crea una fila suelta:</b> {@code p_abm_trs_Rol @ACCION='I'}
 * delega en {@code trs_sp_generarRol}, que además arma los ~52 sábados y todos
 * los participantes con su grupo A/B. Por eso {@code fechaInicio}, {@code fechaFin}
 * y {@code version} los calcula el SP y no son parámetros del ABM.
 *
 * <p><b>Ojo con {@code @ACCION='I'} de {@code p_list_trs_Rol}:</b> devuelve las
 * intervenciones humanas sobre el rol, que son una forma DISTINTA (una fila por
 * intervención, no por rol). Ese listado necesita su propia clase de proyección
 * en {@code dto/}; no se mapea contra este modelo.
 *
 * <p>Los campos que salen como <b>wrapper</b> ({@code Long} / {@code Integer}) son las
 * columnas que en BD admiten NULL: {@code BeanPropertyRowMapper} revienta si intenta
 * meter un NULL en un primitivo. En el ABM tambien conviene, porque asi un campo sin
 * cargar viaja como NULL y no como 0.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Rol implements Serializable {

    // ── campos de la tabla trs_Rol ────────────────────────────────────────
    private long   idRol;
    private Long   codEmpresa;          // NULL = todas
    private Long   codSucursal;         // NULL = todos los activos
    private int    anio;
    private String nombre;              // varchar(60) — el SP arma 'ROL ' + anio
    private Integer turnosObjetivoA;     // meta anual del grupo A (~26)
    private Integer turnosObjetivoB;
    private Integer coberturaObjetivo;   // personas esperadas por sábado

    /**
     * BD: {@code bit NOT NULL DEFAULT 1}. Se inicializa en 1 A PROPÓSITO — el
     * default de un {@code int} en Java es 0, y Jackson lo serializaría como 0,
     * APAGANDO el asueto por cumpleaños de todo el rol sin que nadie lo pidiera.
     * Mismo caso que {@code Cotizaciones.nroGiros}.
     */
    private int aplicaAsuetoCumple = 1;

    private String estado;              // BORRADOR | PUBLICADO | CERRADO
    private String observacion;         // varchar(250)
    private Long   audUsuario;

    // ── solo lectura: los calcula el SP, no son parámetros del ABM ────────
    private Date fechaInicio;           // = MIN(fecha) de los sábados
    private Date fechaFin;              // = MAX(fecha) de los sábados
    private int  version = 1;           // BD: DEFAULT 1
    private Date audFecha;

    // ── solo lectura, ACCION 'L': join y contadores ───────────────────────
    private String sucursal;            // tb_sucursal.nombre
    private int    sabados;
    private int    participantes;       // solo los activos
    private int    celdas;
}
