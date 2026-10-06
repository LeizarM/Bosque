package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Un banco tal como lo devuelven {@code POST /banco/bancosX} y {@code /banco/bancosPlanilla}.
 *
 * <p>Es el JSON que esos endpoints ya devolvian cuando el modelo era {@code ChBanco} (con
 * {@code fila}, un campo de pantalla): el frontend lo lee en el registro de empleados
 * ({@code BancoModel.fromJson}, con {@code ?? 0} por si falta). Por eso la forma NO cambia; solo se
 * movio a un DTO para que {@code ChBanco} pueda ser el espejo de {@code tch_banco}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BancoDto implements Serializable {

    private int codBanco;
    private String nombre;
    private int audUsuario;
    /** Numero de fila, 1..n, para la pantalla. */
    private int fila;
}
