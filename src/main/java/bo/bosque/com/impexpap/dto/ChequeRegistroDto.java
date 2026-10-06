package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.ChCheque;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Lo que manda el formulario de cheque para dar de alta o editar: las columnas de
 * {@link ChCheque} mas la observacion y el modo del formulario.
 *
 * <p><b>{@code modo}</b> es el formulario del legacy que el usuario uso, y de el depende que se
 * acepta y que ACL se exige (el JSF deshabilitaba campos; el servidor los ignora):
 * <ul>
 *   <li>{@code ESTANDAR} (defecto) &mdash; {@code chequeModal}. Alta: boton {@code btnNuevoCH}.
 *       Edicion: {@code btnEditar1CH} con el cheque NO cerrado. En edicion NO se aceptan cambios de
 *       entregado por, tipo, monto, moneda, a la orden ni fecha del cheque (el JSF los bloquea).</li>
 *   <li>{@code TALONARIO} &mdash; {@code chequeModalTal}, para cheques cerrados: {@code btnEditar1CH}
 *       con el cheque CERRADO. Solo cambian el talonario y el recibo manual.</li>
 *   <li>{@code ADMIN} &mdash; {@code chequeModalMad}. Alta: {@code btnNuevo2CH}. Edicion:
 *       {@code btnEditar3CH} con el cheque NO cerrado. Todo editable salvo empresa y sucursal.</li>
 * </ul>
 * El administrador ({@code ROLE_ADM}) pasa por cualquiera de los tres sin importar el estado, como
 * en {@code WizardCheque.esAutorizado}.
 *
 * <p>{@code codEmpleado} = 0 significa que lo entrego el cliente. El estado no se manda: el alta
 * siempre es {@code PEN} y la edicion conserva el que tiene.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class ChequeRegistroDto extends ChCheque {

    public static final String MODO_ESTANDAR = "ESTANDAR";
    public static final String MODO_TALONARIO = "TALONARIO";
    public static final String MODO_ADMIN = "ADMIN";

    /** {@code ESTANDAR}, {@code TALONARIO} o {@code ADMIN}. Vacio = {@code ESTANDAR}. */
    private String modo;

    /** Observacion de la accion REC (2 a 200 caracteres). */
    private String observacion;

    public boolean esAlta() {
        return getCodCheque() == null || getCodCheque() == 0;
    }

    /** El modo normalizado; cualquier valor desconocido cuenta como {@code ESTANDAR}. */
    public String modoEfectivo() {
        if (modo == null) return MODO_ESTANDAR;
        String m = modo.trim().toUpperCase();
        if (MODO_TALONARIO.equals(m) || MODO_ADMIN.equals(m)) return m;
        return MODO_ESTANDAR;
    }
}
