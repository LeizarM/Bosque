package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Cuerpo de las dos operaciones del armado de una propuesta por familia:
 * {@code /price/armado/calcularFamilia} (no escribe) y
 * {@code /price/armado/guardarFamilia} (escribe todo en una transaccion).
 *
 * <p>Reemplaza al trio {@code dlgNuevo} + {@code dlgProd} + {@code dlgVista} de
 * Autorizacion.xhtml. Alli la cabecera, los fletes y los precios se guardaban en
 * pasos sueltos, y la propuesta nacia recien cuando se guardaba la primera
 * familia. Aca se conserva ese momento de nacimiento -no quedan propuestas
 * vacias- pero todo viaja junto y se graba de una sola vez.
 *
 * <p>No lleva {@code audUsuario} ni {@code codEmpresa}: los dos salen del token.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArmadoFamiliaDto implements Serializable {

    /**
     * Propuesta destino. Nulo o 0 = la propuesta todavia no existe y se crea con
     * esta familia; en ese caso son obligatorios {@link #titulo}, {@link #obs} y
     * {@link #fletes}.
     */
    private Long idPropuesta;

    /** Solo en el alta. Obligatorio, hasta 200 caracteres (tpr_propuesta.titulo). */
    private String titulo;

    /** Solo en el alta. Obligatorio, hasta 250 caracteres (tpr_propuesta.obs). */
    private String obs;

    /**
     * Solo en el alta: el costo de flete por sucursal, en USD por tonelada. Se usa
     * {@link CostoIncreSucursalDto} porque es la misma forma que devuelve la
     * lectura de fletes; aca solo importan {@code codSucursal} y {@code valor}.
     */
    private List<CostoIncreSucursalDto> fletes;

    /** Familia a repreciar. Obligatorio. */
    private Integer codigoFamilia;

    /**
     * Costo propuesto en USD por tonelada. En {@code calcularFamilia} puede ir
     * nulo: se usa el costo ya guardado en la propuesta, y si tampoco hay, la
     * grilla vuelve sin precio calculado. En {@code guardarFamilia} es obligatorio.
     */
    private BigDecimal costo;

    /**
     * Porcentajes de utilidad cambiados en el editor, por lista. Opcional: las listas
     * que no vienen usan el de tpr_porcentaje. Al guardar, los que cambian se registran
     * en tpr_porcentaje. Ver {@link PorcentajeArmadoDto}.
     */
    private List<PorcentajeArmadoDto> porcentajes;
}
