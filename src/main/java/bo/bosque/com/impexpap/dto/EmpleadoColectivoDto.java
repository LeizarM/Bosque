package bo.bosque.com.impexpap.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Una persona dentro de una <b>carga colectiva</b>: quién es, de qué relación y de qué sucursal, y
 * —cuando la pregunta es la simulación— cuántos días le tocarían y si entra.
 *
 * <p><b>DTO y no modelo</b> (regla del módulo: {@code model/} = forma de tabla, {@code dto/} =
 * forma calculada). Esto no es ninguna tabla: mezcla {@code tb_empleado}, {@code trh_persona},
 * {@code tb_relEmplEmpr}, {@code tb_sucursal}, {@code tb_empresa} y un número que sale de una
 * función de cálculo.
 *
 * <h3>Un solo tipo para el padrón y para la simulación</h3>
 * Las dos listas tienen exactamente la misma fila. El padrón
 * ({@code /colectivo/empleados}) llega con {@link #dias} en 0 y {@link #entra} en {@code true};
 * la simulación ({@code /colectivo/vacacion/simular}) llega con los días ya calculados por persona
 * y con los excluidos marcados en {@link #detalle}. Dos clases casi iguales serían dos archivos
 * para mantener sincronizados con la misma pantalla.
 *
 * <h3>Por qué {@link #dias} viaja POR PERSONA</h3>
 * En la vacación colectiva el número del cabezal <b>no es el que se graba</b>:
 * {@code f_CalcularDiasHabilesPermiso} descuenta los feriados de {@code trh_diaNoLaborable} que
 * apliquen a <b>la sucursal de esa persona</b> ({@code trh_diaNoLaborable_sucursal}), así que "5
 * días para todos" puede terminar siendo 4,5 para los de una sucursal. Confirmar sobre el número
 * del cabezal sería confirmar un número que nadie va a ver después en la tabla.
 */
@Data
@NoArgsConstructor
public class EmpleadoColectivoDto implements Serializable {

    private long codEmpleado;
    /** Apellidos + nombres, tal como los arma el SP. */
    private String datoEmpleado;

    /**
     * Su relación laboral activa. <b>La resuelve el servidor</b> y viaja de vuelta sólo para que la
     * pantalla pueda mostrarla: lo que sube el cliente es la lista de {@code codEmpleado} tildados
     * y nada más.
     */
    private long codRelEmplEmpr;

    /**
     * De dónde salen los feriados que se le descuentan.
     *
     * <p><b>No se graba en {@code trh_permiso}</b> —esa tabla no tiene columna de sucursal y
     * {@code p_abm_Permiso} no recibe el parámetro—: viaja para explicar en pantalla por qué a dos
     * personas del mismo lote les tocan días distintos.
     */
    private long codSucursal;
    private String datoSucursal;
    private String datoEmpresa;

    /** Lo que le tocaría a ESTA persona. En el padrón viene en 0: todavía no hay rango que calcular. */
    private double dias;
    /** {@link #dias} ya formateado por {@code Fmt.dias}, para que el cliente no redondee. */
    private String diasTxt;

    /**
     * {@link #dias} {@code * 8} — el campo "Horas de permiso" del modal individual. Sólo lo llena
     * la previsualización individual; en el padrón y en la simulación colectiva queda en 0 porque
     * esa pantalla razona en días.
     */
    private double horas;
    private String horasTxt;

    /**
     * "Horas a reponer" del modal legacy. <b>Es un cartel, no un dato</b>: la regla es
     * {@code tipoPermiso in ('otro','pcr') -> horas del permiso, si no 0}
     * ({@code WizardPermiso.verificarTipoPermiso}), y <b>no se persiste en ningún lado</b> —
     * {@code p_abm_Permiso} no tiene parámetro donde guardarlo y {@code trh_permiso} no tiene
     * columna. Viaja de vuelta para que la pantalla pueda avisar "este permiso hay que reponerlo",
     * y ahí termina. El día que exista el módulo de reposición, esto se registra en
     * {@code trh_repper} (hoy, 0 filas).
     */
    private double horasAReponer;
    private String horasAReponerTxt;

    /** Entra en el lote. Cuando es {@code false}, {@link #detalle} dice por qué. */
    private boolean entra;
    /** Motivo de la omisión. Vacío cuando {@link #entra}. */
    private String detalle;
}
