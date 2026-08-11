package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Quiénes pueden corregir CUALQUIER celda del rol de sábados — tabla {@code trs_Rrhh}.
 *
 * <p>SPs: {@code p_abm_trs_Rrhh} (I/U/D) · {@code p_list_trs_Rrhh} (L).
 *
 * <p><b>Por qué existe esta tabla y no alcanza el rol de usuario.</b> Escribir una celda
 * a mano —el editor que se abre al tocar la grilla— no tenía dueño: bastaba con estar
 * logueado. Cualquiera de los usuarios {@code ROLE_LIM} podía cambiarle el sábado a
 * cualquier persona, con cualquier letra, y firmarlo con el {@code audUsuario} que
 * quisiera. Era la puerta de atrás del control de jefes.
 *
 * <p>El rol de Spring no servía para taparlo: {@code ROLE_ADM} son los de Sistemas y
 * {@code ROLE_LIM} son todos los demás. La gente que de verdad carga vacaciones y bajas
 * no se distingue por su rol de usuario, así que hay que nombrarla.
 *
 * <p><b>No confundir con {@code Programador}</b>, que es la otra mitad del control: un
 * programador tiene un árbol y una sucursal y sólo alcanza a su propia gente; quien está
 * acá no tiene límite de árbol ni de sucursal.
 *
 * <p>La baja es LÓGICA ({@code estado='I'}): quién podía corregir celdas cuando se
 * corrigieron es justo lo que se va a querer mirar el día que alguien pregunte quién le
 * cambió el sábado.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Rrhh implements Serializable {

    private long   idRrhh;
    private long   codEmpleado;
    /** char(1): A = puede corregir · I = ya no. */
    private String estado;
    private String observacion;   // varchar(200)
    private Long   audUsuario;
    private Date   audFecha;

    // ── sólo lectura, ACCION 'L': joins ───────────────────────────────────
    /** Apellido y nombres, ya armados. */
    private String persona;
    /** Cargo y sucursal VIGENTES. Pueden llegar vacíos: alguien sin cargo cargado
     *  igual tiene que aparecer en el padrón, no desaparecer de la pantalla. */
    private String cargo;
    private String sucursal;
}
