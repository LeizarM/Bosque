package bo.bosque.com.impexpap.dto;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Proyección de {@code p_list_trs_Sabado @ACCION='N'} — los días no laborables de
 * RR.HH. vistos desde el rol de sábados.
 *
 * <p>Lee {@code trh_diaNoLaborable}, que es de RR.HH. y no del módulo, por eso es un
 * DTO. <b>No confundir con el modelo {@code model.DiaNoLaborable}</b>, que es otra
 * proyección más chica usada por {@code p_listaDiasNoLaborables}.
 *
 * <p>Muestra TODOS los feriados, no sólo los que caen sábado, porque el caso que
 * importa es el <b>PUENTE</b>: el feriado cae viernes y RR.HH. declara además el
 * sábado. Si eso pasa DESPUÉS de generar el rol, hay que correr
 * {@code trs_sp_refrescarFeriados} — nada avisa solo.
 *
 * <p>{@code sucursalesAlcanzadas} importa: los feriados son POR SUCURSAL y hay
 * departamentales que aplican a una sola.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DiaNoLaborableDto implements Serializable {

    private long   codDiaNoLaborable;
    private Date   fecha;
    private String motivo;
    private int    anio;
    private String diaSemana;
    private int    esSabado;
    /** 1 = el día anterior también es no laborable y era viernes. */
    private int    esPuenteDeViernes;
    /** A cuántas sucursales alcanza, según trh_diaNoLaborable_sucursal. */
    private int    sucursalesAlcanzadas;
    /** En cuántos roles quedó ya materializado como sábado 'X'. */
    private int    rolesQueLoAplicaron;
    /** Wrapper: {@code trh_diaNoLaborable.audUsuarioI} admite NULL. */
    private Long   audUsuarioI;
    private Date   audFechaI;
}
