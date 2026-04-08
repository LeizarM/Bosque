package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SolicitudProveedorDto {
    private Long idSolicitudProveedor;
    private Long idSolicitud;
    private String cardCode;
    private String cardName;
    private BigDecimal totalFacturasUsd;
    private BigDecimal totalAmortizadoUsd;
    private BigDecimal totalAPagarUsd;
    private String obs;
    private Integer audUsuario;
    private Integer codEmpresa;

    // CORRECCIÓN: Usar la lista del DTO, no del Modelo.
    // Inicializar la lista evita NullPointerExceptions al hacer .add() más adelante.
    private List<DetalleSolicitudDto> detalles = new ArrayList<>();
}