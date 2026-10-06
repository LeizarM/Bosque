package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de {@code POST /cheque/verificacion/preparar} ("Seleccionar" un cheque pendiente): el cheque, con los datos
 * que muestra el formulario, y la fecha de verificacion que se propone (hoy; el legacy tomaba {@code GETDATE()}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepararVerificacionDto implements Serializable {

    private ChequePendienteFilaDto cheque;

    @FechaDiaJson
    private LocalDate fechaBanco;
}
