package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Un registro de la bitacora de destruccion (tabla {@code tarc_destruccion}): documentos que cumplieron su plazo
 * de guarda y se trituraron. Una linea de la caja, o la caja completa si {@code codContenido} es null.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DestruccionDocumento implements Serializable {

    private long codDestruccion;
    private Long codCaja;
    private Long codContenido;
    /** La pone el SP y llega como texto "dd/mm/aaaa hh:mm" (la JVM de produccion corre en UTC). */
    private String fechaDestruccion;
    private String observacion;
    private long audUsuario;
    private Date audFecha;

    // Solo lectura (p_list_tarc_Destruccion 'L').
    private Integer numeroCaja;
    private Long codArchivo;
    private String nombreArchivo;
    private String nombreTipoDocumento;
    private String detalle;
    private String gestion;
    private String nombreUsuario;
}
