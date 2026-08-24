package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Una linea de una nota ya pagada, tal como quedo congelada al ejecutar el
 * periodo ({@code tcom_pagadoItem}).
 * <p>
 * Es historico: no se recalcula. Lo que la hace util no es lo que descuenta
 * sino lo que NO descuenta, que es la mayoria -15 de cada 19 items medidos- y
 * hasta ahora no quedaba registrado en ningun lado: se perdia al recapturar la
 * nota o al editar la politica.
 * <p>
 * A diferencia del resto de los modelos del modulo, este NO lleva
 * {@code @JsonInclude(NON_NULL)}: {@code motivoExclusion} en null significa
 * "esta linea si descontó", y omitir el campo del JSON obligaria a la app a
 * deducir esa lectura de la ausencia.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PagadoItem implements Serializable {

    private Long       idPagadoItem;
    private Long       idPagado;

    // ---------- el periodo ----------
    private Integer    mesPago;
    private Integer    anioPago;
    private Integer    esInterno;

    // ---------- la nota ----------
    private Long       docNum;
    private String     origen;
    private Integer    bd;
    private Date       fechaDoc;
    private Long       idVendedor;

    // ---------- el item ----------
    /** itemName y grpFam vienen copiados de la nota, no del maestro: el maestro
     *  se edita y el nombre de hoy puede no existir dentro de un anio. */
    private String     itemCode;
    private String     itemName;
    private Integer    itmsGrpCod;
    private Long       idGrpFamiliaSap;
    private String     grpFam;
    private Double     cantidad;
    private Double     montoLineaBS;

    // ---------- que paso con la linea ----------
    /** 1 descontó, 0 quedo excluida. */
    private Integer    aplicaDescuento;
    /** El porcentaje que regia el dia de la nota. Null si no aplico ninguno. */
    private BigDecimal porcentajePago;
    private Double     descuentoBS;
    /** VENDEDOR_EXENTO, SIN_FAMILIA, FAMILIA_SIN_POLITICA, FUERA_DE_VIGENCIA.
     *  Null es el quinto caso: descontó. */
    private String     motivoExclusion;

    private Date       audFecha;
}
