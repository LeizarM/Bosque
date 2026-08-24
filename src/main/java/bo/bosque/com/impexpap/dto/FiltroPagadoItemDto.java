package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Filtro del detalle por item de lo pagado.
 * <p>
 * mes y anio son obligatorios: la tabla guarda todos los periodos y sin
 * periodo la consulta traeria el historico entero.
 * <p>
 * <b>esInterno NO alcanza para separar empresas.</b> IMPEXPAP / PAPIRUS /
 * PRODUCTIVA PAPEL y ESPPAPEL se congelan las dos con {@code esInterno = 1};
 * lo unico que las distingue es {@link #origen}. Sin el, la consulta devuelve
 * las dos mezcladas y el resumen suma las dos, que era exactamente el numero
 * que no cuadraba con ninguno de los dos PDF.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FiltroPagadoItemDto implements Serializable {

    private int     mes;
    private int     anio;

    /** Null vale 1, que es como se congelan internas y Esppapel. */
    private Integer esInterno;

    /** Una nota puntual; null trae el periodo entero. */
    private Long    idPagado;

    /**
     * Una nota puntual por numero de documento.
     * <p>
     * Ojo: {@code docNum} NO es unico entre empresas -198 casos medidos en un
     * mismo periodo-, asi que sin {@link #origen} puede traer la nota de las
     * dos. Va junto con el, no en lugar de el.
     */
    private Long    docNum;

    /**
     * La empresa: {@code ESPPAPEL}, {@code IMPEXPAP}, {@code PAPIRUS} o
     * {@code PRODUCTIVA PAPEL}. Null trae las cuatro juntas.
     * <p>
     * El SP compara por igualdad, asi que es un origen por consulta: no existe
     * "todas menos ESPPAPEL" en un solo llamado.
     */
    private String  origen;

    /** Solo las lineas que no descontaron. */
    private boolean soloExcluidos;
}
