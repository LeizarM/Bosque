package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/** Una caja fisica (tabla {@code tarc_caja}). El numero solo es unico dentro de su archivo. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CajaArchivo implements Serializable {

    private long codCaja;
    private Long codArchivo;
    private Integer numero;
    private String estante;
    private String fila;
    private String observacion;
    private long audUsuario;
    private Date audFecha;

    // Solo lectura (p_list_tarc_Caja 'L'): nombre del archivo, lineas, fotos, rango de anios del contenido
    // y la portada (la foto de menor orden, nunca un PDF), para la miniatura del listado.
    private String nombreArchivo;
    private Integer lineas;
    private Integer fotos;
    private Integer anioDesde;
    private Integer anioHasta;
    private Long codFotoPortada;
    private String rutaPortada;

    // Solo lectura: el prestamo abierto de la caja completa, su destruccion y cuantas lineas estan afuera o destruidas.
    // Las fechas llegan como texto "dd/mm/aaaa hh:mm".
    private Long codPrestamoCaja;
    private String prestadaA;
    private String fechaPrestamo;
    private String fechaDestruccion;
    private Integer prestados;
    private Integer destruidos;
}
