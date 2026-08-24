package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

/**
 * Una linea de lo que se descuenta por familia: de que item se trata, de que
 * nota, de que empresa, cuanto era y cuanto se resta.
 * <p>
 * Es la union de dos formas del SP y por eso casi todo es nullable:
 * <ul>
 *   <li><b>P</b> lee tcom_noPagado -lo que esta por pagarse- y trae el
 *       porcentaje resuelto contra la politica vigente el dia de la factura.</li>
 *   <li><b>H</b> lee tcom_pagado y trae lo que quedo congelado al pagar, que es
 *       lo unico que reconstruye el pasado si hoy los porcentajes son otros.</li>
 * </ul>
 * {@code esHistorico} dice cual de las dos vino.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DescuentoDetalle implements Serializable {

    private Long       idDetalle;

    // ---------- la nota ----------
    private Long       docNum;
    private String     empresa;
    private Date       fechaDoc;
    private Integer    mes;
    private Integer    anio;
    private Integer    mesPago;
    private Integer    anioPago;
    private Long       idVendedor;
    private String     nombreVen;
    private String     cardCode;

    // ---------- el item ----------
    private String     grupoFamilia;
    private Integer    codGrupoSap;
    private String     itemCode;
    private String     itemName;
    private BigDecimal cantidad;
    private BigDecimal montoItemBS;

    // ---------- la regla ----------
    private BigDecimal porcentajePago;
    private BigDecimal porcentajeDescuento;
    private BigDecimal descuentoBS;

    // ---------- la nota, en plata ----------
    private BigDecimal montoBaseNotaBS;
    private BigDecimal descuentoNotaBS;
    private BigDecimal montoNotaAjustadoBS;
    private BigDecimal comision;
    private BigDecimal comisionBS;
    private String     detalleBond;

    /** 1 si el vendedor esta exento: el descuento se calcula pero no se aplica. */
    private Integer    vendedorExento;

    /** 1 si viene de tcom_pagado. */
    private Integer    esHistorico;

    // ---------- solo en el resumen (ACCION R) ----------
    private Integer    notas;
    private Integer    items;
    private BigDecimal unidades;
    private BigDecimal montoItemsBS;
}
