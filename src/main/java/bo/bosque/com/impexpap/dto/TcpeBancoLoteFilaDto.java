package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_BancoLote 'V'}: una por pregunta, con el estado que decide el SP. Si el envío entero no
 * sirve, llega una sola fila 0 con estado {@code ERROR} y el motivo en {@code mensaje}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoLoteFilaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer fila;

    private String pregunta;

    private Boolean esCerrada;

    private Integer cantOpciones;

    private Integer cantValidas;

    /** {@code OK}, {@code DUPLICADA_EN_BANCO}, {@code DUPLICADA_EN_ARCHIVO} o {@code ERROR}. */
    private String estado;

    private String mensaje;

    /** Con {@code DUPLICADA_EN_BANCO}: la pregunta que ya existe en la categoría. */
    private Long codPreguntaExistente;
}
