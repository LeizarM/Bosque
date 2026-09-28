package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Mapea la tabla <b>dbo.tpr_proveedorExtSap</b> (modulo de Precios, prefijo tpr_).
 *
 * <p>Catalogo de proveedores externos que llegan desde SAP. Contiene EXACTAMENTE
 * las cinco columnas de la tabla, ni una mas: este POJO lo serializa Jackson
 * dentro de {@code SpHelper.ejecutarAbm} y cada campo viaja como parametro del
 * procedimiento {@code p_abm_proveedorExtSap}. Un campo de display que no exista
 * como {@code @parametro} en el proc hace fallar el EXEC, por eso cualquier dato
 * que venga de un JOIN va en un DTO aparte y no aqui.
 *
 * <p>Catalogo referenciado por {@code tpr_producto.idProveedorSap}: un proveedor
 * en uso no se puede borrar.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorExtSap implements Serializable {

    /**
     * bigint NOT NULL IDENTITY, PK. Wrapper y no {@code long} a proposito:
     * en el alta viaja en null para que el proc deje trabajar al IDENTITY, y
     * el id real vuelve en {@code RespuestaSp.getIdGenerado()}.
     *
     * <p>El modelo viejo lo declaraba {@code int}: la columna es bigint y el
     * DAO viejo hacia {@code ps.setInt} sobre ella.
     */
    private Long idProveedorSap;

    /**
     * TRAMPA. La columna es <b>varchar(20)</b> NULL, no numerica, pero el proc
     * legacy declaraba {@code @codProvExtSap INT} y el modelo viejo lo tenia
     * como {@code int}. Consecuencias del desajuste, hoy vivas en produccion:
     * un codigo con letras o con ceros a la izquierda no se podia enviar, y el
     * filtro del listado obligaba a SQL Server a convertir la COLUMNA a int en
     * cada fila (error 245 si alguna fila tiene texto, y nunca usa indice).
     * El script {@code resources/sql/tpr_ProveedorExtSap.sql} corrige el
     * parametro a VARCHAR(20); por eso aqui es String.
     */
    private String codProvExtSap;

    /**
     * varchar(50) NULL en la tabla. OJO: el proc declara el parametro como
     * varchar(200), mas ancho que la columna, asi que un nombre de mas de 50
     * caracteres no se trunca: revienta con error 8152 al grabar. El script SQL
     * agrega una validacion que devuelve un mensaje legible en lugar de ese
     * error del motor.
     */
    private String proveedorExtSap;

    /** bigint NULL. Usuario de auditoria. */
    private Long audUsuario;

    /**
     * datetime NULL. El proc SIEMPRE la escribe con GETDATE() y no usa el
     * parametro recibido: lo que se mande desde Java se descarta. Se conserva
     * el campo porque es columna real de la tabla y porque el listado la
     * devuelve. El modelo viejo directamente no la tenia.
     */
    private Date audFecha;
}
