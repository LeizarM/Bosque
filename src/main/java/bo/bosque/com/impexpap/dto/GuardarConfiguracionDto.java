package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nuevo valor de un parámetro de trec_configuracion. */
@Getter
@Setter
@NoArgsConstructor
public class GuardarConfiguracionDto {
    private String clave;
    private String valor;
}
