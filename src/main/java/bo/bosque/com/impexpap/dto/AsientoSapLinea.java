package bo.bosque.com.impexpap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/** Una línea del asiento de planilla, tal como va a {@code JournalEntryLines} de SAP. */
@Getter
@AllArgsConstructor
public class AsientoSapLinea {

    /** {@code CodigoSN} del SP: una cuenta de mayor o un socio de negocio. */
    private final String codigo;
    /** {@code LineMemo}, recortado a 50 caracteres. */
    private final String nombre;
    /** {@code AccountCode}: la cuenta de mayor, o la cuenta asociada del socio (vacía si el SP no la trae). */
    private final String cuenta;
    /** {@code ShortName}: el socio de negocio; {@code null} en las líneas de cuenta de mayor. */
    private final String socio;
    private final BigDecimal debito;
    private final BigDecimal credito;
}
