package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Filtros del listado de talonarios. Todos opcionales.
 *
 * Van como Long / Integer y no como primitivos a proposito: null significa
 * "sin filtro" y el DAO omite el parametro para que el SP use su DEFAULT NULL.
 * Con primitivos, el 0 por defecto filtraria por id = 0 y no devolveria nada.
 *
 * codGrupo no es columna de tmto_talonario (se resuelve via
 * tmto_talonarioPorGrupo), por eso el filtro va en un DTO y no en el modelo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TalonarioFiltroDto implements Serializable {

    private Long codTipoRecibo;
    private Long codEmpresa;
    private Long codGrupo;

    /** 1 Adquirido, 2 Entregado, 3 Devuelto, 4 Cerrado. */
    private Integer codEstadoActual;

    /**
     * Rango de alta (tmto_talonario.audFecha). Los dos sueltos y opcionales.
     *
     * El filtro entra ANTES de agrupar el log de eventos, que es donde esta el
     * trabajo. Ojo con el default: los datos son historicos —nada entre 2024 y
     * 2025— asi que un "ultimos 12 meses" deja la pantalla casi vacia.
     */
    private Date fechaDesde;
    private Date fechaHasta;

    /**
     * Con FALSE saca los cerrados, que son estado terminal y hoy el 54% de las
     * filas: 1045 -> 480, y el payload de 334 KB a 153 KB.
     *
     * Si se pide un estado explicito, ese manda: pedir "Cerrado" los devuelve
     * aunque este interruptor los excluya.
     */
    private Boolean incluirCerrados;

}
