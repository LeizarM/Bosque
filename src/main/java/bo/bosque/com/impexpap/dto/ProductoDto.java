package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_producto @ACCION='L'</b>: la familia con sus
 * descripciones resueltas por JOIN (proveedor, grupo de familia, presentacion,
 * tipo, rango de gramaje y color).
 *
 * <p>Los nombres de los campos son literalmente los alias del SELECT, porque el
 * mapeo es por nombre de columna ({@code BeanPropertyRowMapper}). No renombrar:
 * en esta rama el proveedor viene como {@code proveedorSap} y el grupo de familia
 * como {@code grpFamilia}, mientras que otras ramas del mismo SP los llaman
 * {@code proveedorExtSap} / {@code grpFam}. Por eso hay un DTO por rama.
 *
 * <p>Esta rama NO devuelve las llaves foraneas (idPresentacion, idTipo, idColor,
 * idRangoGram): si la pantalla necesita precargar los combos de edicion, usar la
 * rama LL, que devuelve el registro crudo en {@code Producto}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDto {

    private Integer codigoFamilia;

    /** ISNULL sobre pe.proveedorExtSap: si no hay proveedor llega el texto por defecto, nunca null. */
    private String proveedorSap;

    /** ISNULL sobre g.grpFam: si no hay grupo llega el texto por defecto, nunca null. */
    private String grpFamilia;

    private String presentacion;

    private String tipo;

    /** Texto armado en el SP con el formato "[ min - max ]". No es numerico. */
    private String rangoGramaje;

    private String gramaje;

    private String formato;

    private String color;

    private Integer estado;

    /** float(53) en la base; BigDecimal para no perder centavos. */
    private BigDecimal costoTM;

    private Long idPropuestaAprobada;

    private Long audUsuario;
}
