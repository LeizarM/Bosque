package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Mapea la tabla <b>tpr_RangoGramaje</b> del modulo de Precios.
 *
 * <p>Es el catalogo de rangos de gramaje del papel: cada fila es un intervalo
 * cerrado [min - max] en gramos por metro cuadrado. Lo consumen
 * tpr_grupoFamTipoRangoGram (que asigna un rango a cada par grupo de familia /
 * tipo de papel) y tpr_producto.idRangoGram.
 *
 * <p><b>Contiene EXACTAMENTE las 5 columnas de la tabla.</b> No agregar aqui
 * campos de despliegue: {@code SpHelper.ejecutarAbm} serializa este POJO con
 * Jackson y manda todos sus campos como parametros de
 * {@code p_abm_rangoGramaje}; un campo que no exista como parametro del SP hace
 * fallar el EXEC. El texto formateado "[ min - max ]" que devuelven las ramas
 * 'A' y 'B' del listado vive en
 * {@link bo.bosque.com.impexpap.dto.RangoGramajeDto}.
 *
 * <p><b>Trampas de esta tabla:</b>
 * <ul>
 *   <li>min y max son {@code decimal(16,2)}: son los <b>unicos numericos
 *       exactos de todo el modulo de precios</b> (el resto es float(53)). Van en
 *       {@link BigDecimal} si o si; ver el javadoc de los campos.</li>
 *   <li>Salvo la PK todas las columnas admiten NULL, por eso los campos son
 *       wrappers: {@code BeanPropertyRowMapper} revienta al escribir un NULL en
 *       un primitivo.</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RangoGramaje implements Serializable {

    /** bigint NOT NULL IDENTITY. PK. El alta lo devuelve en {@code RespuestaSp.getIdGenerado()}. */
    private Long idRangoGram;

    /**
     * Limite inferior del rango, en g/m2. <b>decimal(16,2) en la base.</b>
     *
     * <p>El modelo viejo lo declaraba {@code float} y el DAO viejo lo enviaba con
     * {@code ps.setFloat()} contra un parametro que el SP declaraba {@code INT}:
     * la parte decimal se perdia dos veces (float binario no representa 0.01, y
     * la conversion a INT trunca). Un rango 80,50 se guardaba como 80. Por eso
     * aqui es {@link BigDecimal} y el script
     * {@code src/main/resources/sql/tpr_RangoGramaje.sql} cambia el parametro del
     * SP a DECIMAL(16,2).
     */
    private BigDecimal min;

    /**
     * Limite superior del rango, en g/m2. <b>decimal(16,2) en la base.</b>
     * Mismo motivo que {@link #min} para usar {@link BigDecimal}.
     */
    private BigDecimal max;

    /** bigint NULL. Usuario que hizo el ultimo movimiento. */
    private Long audUsuario;

    /**
     * datetime NULL. <b>Solo lectura desde la aplicacion:</b> el SP de ABM ignora
     * el valor que se le mande y siempre estampa GETDATE(). Se conserva el campo
     * porque la columna existe y el listado la devuelve (el script agrega
     * audFecha al final del SELECT de la rama 'L', que hoy no la trae).
     */
    private Date audFecha;
}
