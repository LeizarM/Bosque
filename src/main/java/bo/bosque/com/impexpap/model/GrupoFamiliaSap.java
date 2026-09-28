package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Mapea la tabla <b>tpr_grupoFamiliaSap</b> (modulo de precios): el catalogo de
 * grupos de familia de SAP y su equivalencia de codigo en cada empresa.
 *
 * <p>Contiene EXACTAMENTE las columnas de la tabla, ni una mas: el POJO se
 * serializa entero como parametros de <code>p_abm_grupoFamiliaSap</code> a traves
 * de {@code SpHelper.ejecutarAbm}, y cualquier campo que no exista como
 * <code>@parametro</code> del procedimiento hace fallar el EXEC. Los campos de
 * display que salgan de un JOIN van en un DTO aparte.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GrupoFamiliaSap implements Serializable {

    /** PK. bigint IDENTITY. Wrapper: viaja en null al insertar. */
    private Long idGrpFamiliaSap;

    /**
     * Codigo del grupo en SAP de IMPEXPAP. <b>varchar(20) en la tabla, no numerico</b>:
     * el procedimiento legacy lo declaraba INT y el modelo anterior lo tenia como int,
     * lo que rompia (o convertia mal) cualquier codigo alfanumerico.
     */
    private String codGrpFamSap;

    /** Codigo del grupo en SAP de ESPPAPEL. varchar(20), mismo criterio que codGrpFamSap. */
    private String codGrpFamSapEpp;

    /**
     * Codigo del grupo en SAP de PRODUCTIVA PAPEL. varchar(20).
     * Existe en la tabla desde antes, pero ni el modelo ni los procedimientos la
     * contemplaban: se agrega aqui y en el script tpr_GrupoFamiliaSap.sql.
     *
     * <p><b>Trampa al actualizar:</b> el procedimiento la graba con
     * <code>ISNULL(@codGrpFamSapProdPap, codGrpFamSapProdPap)</code> para que el JSF
     * viejo, que no manda el parametro, no borre el valor cargado. Enviar null en una
     * actualizacion significa "no tocar"; para limpiar la columna hay que mandar cadena vacia.
     */
    private String codGrpFamSapProdPap;

    /** Nombre del grupo de familia. varchar(150). */
    private String grpFam;

    /** Alias o nombre corto. varchar(250) en la tabla (el proc declara 300, ver el script SQL). */
    private String alias;

    /** Usuario de auditoria. bigint NULL -> wrapper. */
    private Long audUsuario;

    /**
     * Fecha de auditoria. El procedimiento la pisa con GETDATE() cuando llega en null,
     * asi que normalmente no hace falta enviarla desde el cliente.
     */
    private Date audFecha;
}
