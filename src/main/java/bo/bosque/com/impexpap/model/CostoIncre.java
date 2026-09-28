package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Mapea 1 a 1 la tabla <b>tpr_costoIncre</b> (costo de incremento / flete de
 * transporte por sucursal dentro de una propuesta de precios).
 *
 * <p><b>Contiene EXACTAMENTE las columnas de la tabla.</b> SpHelper.ejecutarAbm
 * serializa el POJO con Jackson y manda todos sus campos como parametros de
 * p_abm_costoIncre: un campo de mas que no exista como @parametro hace fallar el
 * EXEC. Los campos de display que salen de los JOIN de p_list_costoIncre
 * (nombre de sucursal, ciudad, codCiudad) viven en
 * {@link bo.bosque.com.impexpap.dto.CostoIncreSucursalDto} y
 * {@link bo.bosque.com.impexpap.dto.CostoIncreCiudadDto}, nunca aqui.
 *
 * <p>Columnas segun el diccionario de datos:
 * idIncre bigint IDENTITY PK | codSucursal bigint NULL FK tb_sucursal |
 * idPropuesta bigint NULL FK | valor float(53) NULL | audUsuario bigint NULL |
 * audFecha datetime NULL.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CostoIncre implements Serializable {

    /** PK IDENTITY. Llega en 0/null en el alta: lo devuelve el SP en @idGenerado. */
    private Long idIncre;

    /** FK a tb_sucursal. Wrapper porque la columna admite NULL. */
    private Long codSucursal;

    /**
     * FK a tpr_propuesta. OJO: la rama 'L' de p_list_costoIncre NO devuelve esta
     * columna en el legacy; el script tpr_CostoIncre.sql la agrega al FINAL del
     * SELECT para no correr las posiciones que lee el JSF por indice.
     */
    private Long idPropuesta;

    /**
     * float(53) en la BD. Se mapea a BigDecimal a proposito: es dinero y el
     * float binario no representa 0.01 exacto. Redondear con
     * setScale(2, HALF_UP) en la capa de servicio o vista.
     */
    private BigDecimal valor;

    /** FK al usuario que grabo. bigint NULL en la BD. */
    private Long audUsuario;

    /**
     * La escribe el propio SP con GETDATE() en las acciones 'I' y 'U'; el
     * parametro @audFecha existe en p_abm_costoIncre pero el proc lo ignora.
     * Se conserva en el modelo porque la rama 'L' del listado si la devuelve.
     */
    private Date audFecha;
}
