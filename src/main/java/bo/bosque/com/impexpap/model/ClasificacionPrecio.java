package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Mapea la tabla <b>tpr_clasificacionPrecio</b> (lista de precios por sucursal).
 *
 * <p>Contiene EXACTAMENTE las columnas de la tabla, ni una mas: {@code SpHelper.ejecutarAbm}
 * serializa el POJO con Jackson y manda cada campo como parametro del SP, asi que un campo
 * de display (por ejemplo el nombre de la sucursal, que viene de un JOIN) haria fallar el
 * EXEC. Esos campos viven en {@code bo.bosque.com.impexpap.dto.ClasificacionPrecioDto}.
 *
 * <p>Todos los tipos son wrappers a proposito: salvo la PK, la tabla permite NULL en todas
 * las columnas y {@code BeanPropertyRowMapper} revienta al asignar NULL sobre un primitivo.
 *
 * <p>SP asociados: {@code p_abm_clasificacionPrecio} (ACCION I/B, U, A, D) y
 * {@code p_list_clasificacionPrecio} (ACCION L, B, C, D).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClasificacionPrecio implements Serializable {

    /** PK, bigint IDENTITY. En alta llega null y el SP lo devuelve en @idGenerado. */
    private Long idClasificacion;

    /**
     * bigint NULL, FK a tb_sucursal.codSucursal.
     * <p>TRAMPA: el SP historico declara este parametro como {@code @idSucursal}, no como
     * {@code @codSucursal}. El ALTER de {@code resources/sql/tpr_ClasificacionPrecio.sql}
     * agrega {@code @codSucursal} al final con DEFAULT NULL y lo resuelve con COALESCE,
     * para que el nombre del campo Java coincida con el de la columna sin romper al JSF.
     */
    private Long codSucursal;

    /** bigint NULL. Numero de lista de precios en SAP (Business One), no es un id interno. */
    private Long listNum;

    /**
     * varchar(100) NULL.
     * <p>TRAMPA: el parametro del SP era varchar(50) mientras la columna es varchar(100);
     * el ALTER lo iguala a 100 para que no trunque silenciosamente.
     */
    private String nombrePrecio;

    /** int NULL. Numero de la lista de precios de la vista de precios (VPP). */
    private Integer vpp;

    /** int NULL. 1 = activo, 0 = desactivado (asi lo escribe la rama de alta del SP). */
    private Integer estado;

    /** bigint NULL. Usuario de auditoria. */
    private Long audUsuario;

    /** datetime NULL. Lo sella el SP con GETDATE(); no se toma del cliente. */
    private Date audFecha;
}
