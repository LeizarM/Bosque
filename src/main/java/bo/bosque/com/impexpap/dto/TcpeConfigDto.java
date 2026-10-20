package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Config 'L'} ({@code f_tcpe_config}): límites, valores por defecto y textos del módulo.
 * Se cambian en SQL, sin recompilar (contrato 7.1).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeConfigDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String clave;

    private String valor;

    private String descripcion;
}
