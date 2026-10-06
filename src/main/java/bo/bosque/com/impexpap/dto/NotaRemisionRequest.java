package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Cuerpo de {@code /cheque/nota-remision/registrar} (todos los campos) y de {@code /cheque/nota-remision/eliminar}
 * (solo {@code codCheque} y {@code notaRemision}). El usuario de auditoria sale del token, nunca del cuerpo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class NotaRemisionRequest implements Serializable {

    private Integer codCheque;
    private String notaRemision;
    private Integer nroFactura;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaFactura;
}
