package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Una fila de {@code tbio_bioBitacora} con {@code audUsuario} resuelto a nombre
 * — el modelo crudo sólo trae el id (ver {@link bo.bosque.com.impexpap.model.BioBitacora}),
 * la resolución la hace {@code BiometricoController} contra {@code ILoginDao.getAllUsers()}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraDto {

    private long idBitacora;
    private String tabla;
    private String idRegistro;
    private String accion;
    private String motivo;
    private long audUsuario;
    private String nombreUsuario;
    private Date audFecha;
}
