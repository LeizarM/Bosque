package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/** Filtro de escalas de comision dinamica. esInterno null trae internas y externas. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FiltroComisionDinamicaDto implements Serializable {

    private Integer esInterno;
    private Date    fecha;
}
