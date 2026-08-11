package bo.bosque.com.impexpap.dto;

import lombok.*;
import java.io.Serializable;

/**
 * Proyección de {@code p_list_trs_Programador @ACCION='D'} — el organigrama expandido
 * de un jefe.
 *
 * <p>Sale de la función en línea {@code fn_trs_ProgramadorDependiente()}, que recorre
 * {@code trh_cargo.codCargoPadre} recursivamente. <b>No confundir con el modelo
 * {@code model.Dependiente}</b>, que es de RR.HH. (familiares a cargo).
 *
 * <p>Es una FUNCIÓN y no un procedimiento porque {@code trs_sp_programar} y
 * {@code trs_sp_convocar} la usan ADENTRO, con JOIN y EXISTS, para validar que un
 * dependiente cuelgue del jefe — y un procedimiento no se puede joinear.
 *
 * <p>{@code profundidad} 1 = reportes directos. El {@code alcance} del programador
 * decide el corte: {@code DIRECTOS} filtra profundidad 1, {@code SUBARBOL} trae todo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProgramadorDependienteDto implements Serializable {

    private long   idProgramador;
    private long   codProgramador;
    private int    profundidad;
    private long   codDependiente;
    private String nombreDependiente;
    /** Wrapper: viene de {@code tb_cargo_sucursal.codSucursal}, que admite NULL. */
    private Long   sucDependiente;

    // ── sólo los devuelve @ACCION='P' (previsualizar antes de dar de alta) ──
    /*
     * En 'D' estas dos columnas no vienen y quedan en su default ("" y 0). No es
     * un problema: 'D' se usa para el equipo YA guardado, donde la pantalla que
     * lo consume no las pide.
     */
    /** Nombre de la sucursal del dependiente, ya resuelto. */
    private String sucursal;
    /**
     * 1 si además es participante activo del rol.
     *
     * <p>Estar en el organigrama no alcanza para que el jefe pueda programarlo:
     * {@code trs_sp_programar} exige las dos cosas. Un dependiente con
     * {@code enElRol=0} le va a aparecer al jefe con el cartel "no está en el rol
     * de este año", así que conviene verlo ANTES de dar el permiso.
     */
    private int    enElRol;
}
