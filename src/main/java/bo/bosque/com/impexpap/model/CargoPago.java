package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CargoPago implements Serializable {

    private long   idCargo;        // PK NOT NULL
    // Un cargo pertenece a UNA cotización O a UNA transacción; el otro queda NULL
    // en BD → deben ser wrapper (Long) o BeanPropertyRowMapper revienta al leer.
    private Long   idCotizacion;   // bigint NULL
    private Long   idTransaccion;  // bigint NULL
    private long   idTipoCargo;    // NOT NULL
    private double baseCalculo;    // decimal(18,2) NOT NULL
    private String origenBase;     // varchar NULL
    private Double porcentaje;     // decimal(10,6) NULL → wrapper (cargos de monto fijo no lo traen)
    private Double valorFijo;      // decimal(18,2) NULL → wrapper (cargos porcentuales no lo traen)
    private double montoCargo;     // decimal(18,2) NOT NULL
    private long   idMoneda;       // bigint NOT NULL
    private int    orden;          // NOT NULL
    private String descripcion;    // varchar NULL
    private Long   audUsuario;     // bigint NULL → wrapper

}
