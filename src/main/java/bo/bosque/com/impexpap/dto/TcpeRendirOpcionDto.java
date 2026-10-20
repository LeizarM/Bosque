package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Rendir 'O'}. <b>Nunca</b> trae {@code esValido}: quien rinde no ve la correcta.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRendirOpcionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codRsptaOpH;

    private Long codPreguntaH;

    private String opcionRspta;

    private Boolean marcada;
}
