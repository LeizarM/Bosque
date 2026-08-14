package bo.bosque.com.impexpap.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * Una fila de la <b>Nómina de permisos</b> ({@code p_list_Permiso} ACCION {@code 'Q'}): el kardex
 * de permisos del empleado.
 *
 * <h3>Por qué es un DTO y no {@code model/Permiso}</h3>
 * Por dos razones independientes, cualquiera de las dos alcanza:
 * <ul>
 *   <li><b>{@code model/Permiso} está congelado.</b> {@code diasDisponibles} lo usa con el overload
 *       de MODELO de {@code SpHelper.ejecutarListado}, que serializa TODOS sus campos a
 *       {@code @campo=?}. Un campo más y ese {@code EXEC} falla con "too many arguments", tirando
 *       abajo {@code /permiso-rrhh/saldo/ficha}, que está en producción.</li>
 *   <li><b>Esto no es la forma de la tabla.</b> {@code datoTipoPermiso} sale de un join a
 *       {@code v_tipos} y {@code horas} no existe en ninguna columna: la calcula el servidor. La
 *       convención del módulo es {@code model/} = forma de tabla, {@code dto/} = forma calculada.</li>
 * </ul>
 *
 * <h3>Las columnas que el SP devuelve y acá NO están</h3>
 * <ul>
 *   <li><b>{@code diasAdeudados}</b> — las columnas "Deuda En Día(s)" / "Deuda En Hr(s)" del
 *       legacy. La fórmula del SP es
 *       {@code ISNULL(SUM(trh_repper.cantidadDiasRepuestos),0) - cantidadDias} sólo para
 *       {@code tipoPermiso='otro'} desde 2019. <b>{@code trh_repper} tiene 0 filas</b> (verificado
 *       contra la base), así que la resta da el NEGATIVO de los días: la grilla mostraría deudas
 *       de -0,25 a -2 días en las 27 filas 'otro' y 0 en las otras 8.432. No se mapea a propósito
 *       — BeanPropertyRowMapper ignora la columna sin setter y el dato malo nunca sale del SP.
 *       Vuelve a existir el día que exista el módulo de reposición y {@code trh_repper} tenga
 *       filas.</li>
 *   <li><b>{@code datoAutorizador}</b> — el SP lo devuelve como {@code ''} literal. No es un dato.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
public class PermisoKardexDto implements Serializable {

    private long codPermiso;
    private long codEmpleado;
    private long codRelEmplEmpr;

    /**
     * El nombre de la persona. La ACCION {@code 'Q'} lo devuelve siempre, pero hasta que existió
     * «quién está fuera» no hacía falta: el kardex es de un empleado y el nombre ya está en la
     * cabecera de la pantalla. En la consulta de toda la empresa es la columna principal.
     */
    private String datoEmpleado;

    /** El código crudo ({@code vac}, {@code otro}, {@code pva}…). Lo usa el filtro, no la grilla. */
    private String tipoPermiso;
    /** {@code v_tipos.descripcion} del grupo 13 — la columna "Permiso" de la grilla. */
    private String datoTipoPermiso;

    private Date desde;
    private Date hasta;
    private String motivo;

    /** {@code trh_permiso.cantidadDias}, tal como quedó grabado. La columna "En Día(s)". */
    private double cantidadDias;

    /**
     * La columna "En Hr(s)". <b>Derivada, no leída</b>: {@code cantidadDias * 8}, que es la misma
     * regla con la que {@code f_CalcularDiasHabilesPermiso} produce los días
     * ({@code SUM(minutos)/480}). Ojo con el horario continuo: un día completo topea en 600
     * minutos, o sea 1,25 "días" = 10 horas. Las horas son exactas; los días son horas/8.
     */
    private double horas;

    /** Ya formateados, para que el cliente no vuelva a redondear y las dos unidades no discrepen. */
    private String cantidadDiasTxt;
    private String horasTxt;
}
