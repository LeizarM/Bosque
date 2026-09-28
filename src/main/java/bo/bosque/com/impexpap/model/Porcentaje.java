package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Mapea la tabla <b>tpr_porcentaje</b> (porcentaje de incremento por familia de
 * producto y lista de precios / clasificacion).
 *
 * <p>Contiene EXACTAMENTE las columnas de la tabla, ni una mas: este POJO se
 * serializa completo con Jackson en {@code SpHelper.ejecutarAbm} y cada campo
 * viaja como parametro de {@code p_abm_porcentaje}. Un campo que no exista como
 * {@code @parametro} en el procedimiento hace fallar el EXEC.
 *
 * <p>Los campos de presentacion que devuelven las ramas A, B y C de
 * {@code p_list_porcentaje} (codSucursal, nombre, nombrePrecio, vpp) viven en
 * {@link bo.bosque.com.impexpap.dto.PorcentajeDto}, no aqui.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Porcentaje implements Serializable {

    /** tpr_porcentaje.idPorcen — bigint NOT NULL IDENTITY, PK. Wrapper porque en un alta todavia no tiene valor. */
    private Long idPorcen;

    /** tpr_porcentaje.codigoFamilia — int NULL, FK a tpr_producto.codigoFamilia. Es INT, no BIGINT como el resto de los ids. */
    private Integer codigoFamilia;

    /** tpr_porcentaje.idClasificacion — bigint NULL, FK a tpr_clasificacionPrecio. */
    private Long idClasificacion;

    /**
     * tpr_porcentaje.porcen — float(53) NULL.
     * Se mapea a BigDecimal a proposito: el float binario no representa 0.01 y
     * al ser un porcentaje aplicado sobre precios se perderian centavos.
     */
    private BigDecimal porcen;

    /** tpr_porcentaje.audUsuario — bigint NULL. */
    private Long audUsuario;

    /**
     * tpr_porcentaje.audFecha — datetime NULL.
     * Solo sirve como filtro en la rama L del listado: en las altas y
     * modificaciones el procedimiento la pisa siempre con GETDATE().
     */
    private Date audFecha;

}
