package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Filtros de la grilla de cheques: lo que manda el panel de busqueda de {@code cheque.xhtml}.
 *
 * <p>Equivale a los parametros de {@code ChequesDao.chequeList}. Vacio o 0 = "sin filtro" (el DAO
 * convierte a {@code null}, porque {@code p_list_Cheque} usa {@code @x IS NULL} como "sin filtro").
 * <ul>
 *   <li>{@code nroCheque} y {@code cliente}: coincidencia parcial ({@code LIKE '%x%'}).</li>
 *   <li>{@code tipo}, {@code estado}, {@code codBanco}, {@code fechaCobro}: igualdad.</li>
 *   <li>{@code fechaRecepcion}: el <i>dia</i> de la accion REC.</li>
 *   <li>{@code orden}: {@code RECEPCION} (rama A, mas reciente primero, es el defecto) o
 *       {@code COBRO} (rama B, por fecha de cobro ascendente).</li>
 * </ul>
 * La sucursal es obligatoria. Filtra por sucursal y <b>no por empresa</b>, como el legacy.
 *
 * <p>Paginacion en el servidor ({@code pagina} desde 1; {@code tamanio} por defecto 20, que es lo
 * que mostraba el legacy). El SP devuelve todas las filas: el servidor corta la pagina y devuelve
 * el total, en vez de entregar miles de filas al cliente.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ChequeFiltroDto implements Serializable {

    private Long codSucursal;
    private String nroCheque;
    private String cliente;
    private String tipo;
    private String estado;
    private Integer codBanco;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaCobro;

    /** Un solo dia de recepcion (la rama 'A' del SP lo filtra por igualdad). El rango de abajo lo reemplaza en la pantalla. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaRecepcion;

    /**
     * Rango de la fecha de recepcion (la de la accion {@code REC}, la columna "Recepcion" de la grilla), ambos
     * extremos <b>incluidos</b> y opcionales. El SP no filtra por rango, asi que lo aplica el servicio sobre
     * lo que devuelve (igual que la paginacion); {@code total} y {@code pagina} ya corresponden al rango.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaRecepcionDesde;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaRecepcionHasta;

    private String orden;
    private Integer pagina;
    private Integer tamanio;
}
