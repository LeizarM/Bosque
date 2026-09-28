package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * Cuerpo de la carga en lote del asistente: {@code /price/armado/calcularFamilias} (no
 * escribe) y {@code /price/armado/guardarFamilias} (escribe todo en una transaccion).
 *
 * <p>Lo pidio el usuario para el trabajo operativo: con un costo nuevo de importacion hay
 * que repreciar de a decenas de familias, y abrir el editor de cada una para escribir un
 * numero era lo que mas tiempo llevaba. Cada familia se calcula igual que en
 * {@code calcularFamilia}: con sus listas de precio activas, el porcentaje de cada lista,
 * IVA, IT y el flete de cada sucursal.
 *
 * <p>Los campos del alta (titulo, obs, fletes) valen lo mismo que en
 * {@link ArmadoFamiliaDto}: solo viajan cuando la propuesta todavia no existe, y en ese caso
 * la propuesta nace con el primer lote que se guarda.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArmadoLoteDto implements Serializable {

    /** Propuesta destino. Nulo o 0 = se crea con este lote. */
    private Long idPropuesta;

    /** Solo en el alta. */
    private String titulo;

    /** Solo en el alta. */
    private String obs;

    /** Solo en el alta: el flete por sucursal, en USD por tonelada. */
    private List<CostoIncreSucursalDto> fletes;

    /** Las familias con su costo. Obligatorio, sin repetidas. */
    private List<FamiliaLoteDto> familias;
}
