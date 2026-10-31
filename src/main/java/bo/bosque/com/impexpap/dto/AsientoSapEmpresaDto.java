package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;

/** Una empresa de la configuración del asiento en SAP; {@code clave} solo al cargar la contraseña. */
@Getter
@Setter
public class AsientoSapEmpresaDto {

    private Integer codEmpresa;
    /** La contraseña del Service Layer, en claro solo en este request: el backend la cifra antes de guardarla. */
    private String clave;
}
