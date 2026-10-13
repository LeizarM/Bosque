package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/** Tipo de documento del archivo (tabla {@code tarc_tipoDocumento}): Facturas, Notas de entrega... */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TipoDocumentoArchivo implements Serializable {

    private long codTipoDocumento;
    private String nombre;
    private Integer estado;
    private long audUsuario;
    private Date audFecha;

    // Solo lectura (p_list_tarc_TipoDocumento 'L'): cuantas lineas de contenido lo usan.
    private Integer usos;
}
