package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Mapea 1 a 1 la tabla <b>tpr_tcAncla</b> (modulo de Precios).
 *
 * <p>Es la tabla de configuracion del <b>reprecio nocturno</b>: guarda, por empresa
 * (base de datos SAP), cual fue el tipo de cambio con el que se repreciaron los
 * precios por ultima vez (el "ancla") y cual es la variacion minima que debe
 * acumular el tipo de cambio para volver a disparar un reprecio (el "umbral").
 * La logica del reprecio en si NO vive aqui: este POJO es solo el reflejo de la fila.
 *
 * <p>Contiene EXACTAMENTE las 6 columnas de la tabla, ni una mas. Cualquier campo
 * de display que venga de un JOIN debe vivir en un DTO aparte, nunca en este model.
 *
 * <p><b>Llave primaria:</b> {@code companyDB} — es {@code nvarchar(100)} y <b>NO es
 * IDENTITY</b>. La tabla no tiene columna de identidad, asi que el ABM nunca puede
 * devolver {@code SCOPE_IDENTITY()}: el script de creacion deja {@code @idGenerado}
 * en 0 a proposito.
 *
 * <p><b>Trampa de serializacion:</b> este model NO se puede pasar a
 * {@code SpHelper.ejecutarAbm(sp, model, accion)}. Jackson mandaria los 6 campos
 * como parametros y {@code p_abm_tcAncla} solo declara
 * {@code @companyDB, @tc, @umbral, @fechaAncla}: los campos {@code tcAncla},
 * {@code tcUltimo} y {@code fechaUltimo} no existen como parametro y el EXEC
 * reventaria. Por eso {@code TcAnclaDao} usa siempre {@code ejecutarAbmMap} con los
 * nombres reales de los parametros.
 *
 * @see bo.bosque.com.impexpap.dao.TcAnclaDao
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TcAncla implements Serializable {

    /**
     * PK de tpr_tcAncla. nvarchar(100) NOT NULL, NO identity.
     * <p>Es el nombre de la base de datos de la empresa en SAP (por ejemplo
     * {@code SBO_IMPEXPAP}); lo define el negocio, no la base.
     */
    private String companyDB;

    /**
     * Tipo de cambio con el que se repreciaron los precios por ultima vez.
     * float(53) NOT NULL en la base.
     * <p><b>Trampa:</b> la columna es FLOAT, no DECIMAL. Se mapea a
     * {@code BigDecimal} a proposito para no arrastrar el error binario del float
     * al calculo; al mostrarlo hay que redondear en la capa de servicio/vista.
     * <p>Ojo con el nombre: el parametro del ABM se llama {@code @tc}, NO
     * {@code @tcAncla}.
     */
    private BigDecimal tcAncla;

    /**
     * Fecha en la que se fijo el ancla. datetime NOT NULL.
     * <p><b>Trampa:</b> en un alta nueva se siembra deliberadamente con
     * {@code 1900-01-01} y no con la fecha del dia, porque el job cuenta como
     * "precios pendientes" todo {@code tpr_precio} con {@code audFecha > fechaAncla};
     * sembrar con GETDATE() dejaba a la empresa nueva con cero pendientes para siempre.
     */
    private Date fechaAncla;

    /** Ultimo tipo de cambio observado, haya disparado reprecio o no. float(53) NULL. */
    private BigDecimal tcUltimo;

    /** Fecha de la ultima lectura del tipo de cambio. datetime NULL. */
    private Date fechaUltimo;

    /**
     * Variacion minima del tipo de cambio que habilita un nuevo reprecio.
     * float(53) NOT NULL, DEFAULT 0.10 en la base (es uno de los dos unicos
     * DEFAULT de todo el modulo tpr_).
     * <p>Es una proporcion, no un porcentaje: 0.10 significa 10 %.
     */
    private BigDecimal umbral;
}
