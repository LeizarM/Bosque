package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/** Una linea de contenido de una caja (tabla {@code tarc_contenido}): una fila del Excel original. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ContenidoCaja implements Serializable {

    private long codContenido;
    private Long codCaja;
    private Long codTipoDocumento;
    private Long codEmpresa;
    private String detalle;
    private String gestion;
    // Columnas que el SP deriva de gestion: el cliente no las manda, SimpleJdbcCall las ignora.
    private Integer anioDesde;
    private Integer anioHasta;
    private String observacion;
    private long audUsuario;
    private Date audFecha;

    // Solo lectura (p_list_tarc_Contenido 'L').
    private String nombreTipoDocumento;
    private String nombreEmpresa;
    /** C en la caja, P prestada (sola o con su caja), X destruida (sola o con su caja). */
    private String situacion;
    /** El prestamo abierto que la tiene afuera (el de la linea o el de la caja completa). */
    private Long codPrestamo;
    private String prestadoA;
    private String fechaPrestamo;
    private String fechaDestruccion;
}
