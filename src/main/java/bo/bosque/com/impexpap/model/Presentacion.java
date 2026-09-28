package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Mapea la tabla <b>dbo.tpr_presentacion</b> (modulo de Precios, prefijo tpr_).
 *
 * <p>Contiene EXACTAMENTE las cinco columnas de la tabla, ni una mas: este POJO
 * lo serializa Jackson dentro de {@code SpHelper.ejecutarAbm} y cada campo viaja
 * como parametro del procedimiento {@code p_abm_presentacion}. Un campo de
 * display que no exista como {@code @parametro} en el proc hace fallar el EXEC,
 * por eso los nombres que vienen de JOIN (por ejemplo el nombre del usuario de
 * auditoria) no van aqui sino en un DTO aparte.
 *
 * <p>Catalogo referenciado por {@code tpr_producto.idPresentacion}: una
 * presentacion en uso no se puede borrar.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Presentacion implements Serializable {

    /**
     * bigint NOT NULL IDENTITY, PK. Wrapper y no {@code long} a proposito:
     * en el alta viaja en null para que el proc deje trabajar al IDENTITY, y
     * el id real vuelve en {@code RespuestaSp.getIdGenerado()}.
     */
    private Long idPresentacion;

    /**
     * varchar(150) NULL en la tabla. OJO: el proc declaraba el parametro como
     * varchar(50) y truncaba en silencio; el script
     * {@code resources/sql/tpr_Presentacion.sql} lo amplia a 150.
     */
    private String presentacion;

    /**
     * int NULL. 1 = activa, 0 = inactiva. Es int y no Boolean porque la columna
     * es int y nada garantiza que solo existan esos dos valores.
     * En el alta el proc lo fuerza a 1 e ignora lo que se mande.
     */
    private Integer estado;

    /** bigint NULL. Usuario de auditoria. */
    private Long audUsuario;

    /**
     * datetime NULL. El proc SIEMPRE la escribe con GETDATE() y no usa el
     * parametro recibido: lo que se mande desde Java se descarta. Se conserva
     * el campo porque es columna real de la tabla y porque el listado la
     * devuelve.
     */
    private Date audFecha;
}
