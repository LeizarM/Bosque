// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\ChequeCierreDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Un cheque del panel "Cheques" de la revisión de Cierre de Operaciones
 * ({@code p_SAP_Rpt_ImpChequesPR @ACCION='A'}, el mismo que usaba
 * {@code ValeDao.lstChequeCO} en el sistema anterior).
 *
 * <p>Los nombres son los alias del SELECT del SP; {@code BeanPropertyRowMapper}
 * no distingue mayúsculas, así que {@code nrocheque} llena {@link #nroCheque} y
 * {@code CardName} llena {@link #cardName}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChequeCierreDto {

    private String nroCheque;
    private String codCliente;
    private String cardName;
    private Double monto;

    /** Como la guarda Bosque: 'BS' o 'USD'. "--" si el cheque no está en Bosque. */
    private String moneda;

    /** El nombre corto de la empresa. */
    private String nombre;

    /**
     * Lo que encontró el cruce contra SAP: COBRADO, COBRADO, CON OTRA FECHA,
     * SIN COBRAR, COBRADO - SALIDA CON FECHA ANTERIOR o SIN REGISTRO BOSQUE.
     * En el sistema anterior se mostraba en la columna "Observacion".
     */
    private String resultado;

    private Integer codEmpresa;
}
