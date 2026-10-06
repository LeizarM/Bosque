package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Cuerpo de {@code /cheque/transaccion/registrar} (todos los campos) y de {@code /cheque/transaccion/eliminar} (solo
 * {@code codCheque} y {@code nroTransaccion}). El usuario de auditoria sale del token, nunca del cuerpo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TransaccionBancariaRequest implements Serializable {

    private Integer codCheque;
    private String nroTransaccion;
    private Integer codBanco;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaTransaccion;
}
