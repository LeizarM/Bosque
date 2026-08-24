package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

/**
 * Estado de ejecucion de un periodo de comisiones.
 * <p>
 * Reemplaza a las ramas E y H de p_list_paraPagar, que devolvian la columna
 * llamada 'ejecutado' cuando valia 1 y 'noEjecutado' cuando valia 0, obligando
 * a leerla por posicion.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EstadoPeriodo implements Serializable {

    private int        mes;
    private int        anio;
    private int        esInterno;
    private int        ejecutado;        // 1 = ya se pago
    private int        cantidadPagados;
    private Date       fechaEjecucion;
    private BigDecimal totalComision;
}
