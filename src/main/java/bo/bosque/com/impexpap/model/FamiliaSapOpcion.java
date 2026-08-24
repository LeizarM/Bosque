package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

/**
 * Una familia de articulos de SAP, vista desde comisiones: lo que se ofrece al
 * elegir a que familia ponerle un descuento.
 * <p>
 * <b>No se reusa {@link GrupoFamiliaSap}</b>, que representa la misma tabla pero
 * es del modulo de precios: aquel declara los codigos como int y solo trae dos
 * de las tres empresas. Los codigos de tpr_grupoFamiliaSap son varchar y hay uno
 * por empresa, incluida PRODUCTIVA PAPEL, asi que forzarlos a aquel modelo
 * perderia datos o reventaria la conversion.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FamiliaSapOpcion implements Serializable {

    private long   idGrpFamiliaSap;
    private String grpFam;
    private String alias;

    private String codGrpFamSap;         // IMPEXPAP
    private String codGrpFamSapEpp;      // ESPPAPEL
    private String codGrpFamSapProdPap;  // PRODUCTIVA PAPEL

    /** 1 si ya tiene una politica activa. Solo lectura. */
    private int    tienePolitica;
}
