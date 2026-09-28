package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Mapea 1 a 1 la tabla <b>tpr_producto</b> (modulo de Precios).
 *
 * <p>Contiene EXACTAMENTE las 14 columnas de la tabla, ni una mas: este POJO se
 * serializa completo con Jackson dentro de {@code SpHelper.ejecutarAbm} y cada campo
 * viaja como parametro de {@code p_abm_producto}. Un campo que no exista como
 * {@code @parametro} en el procedimiento rompe el EXEC.
 * Los campos de presentacion que vienen de JOIN (proveedorSap, grpFamilia, presentacion,
 * color, tipo, rangoGramaje) viven en {@code bo.bosque.com.impexpap.dto.ProductoDto}.
 *
 * <p><b>Llave primaria:</b> {@code codigoFamilia} es la PK y <b>NO es IDENTITY</b>;
 * el codigo lo define el usuario al dar de alta la familia. Por eso el ABM no puede
 * devolver {@code SCOPE_IDENTITY()} y el script de ALTER retorna el propio
 * {@code @codigoFamilia} en {@code @idGenerado}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Producto implements Serializable {

    /** PK de tpr_producto. int NOT NULL, NO identity: lo carga el usuario. */
    private Integer codigoFamilia;

    /** FK -> tpr_grupoFamiliaSap. bigint NULL (el SP legacy lo declaraba INT). */
    private Long idGrpFamiliaSap;

    /** FK -> tpr_proveedorExtSap. bigint NULL. */
    private Long idProveedorSap;

    /** FK -> tpr_presentacion. bigint NULL. */
    private Long idPresentacion;

    /** FK -> tpr_tipo. bigint NULL. */
    private Long idTipo;

    /**
     * FK -> tpr_RangoGramaje. bigint NULL.
     * <p><b>Trampa:</b> la columna y el parametro del SP se llaman {@code idRangoGram},
     * NO {@code idRangoGramaje} (asi se llamaba el campo en el modelo viejo, por lo que
     * nunca coincidia con el nombre del parametro).
     */
    private Long idRangoGram;

    /**
     * varchar(50) NULL.
     * <p><b>Trampa:</b> hoy esta 100% vacia en la base de produccion; la pantalla puede
     * mostrarla siempre en blanco sin que sea un error de mapeo.
     */
    private String formato;

    /**
     * varchar(50) NULL. Texto, no numero.
     * <p><b>Trampa:</b> igual que {@code formato}, hoy esta 100% vacia en la base.
     */
    private String gramaje;

    /** FK -> tpr_color. bigint NULL. */
    private Long idColor;

    /**
     * int NULL. 1 = activa, 0 = inactiva.
     * <p>Se deja como Integer y no como Boolean: en este modulo los "estado" son enteros
     * y el ABM los compara contra 1 en varias ramas.
     */
    private Integer estado;

    /**
     * float(53) NULL en la base -> BigDecimal en Java.
     * <p><b>Trampa:</b> es dinero (costo por tonelada metrica). Mapearlo a float/double
     * pierde centavos por representacion binaria. Redondear con
     * {@code setScale(2, HALF_UP)} recien en la capa de servicio o vista.
     */
    private BigDecimal costoTM;

    /** FK -> tpr_propuesta. bigint NULL. Ultima propuesta aprobada para esta familia. */
    private Long idPropuestaAprobada;

    /** bigint NULL. Usuario que hizo el ultimo movimiento. */
    private Long audUsuario;

    /**
     * datetime NULL.
     * <p>El ABM la pisa siempre con {@code GETDATE()}; el campo existe porque el SP
     * declara el parametro {@code @audFecha} y porque la rama LL del listado la devuelve.
     */
    private Date audFecha;
}
