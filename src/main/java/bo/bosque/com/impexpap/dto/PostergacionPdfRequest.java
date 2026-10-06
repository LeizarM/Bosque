package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * La postergacion cuyo documento PDF se consulta o se descarga ({@code POST /cheque/postergacion/pdf/estado} y
 * {@code /pdf/descargar}). {@code codPostergacion} es {@code tch_chPostergacion.codPostergacion}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PostergacionPdfRequest implements Serializable {

    private Integer codPostergacion;
}
