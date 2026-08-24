package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Una fila por periodo congelado ({@code tcom_pagadoItemCorte}).
 * <p>
 * Existe para que un periodo con CERO items se pueda leer. Sin el corte, cero
 * items no distingue "el congelado corrio y no habia nada que congelar" de "el
 * congelado no corrio", y las dos cosas se ven igual: una tabla vacia. Con la
 * politica arrancando el 21/08/2026 y 95 periodos ya pagados antes, el cero
 * legitimo va a ser lo normal durante meses.
 * <p>
 * {@code notasSinItems} es el numero a vigilar: notas que se pagaron y de las
 * que no quedo detalle.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PagadoItemCorte implements Serializable {

    private Long    idCorte;
    private Integer mesPago;
    private Integer anioPago;
    private Integer esInterno;

    private Integer items;
    private Integer itemsExcluidos;

    private Integer notasPagadas;
    private Integer notasConItems;
    private Integer notasSinItems;

    /** La ventana de politica al momento del corte, copiada: la politica se
     *  edita y el cero de hoy dejaria de tener explicacion. */
    private Date    politicaDesde;
    private Integer politicasActivas;

    private Date    audFecha;

    /** Como leer el cero, dicho por el propio SP para que no lo tenga que
     *  deducir cada pantalla. */
    private String  lectura;
}
