package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.*;

/**
 * Entrega masiva de talonarios. Cabecera del formulario + los tildados.
 *
 * Reemplaza completarInformacionTalDet() / guardarLoteTalDet() del
 * WizardTalonario. Aquel aplicaba la cabecera a cada fila tildada y las
 * guardaba en un bucle sin transaccion, ademas de reportar exito aunque
 * fallaran todas. Aca va todo o nada.
 *
 * El destinatario es excluyente: sucursal O empleado, nunca los dos.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntregaLoteDto implements Serializable {

    /** Los talonarios tildados en la grilla. */
    private List<Long> codTalonarios;

    /** Uno de los dos en cero: el otro es el destinatario. */
    private long codSucursal;
    private long codEmpleado;

    /** Cuando ocurrio la entrega. Obligatoria. */
    private Date fechaEvento;

    private String observacion;
    private long audUsuario;

}
