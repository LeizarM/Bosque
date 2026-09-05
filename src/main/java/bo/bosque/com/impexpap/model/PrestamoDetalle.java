package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@NoArgsConstructor
@ToString
public class PrestamoDetalle {
    private Long codPrestDetalle;
    private Long codPrestamo;
    private String tipoPago;
    private String datoPago;
    private String detalle;
    private String observacion;
    private String postergado;
    private String estadoCuota;
    private Integer numeroCuota;
    private float montoPago;
    private float debe;
    private float haber;
    private Double haberAnterior;
    private float saldo;
    private Date fechaPago;

    private Long audUsuario;
    private Date audFecha;
    private Integer mostrarAnulados;
    private Long transIdSAP_pago;
    private String xmlPagos;
}
