package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Mapea 1 a 1 la tabla <b>tpr_propuesta</b> (modulo de precios).
 *
 * <p>Contiene EXACTAMENTE las 10 columnas de la tabla, ni una mas: este POJO se
 * serializa completo con Jackson dentro de {@code SpHelper.ejecutarAbm} y cada
 * campo viaja como un {@code @parametro} de {@code p_abm_propuesta}. Un campo
 * que no exista como parametro del procedimiento hace fallar el EXEC entero.
 * Los campos de presentacion que llegan por JOIN o subconsulta (por ejemplo
 * {@code esAprobada} de la rama C de {@code p_list_propuesta}) viven en
 * {@link bo.bosque.com.impexpap.dto.PropuestaDto}, nunca aqui.
 *
 * <p>Todos los tipos son wrappers a proposito: salvo {@code idPropuesta}, todas
 * las columnas admiten NULL en la base y {@code BeanPropertyRowMapper} revienta
 * al intentar poner un NULL en un primitivo.
 *
 * <p>Procedimientos asociados: ABM {@code p_abm_propuesta} (acciones I, U, D, B)
 * y listado {@code p_list_propuesta} (acciones L, B, C, D, E, F).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Propuesta implements Serializable {

    /** bigint NOT NULL IDENTITY, PK. Wrapper por consistencia: en un alta todavia no existe. */
    private Long idPropuesta;

    /** bigint NULL. Empresa a la que pertenece la propuesta. */
    private Long codEmpresa;

    /**
     * tinyint NULL. Tipo de propuesta (la rama D de {@code p_list_propuesta}
     * devuelve solo esta columna). Se modela como Integer y no como Boolean ni
     * como byte: es un dominio de codigos, no una bandera.
     */
    private Integer tipo;

    /** varchar(200) NULL. OJO al migrar historicos: tpr_propuestaEliminado.titulo es varchar(50) y trunca. */
    private String titulo;

    /** varchar(250) NULL. OJO al migrar historicos: tpr_propuestaEliminado.obs es varchar(150) y trunca. */
    private String obs;

    /**
     * tinyint NULL. <b>Columna practicamente muerta.</b> En la base de prueba
     * 1121 de 1123 filas la tienen en 0, y los procedimientos la ignoran: el
     * alta de {@code p_abm_propuesta} escribe el literal 0 sin mirar el
     * parametro {@code @estado} y la modificacion ni siquiera la toca.
     *
     * <p>El estado real del flujo de aprobacion NO esta aqui: vive en
     * {@code tpr_autorizacion.esAprobada} (dominio v_tipos grupo 38, valores
     * 0 = en proceso, 1, 2, 3). Para conocer el estado de una propuesta hay que
     * leer la autorizacion — la rama C de {@code p_list_propuesta} lo hace y lo
     * expone en {@link bo.bosque.com.impexpap.dto.PropuestaDto#getEsAprobada()}.
     *
     * <p>Se conserva el campo porque la columna existe y el parametro
     * {@code @estado} sigue en el procedimiento; no debe usarse para decidir
     * nada del negocio.
     */
    private Integer estado;

    /** bigint NULL. Usuario que genero (exporto) la propuesta; lo setea la accion B del ABM. */
    private Long audUsGenerado;

    /** datetime NULL. Fecha de generacion; la accion B la pisa con GETDATE() del servidor. */
    private Date audFecGenerado;

    /** bigint NULL. Usuario que dio de alta o modifico la propuesta. */
    private Long audUsuario;

    /**
     * datetime NULL. El procedimiento la escribe siempre con GETDATE() del
     * servidor en las acciones I y U: lo que se mande desde Java se ignora.
     * El campo existe igual porque {@code @audFecha} es parametro del ABM y del
     * listado (ahi si filtra).
     */
    private Date audFecha;

}
