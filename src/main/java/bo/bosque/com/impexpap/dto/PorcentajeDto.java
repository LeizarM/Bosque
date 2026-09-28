package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Fila de las ramas A, B y C de <b>p_list_porcentaje</b>: la grilla de
 * "porcentajes por sucursal y lista de precios" que arma la pantalla de ABM de
 * porcentajes. No es la tabla tpr_porcentaje: cruza tpr_clasificacionPrecio,
 * tb_sucursal y tpr_producto, por eso es un DTO y no un model.
 *
 * <p>Los nombres de los campos son los alias del SELECT en camelCase, que es lo
 * unico que mira BeanPropertyRowMapper.
 *
 * <p>Las tres ramas comparten esta misma forma <b>una vez aplicado</b> el script
 * {@code src/main/resources/sql/tpr_Porcentaje.sql}, que da alias a las columnas
 * literales que hoy salen sin nombre y unifica {@code porcen} / {@code porcentaje}:
 * <ul>
 *   <li><b>A</b> (porcentajes vigentes de una familia): las 7 columnas.</li>
 *   <li><b>B</b> (sucursales y listas de precio para dar de alta): sin idPorcen,
 *       ese campo queda en null; porcentaje llega siempre en 0.</li>
 *   <li><b>C</b> (porcentajes faltantes): las 7 columnas, porcentaje siempre en 0.</li>
 * </ul>
 * Un campo que la rama no devuelve no rompe el mapeo: queda en null.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PorcentajeDto implements Serializable {

    /** tb_sucursal.codSucursal (bigint) — sucursal a la que pertenece la lista de precios. */
    private Long codSucursal;

    /** tpr_clasificacionPrecio.idClasificacion (bigint) — la lista de precios. */
    private Long idClasificacion;

    /**
     * tpr_porcentaje.idPorcen (bigint) cuando ya existe el registro.
     * En la rama A sin porcentajes cargados y en la rama C llega en 0, y en la
     * rama B no viene en el resultset: en ese caso queda null. 0 o null = alta.
     */
    private Long idPorcen;

    /** tb_sucursal.nombre — nombre de la sucursal. */
    private String nombre;

    /** tpr_clasificacionPrecio.nombrePrecio — nombre de la lista de precios. */
    private String nombrePrecio;

    /** tpr_clasificacionPrecio.vpp (int) — orden de la lista de precios; el SP ordena por este campo. */
    private Integer vpp;

    /**
     * ISNULL(tpr_porcentaje.porcen, 0) — float(53) en la base, BigDecimal aca.
     * Antes del script de correccion la rama B lo llamaba {@code porcen} y las
     * ramas A/C {@code porcentaje}; ahora las tres usan {@code porcentaje}.
     */
    private BigDecimal porcentaje;

}
