package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Mapea 1:1 la tabla <b>tch_banco</b>.
 *
 * <pre>
 *   codBanco   int          NOT NULL IDENTITY PK
 *   nombre     varchar(50)  NULL
 *   audUsuario int          NULL
 *   audFecha   datetime     NULL
 * </pre>
 *
 * <p>Es la tabla mas compartida del modulo: la referencian por FK {@code tdep_BancoXCuenta}
 * (Depositos) y cinco tablas {@code tpex_*} (Pagos al Exterior). Borrar un banco que tiene filas
 * ahi lo rechaza {@code p_abm_Banco} 'D' con el error 547.
 *
 * <p>La respuesta de {@code /banco/bancosX} y {@code /banco/bancosPlanilla} no es esta clase sino
 * {@link bo.bosque.com.impexpap.dto.BancoDto}: lleva {@code fila}, un campo de pantalla, y el
 * frontend (registro de empleados) lo lee. Los datos de pantalla no van en el modelo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChBanco implements Serializable {

    /** int IDENTITY. 0 o null = alta. */
    private Integer codBanco;

    /** varchar(50). */
    private String nombre;

    /** int. Sale del token, nunca del cuerpo. */
    private Integer audUsuario;

    /** datetime. La sella el procedimiento con GETDATE(). */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date audFecha;
}
