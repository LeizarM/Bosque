package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma de la rama <b>P</b> de {@code p_list_producto}: exportacion de las familias
 * activas con todas sus descripciones resueltas.
 *
 * <p>Ojo con los alias: en esta rama el proveedor se llama {@code proveedor}, el grupo
 * de familia {@code familia} y el rango {@code rangoGram} (en la rama L son
 * {@code proveedorSap}, {@code grpFamilia} y {@code rangoGramaje}). Por eso no se
 * puede reutilizar {@code ProductoDto} aqui.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductoExportDto {

    private Integer codigoFamilia;

    /** float(53) en la base; BigDecimal para no perder centavos. */
    private BigDecimal costoTM;

    /** ISNULL sobre po.proveedorExtSap: si no hay proveedor llega el texto por defecto. */
    private String proveedor;

    /** ISNULL sobre g.grpFam: es el grupo de familia SAP, no la familia. */
    private String familia;

    private String formato;

    private String gramaje;

    private String color;

    private String presentacion;

    private String tipo;

    /** Texto armado en el SP con el formato "[min - max]". */
    private String rangoGram;

    /**
     * Duplicado de {@code costoTM} que el SELECT legacy repite al final de la fila.
     * Trae siempre el mismo valor. Se conserva porque el export del JSF lee por
     * posicion; el ALTER solo le puso el alias {@code costoTM2} para que las dos
     * columnas puedan mapearse por separado en vez de pisarse.
     */
    private BigDecimal costoTM2;
}
