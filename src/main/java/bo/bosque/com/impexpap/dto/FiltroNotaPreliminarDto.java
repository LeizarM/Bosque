package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Filtro del desglose de notas de una fila del preliminar.
 * <p>
 * Los cuatro primeros campos son los que identifican la fila que el usuario
 * toco: vendedor, periodo y tasa. La tasa entra como numero y sale hacia el SP
 * como cadena, ver {@code comisionCad()}.
 * <p>
 * {@code modalidad} no viaja al SP: sirve para saber con que boton del ACL se
 * autoriza el pedido, porque en Comisiones.xhtml el boton vivia DENTRO de cada
 * pestana de preliminar y cada una tenia su permiso.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FiltroNotaPreliminarDto implements Serializable {

    private int idVendedor;
    private int mes;
    private int anio;

    /** Factor de comision, base 1: 0.005 es 0,5%. */
    private BigDecimal comision;

    /** interno | externo | dinamicaAnterior | dinamicaVigente */
    private String modalidad;

    /**
     * La comision como la espera el SP.
     * <p>
     * {@code @comisionCad} esta declarado {@code VARCHAR(6)} y se compara contra
     * una expresion float, asi que SQL Server convierte la cadena. Se mandan los
     * ceros de mas recortados —0.005 y no 0.00500— porque con 7 caracteres el
     * motor trunca a 6 y la comparacion podria cambiar de valor en silencio.
     */
    public String comisionCad() {
        if (comision == null) return null;
        return comision.stripTrailingZeros().toPlainString();
    }
}
