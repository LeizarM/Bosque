package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de <b>{@code p_list_propuesta} ACCION = 'E'</b> (items no
 * creados por familia) y <b>ACCION = 'F'</b> (el mismo caso por articulo).
 * Las dos ramas devuelven la misma forma, por eso comparten DTO.
 *
 * <p><b>Advertencia — forma NO verificable desde esta base.</b> Ambas ramas
 * hacen {@code SELECT * FROM OPENROWSET(...)} contra un servidor remoto
 * (192.168.3.114) y las columnas las decide el procedimiento
 * {@code [CONEXION].[dbo].[p_list_ItemsNoCreados]}, que no vive en esta base y
 * no se pudo auditar. Los nombres de abajo salen del unico rastro disponible:
 * el SELECT de ejemplo comentado dentro del cuerpo de {@code p_list_propuesta}.
 * Antes de dar por buena esta pantalla hay que ejecutar E y F contra el entorno
 * real y comparar el {@code ResultSetMetaData} con estos campos; si algun
 * nombre no coincide, {@code BeanPropertyRowMapper} deja ese campo en null sin
 * lanzar ningun error (objeto casi vacio, falla silenciosa).
 *
 * <p>El matching del mapper es case-insensitive (baja a minusculas y quita
 * espacios), asi que {@code IPXCode} del SQL entra igual en {@code ipxCode}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PropuestaItemNoCreadoDto implements Serializable {

    /** Codigo del articulo en Impexpap. Columna remota {@code IPXCode}. */
    private String ipxCode;

    /** Descripcion del articulo en Impexpap. Columna remota {@code INameIPX}. */
    private String iNameIPX;

    /** Peso / UTM en Impexpap. BigDecimal porque del otro lado es float(53) y perderia centavos. */
    private BigDecimal utmIPX;

    /** Unidad de medida. */
    private String unidadMedida;

    /** Codigo de familia del producto. */
    private Integer codFamilia;

    /** Codigo del articulo en Productiva (PAP). Columna remota {@code PAPCode}. */
    private String papCode;

    /** Descripcion del articulo en Productiva. Columna remota {@code INamePAP}. */
    private String iNamePAP;

    /** Peso / UTM en Productiva. */
    private BigDecimal utmPAP;

    /** Codigo del articulo en Esppapel (EPP). Columna remota {@code EPPCode}. */
    private String eppCode;

    /** Descripcion del articulo en Esppapel. Columna remota {@code INameEPP}. */
    private String iNameEPP;

    /** Peso / UTM en Esppapel. */
    private BigDecimal utmEPP;

}
