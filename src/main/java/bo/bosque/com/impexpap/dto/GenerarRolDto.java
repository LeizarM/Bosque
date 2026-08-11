package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Payload de {@code trs_sp_generarRol}, el SP que arma el rol entero: la cabecera,
 * los ~52 sábados y todos los participantes con su grupo A/B.
 *
 * <p>No es un {@code Rol}: acá no hay {@code idRol} ni {@code estado}, porque el rol
 * todavía no existe. Lo que viaja son los parámetros de la generación.
 *
 * <p><b>{@code modo} es el campo que importa:</b>
 * <ul>
 *   <li>{@code CREAR} — falla si el rol de ese año y alcance ya existe.</li>
 *   <li>{@code REGENERAR} — reconcilia contra el organigrama de hoy: agrega a los que
 *       entraron, da de baja a los que salieron y <b>rehace sólo las celdas
 *       {@code origen='G'}</b>. Las correcciones manuales ('M') y lo que programó un
 *       jefe ('P') sobreviven.</li>
 * </ul>
 *
 * <p>El alcance sale de {@code codSucursal} y {@code codEmpresa}: los dos en 0 (o sin
 * mandar) generan un rol <b>global</b>, con toda la empresa adentro. No es lo mismo que
 * generar uno por sucursal — en el global el feriado departamental se resuelve por
 * persona, con {@code trs_Participante.codSucursal}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GenerarRolDto implements Serializable {

    /** Obligatorio. */
    private int  anio;

    // Alcance. 0 o null = sin filtrar (rol global).
    private Long codSucursal;
    private Long codEmpresa;

    // Metas. Null = el SP las calcula solo a partir de la cantidad de sábados.
    private Integer turnosObjetivoA;
    private Integer turnosObjetivoB;
    private Integer coberturaObjetivo;

    /**
     * Null = no tocar. Al CREAR asume 1; al REGENERAR conserva lo que ya tenga el rol.
     * Por eso es wrapper y no un {@code int} con default.
     */
    private Integer aplicaAsuetoCumple;

    private Long audUsuario;

    /** CREAR | REGENERAR. Si no viene, el SP asume CREAR. */
    private String modo;

    /** 1 = marcar jornada especial el primer sábado de cada trimestre. */
    private int todosPrimerSabadoTrimestre;
}
