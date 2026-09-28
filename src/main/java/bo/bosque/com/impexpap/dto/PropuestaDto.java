package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Forma del resultset de <b>{@code p_list_propuesta} ACCION = 'C'</b>
 * (datos de una propuesta por su id, para la pantalla de detalle).
 *
 * <p>No es la tabla: esa rama devuelve 5 columnas, cuatro de
 * {@code tpr_propuesta} mas {@code esAprobada}, que sale de una subconsulta
 * correlacionada a {@code tpr_autorizacion}. Por eso no puede mapearse sobre
 * {@link bo.bosque.com.impexpap.model.Propuesta}: si {@code esAprobada}
 * estuviera en el modelo viajaria como parametro inexistente en
 * {@code p_abm_propuesta} y rompería el ABM.
 *
 * <p>Los nombres son exactamente los alias del SELECT en camelCase, que es lo
 * unico que sabe mapear {@code BeanPropertyRowMapper}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PropuestaDto implements Serializable {

    /** bigint. Id de la propuesta. */
    private Long idPropuesta;

    /** bigint NULL. Empresa de la propuesta. */
    private Long codEmpresa;

    /** varchar(200) NULL. */
    private String titulo;

    /** varchar(250) NULL. */
    private String obs;

    /**
     * tinyint NULL, viene de {@code tpr_autorizacion.esAprobada} — <b>este es el
     * estado real del flujo</b>, no {@code Propuesta.estado}.
     *
     * <p>Es Integer y no Boolean: el dominio (v_tipos grupo 38) tiene cuatro
     * valores, 0 = en proceso mas 1, 2 y 3.
     *
     * <p>Llega en null si la propuesta todavia no tiene fila en
     * {@code tpr_autorizacion} (no deberia pasar: el alta crea las dos filas en
     * la misma transaccion, pero los historicos anteriores al arreglo pueden
     * tenerlo).
     */
    private Integer esAprobada;

}
