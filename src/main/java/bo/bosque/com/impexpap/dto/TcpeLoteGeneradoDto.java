package bo.bosque.com.impexpap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Respuesta de {@code /examenes/lotes/generar} y {@code /agregar}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TcpeLoteGeneradoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codRolExamen;

    private List<TcpeCredencialDto> credenciales;
}
