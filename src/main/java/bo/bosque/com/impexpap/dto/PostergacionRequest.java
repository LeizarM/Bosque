package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Cuerpo de {@code /cheque/postergacion/registrar} ({@code codCheque}, {@code fecha}, {@code observacion}) y de
 * {@code /cheque/postergacion/eliminar} ({@code codCheque} y {@code codPostergacion}: la postergacion tiene que ser de ese
 * cheque). El usuario de auditoria sale del token, nunca del cuerpo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PostergacionRequest implements Serializable {

    private Integer codCheque;
    private Integer codPostergacion;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fecha;

    private String observacion;
}
