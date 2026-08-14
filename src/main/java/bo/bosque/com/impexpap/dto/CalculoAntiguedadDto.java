package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Proyección de {@code p_list_Permiso @ACCION='U'} — la calculadora de antigüedad.
 *
 * <p>El cuerpo entero de esa ACCION son tres líneas:
 * {@code select dbo.f_calcDiasLaborablesExt(@desde, @hasta) as datoAntCalc}. Una fila, una
 * columna, un {@code VARCHAR(200)} en prosa.
 *
 * <p><b>El string NO se parsea.</b> Se muestra tal cual, porque el criterio de aceptación #7
 * compara la frase literal contra el modal {@code apyCalcAntModal} del ERP legacy. Si algún
 * día se quiere el desglose numérico se pide una ACCION nueva que devuelva las 5 magnitudes en
 * columnas — no se le hace ingeniería inversa a una frase.
 *
 * <p><b>Dos casos límite que la UI tiene que avisar.</b> {@code f_calcDiasLaborablesExt}
 * devuelve <b>0 días por antigüedad</b> —callada— cuando {@code hasta} cae justo en el
 * aniversario de {@code desde}, y también cuando las dos fechas están en el mismo mes: en los
 * dos casos saltea el bloque que calcula meses y días. La pantalla detecta la subcadena
 * <b>{@code "= 0 dias por antiguedad"}</b> y muestra el aviso de caso límite. (El literal
 * {@code "Es la fecha de cumple Felicidades"} que menciona el borrador NO existe en ningún
 * {@code RETURN}: vive dentro de un {@code ----print} comentado.)
 *
 * <p><b>Es orientativa, y así se rotula.</b> Su algoritmo —años enteros por {@code WHILE}— no
 * es el de {@code f_calcVacacionesAnioLaboral} —años con fracción—, así que cerca de un
 * aniversario las dos caen en tramos distintos de la escala de {@code tb_parametro id='VAC'} y
 * dan números distintos. No es el saldo del empleado: es una simulación.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CalculoAntiguedadDto implements Serializable {

    /**
     * La frase completa, literal, con sus rarezas de formato incluidas: empieza con un espacio
     * ({@code " Trabajo ..."}) y lleva un espacio ANTES de la coma en
     * {@code "Dias acumulados , hasta el momento"}. Nada de {@code trim()}.
     */
    private String datoAntCalc;
}
