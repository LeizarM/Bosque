package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

/**
 * Área emisora del documento ({@code tcr_Area}), por empresa.
 *
 * <p>Lo que se graba en {@code tcr_documento.area} y sale impreso en el CITE
 * es la <b>sigla</b> ("G.A.", "R.RH."), no el id: por eso acá no hay idArea.
 * {@code tcr_Area} tiene siglas repetidas dentro de una misma empresa
 * (ESPPAPEL tiene dos filas "G.A."), y el SP las agrupa para que el combo no
 * muestre la misma opción dos veces.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CiteArea implements Serializable {

    private String siglas;
    private String descripcion;
}
