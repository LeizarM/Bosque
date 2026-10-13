package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/** Una foto de caja (tabla {@code tarc_cajaFoto}): hasta 3 por caja, en las posiciones 1 a 3. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FotoCaja implements Serializable {

    private long codFoto;
    private Long codCaja;
    private Integer orden;
    // Relativa a uploads.dir; la arma el servidor, nunca el cliente.
    private String ruta;
    private long audUsuario;
    private Date audFecha;
}
