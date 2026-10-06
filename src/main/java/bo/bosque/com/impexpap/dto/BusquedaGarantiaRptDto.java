package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.*;

/**
 * Fila del reporte <b>RptCbrGarantia</b> (listado filtrado). Se arma en el controlador a
 * partir de {@link GarantiaDto} (ACCION 'G'), no de la rama 'E' del legacy, que repetia
 * cada garantia una vez por empresa SAP y dejaba {@code datoGarantia} en blanco.
 *
 * <p>Tipos iguales a los {@code <field>} del .jrxml ({@code fila} es BigDecimal y los montos
 * Double porque asi los declara el reporte original).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaGarantiaRptDto implements Serializable {

    private BigDecimal fila;
    private String cliente;
    private Double monto;
    private Double lineaCredito;
    private String tiempoDePago;
    private java.sql.Date fechaInicio;
    private java.sql.Date fechaExpiracion;
    private String datoGarantia;
    private String realizoEmp;
    private java.sql.Date fechaRecepcion;
    private String descripcion;
    private String datoEstado;
}
